package com.bettercloud.scim2.server;


import com.bettercloud.scim2.common.Path;
import com.bettercloud.scim2.common.types.AttributeDefinition;

import java.util.Set;

/**
 * A resource trimmer implementing the SCIM standard for returning attributes.
 */
public class ScimResourceTrimmer extends ResourceTrimmer {
    private final ResourceTypeDefinition resourceType;
    private final Set<Path> requestAttributes;
    private final Set<Path> queryAttributes;
    private final boolean excluded;

    /**
     * Create a new SCIMResourceTrimmer.
     *
     * @param resourceType      The resource type definition for resources to
     *                          trim.
     * @param requestAttributes The attributes in the request object or
     *                          {@code null} for
     *                          other requests.
     * @param queryAttributes   The attributes from the 'attributes' or
     *                          'excludedAttributes' query parameter.
     * @param excluded          {@code true} if the queryAttributes came from
     *                          the excludedAttributes query parameter.
     */
    public ScimResourceTrimmer(final ResourceTypeDefinition resourceType,
                               final Set<Path> requestAttributes,
                               final Set<Path> queryAttributes,
                               final boolean excluded) {
        this.resourceType = resourceType;
        this.requestAttributes = requestAttributes;
        this.queryAttributes = queryAttributes;
        this.excluded = excluded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldReturn(final Path path) {
        AttributeDefinition attributeDefinition = resourceType.getAttributeDefinition(path);
        AttributeDefinition.Returned returned = attributeDefinition == null
                                                ? AttributeDefinition.Returned.DEFAULT
                                                : attributeDefinition.getReturned();

        switch (returned) {
            case ALWAYS:
                return true;
            case NEVER:
                return false;
            case REQUEST:
                // Return only if it was one of the request attributes or if there are
                // no request attributes, then only if it was one of the override query
                // attributes.
                return pathContains(requestAttributes, path) || (requestAttributes.isEmpty() && !excluded && pathContains(queryAttributes, path));
            default:
                // Return if it is not one of the excluded query attributes and no
                // override query attributes are provided. If override query attributes
                // are provided, only return if it is one of them.
                if (excluded) {
                    return !pathContains(queryAttributes, path);
                } else {
                    return queryAttributes.isEmpty() || pathContains(queryAttributes, path);
                }
        }
    }

    private boolean pathContains(final Set<Path> paths, final Path path) {
        // Exact path match
        if (paths.contains(path)) {
            return true;
        }

        if (!excluded) {
            // See if a sub-attribute of the given path is included in the list
            // ie. include name if name.givenName is in the list.
            for (Path p : paths) {
                if (p.size() > path.size() && path.equals(p.subPath(path.size()))) {
                    return true;
                }
            }
        }

        // See if the parent attribute of the given path is included in the list
        // ie. include name.{anything} if name is in the list.
        for (Path p = path; p.size() > 0; p = p.subPath(p.size() - 1)) {
            if (paths.contains(p)) {
                return true;
            }
        }

        return false;
    }
}