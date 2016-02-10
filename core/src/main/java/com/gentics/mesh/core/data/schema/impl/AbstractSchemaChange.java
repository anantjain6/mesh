package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CHANGE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see SchemaChange
 */
public abstract class AbstractSchemaChange<T extends FieldSchemaContainer> extends MeshVertexImpl implements SchemaChange<T> {
	private static final Logger log = LoggerFactory.getLogger(AbstractSchemaChange.class);

	private static String OPERATION_NAME_PROPERTY_KEY = "operation";

	private static String MIGRATION_SCRIPT_PROPERTY_KEY = "migrationScript";

	public static void checkIndices(Database database) {
		database.addVertexType(AbstractSchemaChange.class);
	}

	@Override
	public SchemaChange<?> getNextChange() {
		return (SchemaChange) out(HAS_CHANGE).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setNextChange(SchemaChange<?> change) {
		setUniqueLinkOutTo(change.getImpl(), HAS_CHANGE);
		return this;
	}

	@Override
	public SchemaChange<?> getPreviousChange() {
		return (SchemaChange) in(HAS_CHANGE).nextOrDefault(null);
	}

	@Override
	public SchemaChange<T> setPreviousChange(SchemaChange<?> change) {
		setUniqueLinkInTo(change.getImpl(), HAS_CHANGE);
		return this;
	}

	@Override
	public SchemaChange setOperation(SchemaChangeOperation operation) {
		setProperty(OPERATION_NAME_PROPERTY_KEY, operation.name());
		return this;
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return getProperty(OPERATION_NAME_PROPERTY_KEY);
	}

	@Override
	public SchemaContainer getPreviousSchemaContainer() {
		return in(HAS_SCHEMA_CONTAINER).has(SchemaContainerImpl.class).nextOrDefaultExplicit(SchemaContainerImpl.class, null);
	}

	@Override
	public SchemaChange<T> setPreviousContainer(GraphFieldSchemaContainer<?, ?, ?> container) {
		setSingleLinkInTo(container.getImpl(), HAS_SCHEMA_CONTAINER);
		return this;
	}

	@Override
	public SchemaContainer getNextContainer() {
		return out(HAS_SCHEMA_CONTAINER).has(SchemaContainerImpl.class).nextOrDefaultExplicit(SchemaContainerImpl.class, null);
	}

	@Override
	public SchemaChange<T> setNextSchemaContainer(GraphFieldSchemaContainer<?, ?, ?> container) {
		setSingleLinkOutTo(container.getImpl(), HAS_SCHEMA_CONTAINER);
		return this;
	}

	@Override
	public String getMigrationScript() throws IOException {
		String migrationScript = getProperty(MIGRATION_SCRIPT_PROPERTY_KEY, String.class);
		if (migrationScript == null) {
			migrationScript = getAutoMigrationScript();
		}

		if (migrationScript != null) {
			migrationScript = "node = JSON.parse(node);\n" + migrationScript + "\nnode = JSON.stringify(node);";
		}

		return migrationScript;
	}

	@Override
	public SchemaChange<T> setCustomMigrationScript(String migrationScript) {
		setProperty(MIGRATION_SCRIPT_PROPERTY_KEY, migrationScript);
		return this;
	}

	@Override
	public String getAutoMigrationScript() throws IOException {
		return null;
	}

	@Override
	public List<Tuple<String, Object>> getMigrationScriptContext() {
		return null;
	}

	/**
	 * Load the automatic migration script with given name
	 * 
	 * @param scriptName
	 *            script name
	 * @return script file contents
	 * @throws IOException
	 */
	protected String loadAutoMigrationScript(String scriptName) throws IOException {
		try (InputStream ins = getClass().getResourceAsStream("/script/" + scriptName)) {
			if (ins == null) {
				log.error("Json could not be loaded from classpath file {" + scriptName + "}");
				throw new FileNotFoundException("Could not find script file {" + scriptName + "}");
			} else {
				StringWriter writer = new StringWriter();
				try {
					IOUtils.copy(ins, writer);
					return writer.toString();
				} catch (IOException e) {
					log.error("Error while reading script file {" + scriptName + "}", e);
					throw e;
				}
			}
		}
	}
}
