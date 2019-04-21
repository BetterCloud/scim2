package com.bettercloud.scim2.server;

import com.bettercloud.scim2.common.Path;
import com.bettercloud.scim2.common.types.AttributeDefinition;
import com.bettercloud.scim2.common.types.ResourceTypeResource;
import com.bettercloud.scim2.common.types.SchemaResource;
import com.bettercloud.scim2.common.utils.SchemaUtils;
import com.bettercloud.scim2.server.annotation.ScimResource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Declaration of a resource type including all schemas.
 */
@Slf4j
@Getter
public final class ResourceTypeDefinition {
    private final String id;
    private final String name;
    private final String description;
    private final String endpoint;
    private final SchemaResource coreSchema;
    private final Map<SchemaResource, Boolean> schemaExtensions;
    private final Map<Path, AttributeDefinition> attributeNotationMap;
    private final boolean discoverable;

    /**
     * Create a new ResourceType.
     *
     * @param id               Unique id of the resource.
     * @param name             Name of the resource.
     * @param description      Description of the resource.
     * @param endpoint         The Uri of the resource.
     * @param coreSchema       The core schema for the resource type.
     * @param schemaExtensions A map of schema extensions to whether it is
     *                         required for the resource type.
     * @param discoverable     Flag that defines if the resource with be returned by the ResourceTypes API.
     */
    public ResourceTypeDefinition(final String id,
                                  final String name,
                                  final String description,
                                  final String endpoint,
                                  final SchemaResource coreSchema,
                                  final Map<SchemaResource, Boolean> schemaExtensions,
                                  final boolean discoverable) {

        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint must not be null");
        }
        this.name = name;
        this.endpoint = endpoint;

        this.id = id;
        this.description = description;
        this.coreSchema = coreSchema;
        this.schemaExtensions = Collections.unmodifiableMap(schemaExtensions);
        this.discoverable = discoverable;
        this.attributeNotationMap = new HashMap<>();

        // Add the common attributes
        buildAttributeNotationMap(Path.root(), SchemaUtils.COMMON_ATTRIBUTE_DEFINITIONS);

        // Add the core attributes
        if (coreSchema != null) {
            buildAttributeNotationMap(Path.root(), coreSchema.getAttributes());
        }

        // Add the extension attributes
        for (SchemaResource schemaExtension : schemaExtensions.keySet()) {
            buildAttributeNotationMap(Path.root(schemaExtension.getId()), schemaExtension.getAttributes());
        }
    }

    void buildAttributeNotationMap(final Path parentPath, final Collection<AttributeDefinition> attributes) {
        for (AttributeDefinition attribute : attributes) {
            Path path = parentPath.attribute(attribute.getName());
            attributeNotationMap.put(path, attribute);
            if (attribute.getSubAttributes() != null) {
                buildAttributeNotationMap(path, attribute.getSubAttributes());
            }
        }
    }

    /**
     * Retrieve the attribute definition for the attribute in the path.
     *
     * @param path The attribute path.
     *
     * @return The attribute definition or {@code null} if there is no attribute
     * defined for the path.
     */
    public AttributeDefinition getAttributeDefinition(final Path path) {
        return attributeNotationMap.get(normalizePath(path).withoutFilters());
    }

    /**
     * Normalize a path by removing the schema URN for core attributes.
     *
     * @param path The path to normalize.
     *
     * @return The normalized path.
     */
    public Path normalizePath(final Path path) {
        if (path.getSchemaUrn() != null && coreSchema != null && path.getSchemaUrn().equalsIgnoreCase(coreSchema.getId())) {
            return Path.root().attribute(path);
        }
        return path;
    }

    /**
     * Retrieve the ResourceType SCIM resource that represents this definition.
     *
     * @return The ResourceType SCIM resource that represents this definition.
     */
    public ResourceTypeResource toScimResource() {
        URI coreSchemaUri = null;
        if (coreSchema != null) {
            try {
                coreSchemaUri = new URI(coreSchema.getId());
            } catch (URISyntaxException e) {
                log.error("Core schema id is not a valid URI", e);
            }
        }
        List<ResourceTypeResource.SchemaExtension> schemaExtensionList = null;
        if (schemaExtensions.size() > 0) {
            schemaExtensionList = new ArrayList<>(schemaExtensions.size());

            for (Map.Entry<SchemaResource, Boolean> schemaExtension : schemaExtensions.entrySet()) {
                schemaExtensionList.add(
                        new ResourceTypeResource.SchemaExtension(URI.create(schemaExtension.getKey().getId()), schemaExtension.getValue()));
            }
        }

        return new ResourceTypeResource(id == null ? name : id, name, description, URI.create(endpoint), coreSchemaUri, schemaExtensionList);

    }

    public static ResourceTypeDefinition fromScimResource(final Class<?> scimResource) {
        Class<?> c = scimResource;
        ScimResource resourceType;
        do {
            resourceType = c.getAnnotation(ScimResource.class);
            c = c.getSuperclass();
        } while (c != null && resourceType == null);

        c = scimResource;
        RequestMapping mapping;
        do {
            mapping = c.getAnnotation(RequestMapping.class);
            c = c.getSuperclass();
        } while (c != null && mapping == null);

        if (resourceType == null || mapping == null) {
            return null;
        }

        try {
            Map<SchemaResource, Boolean> schemaExtensions = new HashMap<>();

            for (Class<?> optionalSchemaExtension : resourceType.optionalSchemaExtensions()) {
                schemaExtensions.put(SchemaUtils.getSchema(optionalSchemaExtension), false);
            }

            for (Class<?> requiredSchemaExtension : resourceType.requiredSchemaExtensions()) {
                schemaExtensions.put(SchemaUtils.getSchema(requiredSchemaExtension), true);
            }

            return new ResourceTypeDefinition(null, resourceType.name(), resourceType.description(), mapping.value()[0],
                                              SchemaUtils.getSchema(resourceType.schema()), schemaExtensions, resourceType.discoverable());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static ResourceTypeDefinition fromScimResource(ScimResource resourceType, RequestMapping mapping) {

        if (resourceType == null || mapping == null) {
            return null;
        }

        try {
            Map<SchemaResource, Boolean> schemaExtensions = new HashMap<>();

            for (Class<?> optionalSchemaExtension : resourceType.optionalSchemaExtensions()) {
                schemaExtensions.put(SchemaUtils.getSchema(optionalSchemaExtension), false);
            }

            for (Class<?> requiredSchemaExtension : resourceType.requiredSchemaExtensions()) {
                schemaExtensions.put(SchemaUtils.getSchema(requiredSchemaExtension), true);
            }

            return new ResourceTypeDefinition(null, resourceType.name(), resourceType.description(), mapping.value()[0],
                                              SchemaUtils.getSchema(resourceType.schema()), schemaExtensions, resourceType.discoverable());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
