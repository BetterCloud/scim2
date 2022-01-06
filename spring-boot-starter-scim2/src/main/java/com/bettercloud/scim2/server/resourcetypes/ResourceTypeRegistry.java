package com.bettercloud.scim2.server.resourcetypes;

import com.bettercloud.scim2.server.ResourceTypeDefinition;

import java.util.Set;

public interface ResourceTypeRegistry {

    Set<ResourceTypeDefinition> getResourceTypeDefinitions();

}