package com.bettercloud.scim2.server.controller.discovery;


import com.bettercloud.scim2.common.GenericScimResource;
import com.bettercloud.scim2.common.types.ResourceTypeResource;
import com.bettercloud.scim2.common.utils.ApiConstants;
import com.bettercloud.scim2.server.ResourceTypeDefinition;
import com.bettercloud.scim2.server.annotation.ScimResource;
import com.bettercloud.scim2.server.config.Scim2Properties;
import com.bettercloud.scim2.server.resourcetypes.ResourceTypeRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Each SCIM resource is a JSON object that has the following
 * components:
 * <p>
 * Resource Type
 * Each resource (or JSON object) in SCIM has a resource type
 * ("meta.resourceType"; see Section 3.1) that defines the resource's
 * core attribute schema and any attribute extension schema, as well
 * as the endpoint where objects of the same type may be found.  More
 * information about a resource MAY be found in its resource type
 * definition (see Section 6).
 * <p>
 * "Schemas" Attribute
 * The "schemas" attribute is a REQUIRED attribute and is an array of
 * Strings containing URIs that are used to indicate the namespaces
 * of the SCIM schemas that define the attributes present in the
 * current JSON structure.  This attribute may be used by parsers to
 * define the attributes present in the JSON structure that is the
 * body to an HTTP request or response.  Each String value must be a
 * unique URI.  All representations of SCIM schemas MUST include a
 * non-empty array with value(s) of the URIs supported by that
 * representation.  The "schemas" attribute for a resource MUST only
 * contain values defined as "schema" and "schemaExtensions" for the
 * resource's defined "resourceType".  Duplicate values MUST NOT be
 * included.  Value order is not specified and MUST NOT impact
 * behavior.
 * <p>
 * Common Attributes
 * A resource's common attributes are those attributes that are part
 * of every SCIM resource, regardless of the value of the "schemas"
 * attribute present in a JSON body.  These attributes are not
 * defined in any particular schema but SHALL be assumed to be
 * present in every resource, regardless of the value of the
 * "schemas" attribute.  See Section 3.1.
 * <p>
 * Core Attributes
 * A resource's core attributes are those attributes that sit at the
 * top level of the JSON object together with the common attributes
 * (such as the resource "id").  The list of valid attributes is
 * specified by the resource's resource type "schema" attribute (see
 * Section 6).  This same value is also present in the resource's
 * "schemas" attribute.
 * <p>
 * Extended Attributes
 * Extended schema attributes are specified by the resource's
 * resource type "schemaExtensions" attribute (see Section 6).
 * Unlike core attributes, extended attributes are kept in their own
 * sub-attribute namespace identified by the schema extension URI.
 * This avoids attribute name conflicts that may arise due to
 * conflicts from separate schema extensions.
 * <p>
 * RFC 7643
 * SCIM Core Schema
 * September 2015
 * https://tools.ietf.org/html/rfc7643#page-29
 */
@ScimResource(description = "SCIM 2.0 Resource Type", name = "ResourceType", schema = ResourceTypeResource.class, discoverable = false)
@RestController
@RequestMapping(value = ApiConstants.RESOURCE_TYPES_ENDPOINT)
public class ResourceTypesController extends SchemaAwareController {

    @Autowired
    public ResourceTypesController(final Scim2Properties scim2Properties, final ResourceTypeRegistry resourceTypeRegistry) {
        super(scim2Properties, resourceTypeRegistry);
    }

    /**
     * Return a list of resource type definitions to be returned by the API.  Only resources marked as discoverable are returned.
     *
     * @param resourceDefinitions Set of all resource type definitions defined in the server.
     *
     * @return List of {@link GenericScimResource} that represent the discoverable resource types.
     */
    @Override
    protected List<GenericScimResource> getResources(final Set<ResourceTypeDefinition> resourceDefinitions) {
        return resourceDefinitions.stream()
                                  .filter(ResourceTypeDefinition::isDiscoverable)
                                  .map(ResourceTypeDefinition::toScimResource)
                                  .map(ResourceTypeResource::asGenericScimResource)
                                  .collect(Collectors.toList());
    }
}