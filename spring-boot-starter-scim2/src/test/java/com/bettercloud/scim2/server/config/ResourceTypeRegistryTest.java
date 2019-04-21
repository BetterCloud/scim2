package com.bettercloud.scim2.server.config;

import com.bettercloud.scim2.server.ResourceTypeDefinition;
import com.bettercloud.scim2.server.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {TestApplication.class, ResourceTypeRegistry.class})
public class ResourceTypeRegistryTest {

    @Autowired
    private Set<ResourceTypeDefinition> resourceTypeDefinitions;

    @Test
    public void validateResourceTypeDefinitions() {
        assertEquals(4, resourceTypeDefinitions.size());
        assertTrue(resourceTypeDefinitions.stream()
                                          .anyMatch(resourceTypeDefinition -> resourceTypeDefinition.getName().equals("ServiceProviderConfig")));
        assertTrue(resourceTypeDefinitions.stream().anyMatch(resourceTypeDefinition -> resourceTypeDefinition.getName().equals("Schema")));
        assertTrue(resourceTypeDefinitions.stream().anyMatch(resourceTypeDefinition -> resourceTypeDefinition.getName().equals("ResourceType")));
        assertTrue(resourceTypeDefinitions.stream().anyMatch(resourceTypeDefinition -> resourceTypeDefinition.getName().equals("User")));
    }
}