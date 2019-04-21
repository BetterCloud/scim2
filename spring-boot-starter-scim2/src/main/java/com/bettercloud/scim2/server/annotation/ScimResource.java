package com.bettercloud.scim2.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for SCIM resource classes.
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ScimResource {

    /**
     * The description for the object.
     *
     * @return The object's description.
     */
    String description();

    /**
     * The name for the object.  This is a human readable
     * name.
     *
     * @return The object's human-readable name.
     */
    String name();

    /**
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
     *
     * @return The primary/base resource class.
     */
    Class<?> schema();

    /**
     * The required schema extension resource classes.
     *
     * @return The required schema extension resource classes.
     */
    Class<?>[] requiredSchemaExtensions() default {};

    /**
     * The optional schema extension resource classes.
     *
     * @return The optional schema extension resource classes.
     */
    Class<?>[] optionalSchemaExtensions() default {};

    /**
     * Whether this resource type and its associated schemas should be
     * discoverable using the SCIM 2 standard /resourceTypes and /schemas
     * endpoints.
     *
     * @return A flag indicating the discoverability of this resource type and
     * its associated schemas.
     */
    boolean discoverable() default true;
}