package com.bettercloud.scim2.server;

import com.unboundid.scim2.common.GenericScimResource;
import com.unboundid.scim2.common.Path;
import com.unboundid.scim2.common.ScimResource;
import com.unboundid.scim2.common.exceptions.BadRequestException;
import com.unboundid.scim2.common.messages.PatchOperation;
import com.unboundid.scim2.common.types.Meta;
import com.unboundid.scim2.common.utils.StaticUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility to prepare a resource to return to the client. This includes:
 *
 * <ul>
 * <li>
 * Returning the attributes based on the returned constraint of the
 * attribute definition in the schema.
 * </li>
 * <li>
 * Returning the attributes requested by the client using the request
 * resource as well as the attributes or excludedAttributes query parameter.
 * </li>
 * <li>
 * Setting the meta.resourceType and meta.location attributes if not
 * already set.
 * </li>
 * </ul>
 */
public class ResourcePreparer<T extends ScimResource> {
    private final ResourceTypeDefinition resourceType;
    private final URI baseUri;
    private final Set<Path> queryAttributes;
    private final boolean excluded;

    /**
     * Create a new ResourcePreparer for preparing returned resources for a
     * SCIM operation.
     *
     * @param resourceType             The resource type definition for resources to prepare.
     * @param attributesString         The attributes query param.
     * @param excludedAttributesString The excludedAttributes query param.
     * @param baseUri                  The resource type base URI.
     *
     * @throws BadRequestException This will be thrown if an attribute is not valid.
     */
    public ResourcePreparer(final ResourceTypeDefinition resourceType,
                            final String attributesString,
                            final String excludedAttributesString,
                            final URI baseUri) throws BadRequestException {
        if (attributesString != null && !attributesString.isEmpty()) {
            Set<String> attributeSet = StaticUtils.arrayToSet(StaticUtils.splitCommaSeparatedString(attributesString));
            this.queryAttributes = new LinkedHashSet<>(attributeSet.size());
            for (String attribute : attributeSet) {
                Path normalizedPath;
                try {
                    normalizedPath = resourceType.normalizePath(Path.fromString(attribute)).withoutFilters();
                } catch (BadRequestException e) {
                    throw BadRequestException.invalidValue(
                            "'" + attribute + "' is not a valid value for the attributes parameter: " + e.getMessage());
                }
                this.queryAttributes.add(normalizedPath);

            }
            this.excluded = false;
        } else if (excludedAttributesString != null && !excludedAttributesString.isEmpty()) {
            Set<String> attributeSet = StaticUtils.arrayToSet(StaticUtils.splitCommaSeparatedString(excludedAttributesString));
            this.queryAttributes = new LinkedHashSet<>(attributeSet.size());
            for (String attribute : attributeSet) {
                Path normalizedPath;
                try {
                    normalizedPath = resourceType.normalizePath(Path.fromString(attribute)).withoutFilters();
                } catch (BadRequestException e) {
                    throw BadRequestException.invalidValue(
                            "'" + attribute + "' is not a valid value for the excludedAttributes parameter: " + e.getMessage());
                }
                this.queryAttributes.add(normalizedPath);
            }
            this.excluded = true;
        } else {
            this.queryAttributes = Collections.emptySet();
            this.excluded = true;
        }
        this.resourceType = resourceType;
        this.baseUri = baseUri;
    }

    /**
     * Trim attributes of the resources returned from a search or retrieve
     * operation based on schema and the request parameters.
     *
     * @param returnedResource The resource to return.
     *
     * @return The trimmed resource ready to return to the client.
     */
    public GenericScimResource trimRetrievedResource(final T returnedResource) {
        return trimReturned(returnedResource, null, null);
    }

    /**
     * Trim attributes of the resources returned from a create operation based on
     * schema as well as the request resource and request parameters.
     *
     * @param returnedResource The resource to return.
     * @param requestResource  The resource in the create request or
     *                         {@code null} if not available.
     *
     * @return The trimmed resource ready to return to the client.
     */
    public GenericScimResource trimCreatedResource(final T returnedResource, final T requestResource) {
        return trimReturned(returnedResource, requestResource, null);
    }

    /**
     * Trim attributes of the resources returned from a replace operation based on
     * schema as well as the request resource and request parameters.
     *
     * @param returnedResource The resource to return.
     * @param requestResource  The resource in the replace request or
     *                         {@code null} if not available.
     *
     * @return The trimmed resource ready to return to the client.
     */
    public GenericScimResource trimReplacedResource(final T returnedResource, final T requestResource) {
        return trimReturned(returnedResource, requestResource, null);
    }

    /**
     * Trim attributes of the resources returned from a modify operation based on
     * schema as well as the patch request and request parameters.
     *
     * @param returnedResource The resource to return.
     * @param patchOperations  The operations in the patch request or
     *                         {@code null} if not available.
     *
     * @return The trimmed resource ready to return to the client.
     */
    public GenericScimResource trimModifiedResource(final T returnedResource, final Iterable<PatchOperation> patchOperations) {
        return trimReturned(returnedResource, null, patchOperations);
    }

    /**
     * Sets the meta.resourceType and meta.location metadata attribute values.
     *
     * @param returnedResource The resource to set the attributes.
     */
    public void setResourceTypeAndLocation(final T returnedResource) {
        Meta meta = returnedResource.getMeta();

        boolean metaUpdated = false;
        if (meta == null) {
            meta = new Meta();
        }

        if (meta.getResourceType() == null) {
            meta.setResourceType(resourceType.getName());
            metaUpdated = true;
        }

        if (meta.getLocation() == null) {
            String id = returnedResource.getId();
            if (id != null) {
                UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUri);
                builder.path("/");
                builder.path(id);
                meta.setLocation(builder.build().toUri());
            } else {
                meta.setLocation(baseUri);
            }
            metaUpdated = true;
        }

        if (metaUpdated) {
            returnedResource.setMeta(meta);
        }
    }

    /**
     * Trim attributes of the resources to return based on schema and the client
     * request.
     *
     * @param returnedResource The resource to return.
     * @param requestResource  The resource in the PUT or POST request or
     *                         {@code null} for other requests.
     * @param patchOperations  The patch operations in the PATCH request or
     *                         {@code null} for other requests.
     *
     * @return The trimmed resource ready to return to the client.
     */
    @SuppressWarnings("unchecked")
    private GenericScimResource trimReturned(final T returnedResource, final T requestResource, final Iterable<PatchOperation> patchOperations) {
        Set<Path> requestAttributes = Collections.emptySet();
        if (requestResource != null) {
            ObjectNode requestObject = requestResource.asGenericScimResource().getObjectNode();
            requestAttributes = new LinkedHashSet<>();
            collectAttributes(Path.root(), requestAttributes, requestObject);
        }

        if (patchOperations != null) {
            requestAttributes = new LinkedHashSet<>();
            collectAttributes(requestAttributes, patchOperations);
        }

        setResourceTypeAndLocation(returnedResource);
        GenericScimResource genericReturnedResource = returnedResource.asGenericScimResource();
        ScimResourceTrimmer trimmer = new ScimResourceTrimmer(resourceType, requestAttributes, queryAttributes, excluded);
        return new GenericScimResource(trimmer.trimObjectNode(genericReturnedResource.getObjectNode()));
    }

    /**
     * Collect a list of attributes in the object node.
     *
     * @param parentPath The parent path of attributes in the object.
     * @param paths      The set of paths to add to.
     * @param objectNode The object node to collect from.
     */
    private void collectAttributes(final Path parentPath, final Set<Path> paths, final ObjectNode objectNode) {
        Iterator<Map.Entry<String, JsonNode>> i = objectNode.fields();
        while (i.hasNext()) {
            Map.Entry<String, JsonNode> field = i.next();
            Path path = parentPath.attribute(field.getKey());
            if (path.size() > 1 || path.getSchemaUrn() == null) {
                // Don't add a path for the extension schema object itself.
                paths.add(path);
            }
            if (field.getValue().isArray()) {
                collectAttributes(path, paths, (ArrayNode) field.getValue());
            } else if (field.getValue().isObject()) {
                collectAttributes(path, paths, (ObjectNode) field.getValue());
            }
        }
    }

    /**
     * Collect a list of attributes in the array node.
     *
     * @param parentPath The parent path of attributes in the array.
     * @param paths      The set of paths to add to.
     * @param arrayNode  The array node to collect from.
     */
    private void collectAttributes(final Path parentPath, final Set<Path> paths, final ArrayNode arrayNode) {
        for (JsonNode value : arrayNode) {
            if (value.isArray()) {
                collectAttributes(parentPath, paths, (ArrayNode) value);
            } else if (value.isObject()) {
                collectAttributes(parentPath, paths, (ObjectNode) value);
            }
        }
    }

    /**
     * Collect a list of attributes in the patch operation.
     *
     * @param paths           The set of paths to add to.
     * @param patchOperations The patch operation to collect attributes from.
     */
    private void collectAttributes(final Set<Path> paths, final Iterable<PatchOperation> patchOperations) {
        for (PatchOperation patchOperation : patchOperations) {
            Path path = Path.root();
            if (patchOperation.getPath() != null) {
                path = resourceType.normalizePath(patchOperation.getPath()).
                        withoutFilters();
                paths.add(path);
            }
            if (patchOperation.getJsonNode() != null) {
                if (patchOperation.getJsonNode().isArray()) {
                    collectAttributes(path, paths, (ArrayNode) patchOperation.getJsonNode());
                } else if (patchOperation.getJsonNode().isObject()) {
                    collectAttributes(path, paths, (ObjectNode) patchOperation.getJsonNode());
                }
            }
        }
    }
}