package com.bettercloud.scim2.server;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bettercloud.scim2.common.Path;
import com.bettercloud.scim2.common.utils.JsonUtils;
import com.bettercloud.scim2.common.utils.SchemaUtils;

import java.util.Iterator;
import java.util.Map;


/**
 * An abstract class which may be implemented to trim resources down to
 * selected attributes.
 */
public abstract class ResourceTrimmer {
    /**
     * Trim attributes of the object node to return.
     *
     * @param objectNode The object node to return.
     *
     * @return The trimmed object node ready to return to the client.
     */
    public ObjectNode trimObjectNode(final ObjectNode objectNode) {
        return trimObjectNode(objectNode, Path.root());
    }

    /**
     * Trim attributes of an inner object node to return.
     *
     * @param objectNode The object node to return.
     * @param parentPath The parent path of attributes in the object.
     *
     * @return The trimmed object node ready to return to the client.
     */
    private ObjectNode trimObjectNode(final ObjectNode objectNode, final Path parentPath) {
        final ObjectNode objectToReturn = JsonUtils.getJsonNodeFactory().objectNode();
        final Iterator<Map.Entry<String, JsonNode>> i = objectNode.fields();
        while (i.hasNext()) {
            Map.Entry<String, JsonNode> field = i.next();
            processEntry(parentPath, objectToReturn, field);
        }
        return objectToReturn;
    }

    private void processEntry(final Path parentPath, ObjectNode objectToReturn, Map.Entry<String, JsonNode> field) {
        final Path path;
        if (parentPath.isRoot() && parentPath.getSchemaUrn() == null && SchemaUtils.isUrn(field.getKey())) {
            path = Path.root(field.getKey());
        } else {
            path = parentPath.attribute(field.getKey());
        }

        if (path.isRoot() || shouldReturn(path)) {
            if (field.getValue().isArray()) {
                ArrayNode trimmedNode = trimArrayNode((ArrayNode) field.getValue(), path);
                if (trimmedNode.size() > 0) {
                    objectToReturn.set(field.getKey(), trimmedNode);
                }
            } else if (field.getValue().isObject()) {
                ObjectNode trimmedNode = trimObjectNode((ObjectNode) field.getValue(), path);
                if (trimmedNode.size() > 0) {
                    objectToReturn.set(field.getKey(), trimmedNode);
                }
            } else {
                objectToReturn.set(field.getKey(), field.getValue());
            }
        }
    }

    /**
     * Trim attributes of the values in the array node to return.
     *
     * @param arrayNode  The array node to return.
     * @param parentPath The parent path of attributes in the array.
     *
     * @return The trimmed object node ready to return to the client.
     */
    protected ArrayNode trimArrayNode(final ArrayNode arrayNode, final Path parentPath) {
        final ArrayNode arrayToReturn = JsonUtils.getJsonNodeFactory().arrayNode();
        for (JsonNode value : arrayNode) {
            if (value.isArray()) {
                ArrayNode trimmedNode = trimArrayNode((ArrayNode) value, parentPath);
                if (trimmedNode.size() > 0) {
                    arrayToReturn.add(trimmedNode);
                }
            } else if (value.isObject()) {
                ObjectNode trimmedNode = trimObjectNode((ObjectNode) value, parentPath);
                if (trimmedNode.size() > 0) {
                    arrayToReturn.add(trimmedNode);
                }
            } else {
                arrayToReturn.add(value);
            }
        }
        return arrayToReturn;
    }

    /**
     * Determine if the attribute specified by the path should be returned.
     *
     * @param path The path for the attribute.
     *
     * @return {@code true} to return the attribute or {@code false} to remove the
     * attribute from the returned resource..
     */
    public abstract boolean shouldReturn(final Path path);
}