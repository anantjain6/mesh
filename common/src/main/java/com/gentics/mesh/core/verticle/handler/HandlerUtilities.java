package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ResultInfo;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.tx.TxAction;
import com.syncleus.ferma.tx.TxAction0;
import com.syncleus.ferma.tx.TxAction1;
import com.syncleus.ferma.tx.TxAction2;

import io.vertx.core.AsyncResult;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Common request handler methods which can be used for CRUD operations.
 */
@Singleton
public class HandlerUtilities {

	private static final Logger log = LoggerFactory.getLogger(HandlerUtilities.class);

	private Database database;

	@Inject
	public HandlerUtilities(Database database) {
		this.database = database;
	}

	/**
	 * Create an object using the given aggregation node and respond with a transformed object.
	 * 
	 * @param ac
	 * @param handler
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void createElement(InternalActionContext ac, TxAction1<RootVertex<T>> handler) {
		createOrUpdateElement(ac, null, handler);
	}

	/**
	 * Delete the specified element.
	 * 
	 * @param ac
	 * @param handler
	 *            Handler which provides the root vertex which will be used to load the element
	 * @param uuid
	 *            Uuid of the element which should be deleted
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void deleteElement(InternalActionContext ac, TxAction1<RootVertex<T>> handler,
		String uuid) {
		EventQueueBatch batch = database.tx(() -> {
			RootVertex<T> root = handler.handle();
			T element = root.loadObjectByUuid(ac, uuid, DELETE_PERM);

			// Load the name and uuid of the element. We need this info after deletion.
			String elementUuid = element.getUuid();
			EventQueueBatch b = database.tx(() -> {
				BulkActionContext bac = BulkActionContext.create();
				bac.setRootCause(element.getTypeInfo().getType(), elementUuid, "delete");
				element.delete(bac);
				return bac.batch();
			});
			log.info("Deleted element {" + elementUuid + "} for type {" + root.getClass().getSimpleName() + "}");
			return b;
		});

		batch.dispatch();
		ac.send(NO_CONTENT);
	}

	/**
	 * Locate and update or create the element using the action context data.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be updated
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 * 
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void updateElement(InternalActionContext ac, String uuid,
		TxAction1<RootVertex<T>> handler) {
		createOrUpdateElement(ac, uuid, handler);
	}

	/**
	 * Either create or update an element with the given uuid.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element to create or update. If null, an element will be created with random Uuid
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void createOrUpdateElement(InternalActionContext ac, String uuid,
		TxAction1<RootVertex<T>> handler) {
		AtomicBoolean created = new AtomicBoolean(false);
		asyncTx(ac, (tx) -> {
			RootVertex<T> root = handler.handle();

			// 1. Load the element from the root element using the given uuid (if not null)
			T element = null;
			if (uuid != null) {
				if (!UUIDUtil.isUUID(uuid)) {
					throw error(BAD_REQUEST, "error_illegal_uuid", uuid);
				}
				element = root.loadObjectByUuid(ac, uuid, UPDATE_PERM, false);
			}

			ResultInfo info = null;

			// Check whether we need to update a found element or whether we need to create a new one.
			if (element != null) {
				final T updateElement = element;
				Tuple<Boolean, EventQueueBatch> tuple = database.tx(() -> {
					EventQueueBatch batch = EventQueueBatch.create();
					boolean updated = updateElement.update(ac, batch);
					return Tuple.tuple(updated, batch);
				});

				EventQueueBatch b = tuple.v2();
				Boolean isUpdated = tuple.v1();
				RM model = updateElement.transformToRestSync(ac, 0);
				info = new ResultInfo(model, b);
				if (isUpdated) {
					b.add(updateElement.onUpdated());
				}
			} else {
				Tuple<T, EventQueueBatch> tuple = database.tx(() -> {
					EventQueueBatch batch = EventQueueBatch.create();
					created.set(true);
					return Tuple.tuple(root.create(ac, batch, uuid), batch);
				});

				EventQueueBatch b = tuple.v2();
				T createdElement = tuple.v1();
				RM model = createdElement.transformToRestSync(ac, 0);
				String path = createdElement.getAPIPath(ac);
				info = new ResultInfo(model, b);
				info.setProperty("path", path);
				createdElement.onCreated();
				ac.setLocation(path);
			}

			// 3. The updating transaction has succeeded. Now lets store it in the index
			final ResultInfo info2 = info;
			return database.tx(() -> {
				info2.getBatch().dispatch();
				return info2.getModel();
			});
		}, model -> ac.send(model, created.get() ? CREATED : OK));
	}

	/**
	 * Read the element with the given element by loading it from the specified root vertex.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of the element which should be loaded
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 * @param perm
	 *            Permission to check against when loading the element
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void readElement(InternalActionContext ac, String uuid,
		TxAction1<RootVertex<T>> handler, GraphPermission perm) {
		asyncTx(ac, (tx) -> {
			RootVertex<T> root = handler.handle();
			T element = root.loadObjectByUuid(ac, uuid, perm);

			// Handle etag
			if (ac.getGenericParameters().getETag()) {
				String etag = element.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
			}
			return element.transformToRestSync(ac, 0);
		}, (model) -> ac.send(model, OK));

	}

	/**
	 * Read a list of elements of the given root vertex and respond with a list response.
	 * 
	 * @param ac
	 * @param handler
	 *            Handler which provides the root vertex which should be used when loading the element
	 */
	public <T extends MeshCoreVertex<RM, T>, RM extends RestModel> void readElementList(InternalActionContext ac, TxAction1<RootVertex<T>> handler) {
		asyncTx(ac, (tx) -> {
			RootVertex<T> root = handler.handle();

			PagingParameters pagingInfo = ac.getPagingParameters();
			TransformablePage<? extends T> page = root.findAll(ac, pagingInfo);

			// Handle etag
			if (ac.getGenericParameters().getETag()) {
				String etag = page.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
			}
			return page.transformToRest(ac, 0).blockingGet();
		}, (e) -> ac.send(e, OK));
	}

	/**
	 * Asynchronously execute the handler within a scope of a no tx transaction.
	 * 
	 * @param ac
	 * @param handler
	 *            Handler which will be executed within a worker thread
	 * @param action
	 *            Action which will be invoked once the handler has finished
	 */
	@Deprecated
	public <RM extends RestModel> void asyncTx(InternalActionContext ac, TxAction<RM> handler, Consumer<RM> action) {
		async(ac, () -> {
			return database.tx(handler);
		}, action);
	}

	@Deprecated
	public <RM extends RestModel> void asyncTx(InternalActionContext ac, TxAction<RM> handler, Consumer<RM> action, boolean order) {
		async(ac, () -> {
			return database.tx(handler);
		}, action, order);
	}

	@Deprecated
	public <RM extends RestModel> void asyncTx(InternalActionContext ac, TxAction0 handler, Consumer<RM> action) {
		async(ac, () -> {
			database.tx(handler);
			return null;
		}, action);
	}

	@Deprecated
	public <RM extends RestModel> void asyncTx(InternalActionContext ac, TxAction1<RM> handler, Consumer<RM> action) {
		async(ac, () -> {
			return database.tx(handler);
		}, action);
	}

	@Deprecated
	public <RM extends RestModel> void asyncTx(InternalActionContext ac, TxAction2 handler, Consumer<RM> action) {
		async(ac, () -> {
			database.tx(handler);
			return null;
		}, action);
	}

	@Deprecated
	private <RM extends RestModel> void async(InternalActionContext ac, TxAction1<RM> handler, Consumer<RM> action) {
		async(ac, handler, action, false);
	}

	/**
	 * Asynchronously execute the handler.
	 * 
	 * @param ac
	 * @param handler
	 * @param action
	 */
	@Deprecated
	private <RM extends RestModel> void async(InternalActionContext ac, TxAction1<RM> handler, Consumer<RM> action, boolean order) {
		Mesh.vertx().executeBlocking(bc -> {
			try {
				bc.complete(handler.handle());
			} catch (Exception e) {
				bc.fail(e);
			}
		}, order, (AsyncResult<RM> rh) -> {
			if (rh.failed()) {
				ac.fail(rh.cause());
			} else {
				action.accept(rh.result());
			}
		});
	}

}
