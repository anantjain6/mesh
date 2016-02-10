package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Change entry which contains information for a field update. This includes field specific settings.
 */
public class UpdateFieldChangeImpl extends AbstractSchemaFieldChange implements UpdateFieldChange {

	public static final SchemaChangeOperation OPERATION = SchemaChangeOperation.UPDATEFIELD;

	@Override
	public FieldSchemaContainer apply(FieldSchemaContainer container) {
		Optional<FieldSchema> fieldSchema = container.getFieldSchema(getFieldName());

		if (!fieldSchema.isPresent()) {
			throw error(BAD_REQUEST, "Could not find schema field {" + getFieldName() + "} within schema {" + container.getName() + "}");
		}
		// Remove prefix from map keys
		Map<String, Object> properties = new HashMap<>();
		for (String key : getFieldProperties().keySet()) {
			Object value = getFieldProperties().get(key);
			key = key.replace(FIELD_PROPERTY_PREFIX_KEY, "");
			properties.put(key, value);
		}
		fieldSchema.get().apply(properties);
		return container;
	}

}
