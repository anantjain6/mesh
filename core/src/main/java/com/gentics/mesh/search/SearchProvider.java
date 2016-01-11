package com.gentics.mesh.search;

import java.util.Map;

import org.elasticsearch.node.Node;

import com.gentics.mesh.core.rest.schema.Schema;

import rx.Observable;

/**
 * A search provider is a service this enables storage and retrieval of indexed documents.
 */
public interface SearchProvider {

	/**
	 * Explicitly refresh one or more indices (making the content indexed since the last refresh searchable).
	 */
	void refreshIndex();

	/**
	 * Create a search index with the given name.
	 * 
	 * @param indexName
	 */
	void createIndex(String indexName);

	/**
	 * Set the mapping for the given type in the given index for the schema
	 *
	 * @param indexName
	 *            index name
	 * @param type
	 *            type name
	 * @param schema
	 *            schema
	 * @return observable
	 */
	Observable<Void> setMapping(String indexName, String type, Schema schema);

	// TODO add a good response instead of void. We need this in oder to handle correct logging?
	/**
	 * Update the document and invoke the handler when the document has been updated or an error occurred.
	 * 
	 * @param index
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid of the document
	 * @param transformToDocumentMap
	 */
	Observable<Void> updateDocument(String index, String type, String uuid, Map<String, Object> transformToDocumentMap);

	/**
	 * Delete the given document and invoke the handler when the document has been deleted or an error occurred.
	 * 
	 * @param index
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid for the document
	 */
	Observable<Void> deleteDocument(String index, String type, String uuid);

	/**
	 * Store the given document and invoke the handler when the document has been stored or an error occurred.
	 * 
	 * @param index
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid for the document
	 * @param map
	 *            Map that holds the document properties
	 */
	Observable<Void> storeDocument(String index, String type, String uuid, Map<String, Object> map);

	/**
	 * Get the given document and invoke the handler when the document has been loaded or an error occurred.
	 * 
	 * @param index
	 *            Index name of the document
	 * @param type
	 *            Index type of the document
	 * @param uuid
	 *            Uuid for the document
	 */
	Observable<Map<String, Object>> getDocument(String index, String type, String uuid);

	/**
	 * Start the search provider.
	 */
	void start();

	/**
	 * Stop the search provider.
	 */
	void stop();

	/**
	 * Reset the search provider.
	 */
	void reset();

	/**
	 * Return the elastic search node.
	 * 
	 * @return Elasticsearch node
	 */
	// TODO get rid of the elastic search dependency within the interface
	Node getNode();

}
