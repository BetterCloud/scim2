package com.bettercloud.scim2.server.controller.discovery;

import com.bettercloud.scim2.common.GenericScimResource;
import com.bettercloud.scim2.common.types.SchemaResource;
import com.bettercloud.scim2.common.utils.ApiConstants;
import com.bettercloud.scim2.server.BaseUrlProvider;
import com.bettercloud.scim2.server.ResourceTypeDefinition;
import com.bettercloud.scim2.server.annotation.ScimResource;
import com.bettercloud.scim2.server.config.Scim2Properties;
import com.bettercloud.scim2.server.resourcetypes.ResourceTypeRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A SCIM server provides a set of resources, the allowable contents of
 * which are defined by a set of schema URIs and a resource type.
 * SCIM's schema is not a document-centric one such as with
 * [XML-Schema].  Instead, SCIM's support of schema is attribute based,
 * where each attribute may have different type, mutability,
 * cardinality, or returnability.  Validation of documents and messages
 * is always performed by an intended receiver, as specified by the SCIM
 * specifications.  Validation is performed by the receiver in the
 * context of a SCIM protocol request (see [RFC7644]).  For example, a
 * SCIM service provider, upon receiving a request to replace an
 * existing resource with a replacement JSON object, evaluates each
 * asserted attribute based on its characteristics as defined in the
 * relevant schema (e.g., mutability) and decides which attributes may
 * be replaced or ignored.
 * <p>
 * This specification provides a minimal core schema for representing
 * users and groups (resources), encompassing common attributes found in
 * many existing deployments and schemas.  In addition to the minimal
 * core schema, this document also specifies a standardized means by
 * which service providers may extend schemas to define new resources
 * and attributes in both standardized and service-provider-specific
 * cases.
 * <p>
 * Resources are categorized into common resource types such as "User"
 * or "Group".  Collections of resources of the same type are usually
 * contained within the same "container" ("folder") endpoint.
 * <p>
 * RFC 7643
 * SCIM Core Schema
 * September 2015
 * https://tools.ietf.org/html/rfc7643#page-29
 */
@ScimResource(description = "SCIM 2.0 Schema", name = "Schema", schema = SchemaResource.class, discoverable = false)
@RestController
@RequestMapping(value = ApiConstants.SCHEMAS_ENDPOINT)
public class SchemasController extends SchemaAwareController {

    @Autowired
    public SchemasController(final BaseUrlProvider baseUrlProvider, final ResourceTypeRegistry resourceTypeRegistry) {
        super(baseUrlProvider, resourceTypeRegistry);
    }

  @Override
  protected List<GenericScimResource> getResources(final Set<ResourceTypeDefinition> resourceDefinitions) {
    return resourceDefinitions.stream()
        .filter(ResourceTypeDefinition::isDiscoverable)
        .map(resourceTypeDefinition -> {
          ArrayList<SchemaResource> ret = new ArrayList<>(resourceTypeDefinition.getSchemaExtensions().keySet());
          if (resourceTypeDefinition.getCoreSchema() != null) {
            ret.add(resourceTypeDefinition.getCoreSchema());
          }
          return ret;
        })
        .flatMap(Collection::stream)
        .map(SchemaResource::asGenericScimResource)
        .collect(Collectors.toList());
  }
}
