package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.admin.MeshStatusResponse;
import com.gentics.mesh.core.rest.admin.MigrationStatusResponse;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.MeshRequest;

public interface AdminClientMethods {

	/**
	 * Return the mesh status.
	 * 
	 * @return
	 */
	MeshRequest<MeshStatusResponse> meshStatus();

	/**
	 * Return the migration status.
	 * 
	 * @return
	 */
	MeshRequest<MigrationStatusResponse> migrationStatus();

	/**
	 * Invoke a graph database backup.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeBackup();

	/**
	 * Invoke a graph database export.
	 * 
	 * @return
	 * @deprecated Endpoint currently not supported
	 */
	@Deprecated
	MeshRequest<GenericMessageResponse> invokeExport();

	/**
	 * Invoke a graph database restore.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> invokeRestore();

	/**
	 * Invoke a graph database import.
	 * 
	 * @return
	 * @deprecated Endpoint currently not supported
	 */
	@Deprecated
	MeshRequest<GenericMessageResponse> invokeImport();

	/**
	 * Invoke a consistency check of the graph database.
	 * 
	 * @return
	 */
	MeshRequest<ConsistencyCheckResponse> checkConsistency();

}
