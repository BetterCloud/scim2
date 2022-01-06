package com.bettercloud.scim2.server.resourcetypes;

import com.bettercloud.scim2.server.ResourceTypeDefinition;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Set;

@RequiredArgsConstructor
public class SimpleResourceTypeRegistry implements ResourceTypeRegistry {
    final Set<ResourceTypeDefinition> resourceTypes;

    @Override
    public Set<ResourceTypeDefinition> getResourceTypeDefinitions() {
        return Collections.unmodifiableSet(resourceTypes);
    }
}
