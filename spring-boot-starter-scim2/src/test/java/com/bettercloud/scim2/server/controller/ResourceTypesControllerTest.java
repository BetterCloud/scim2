package com.bettercloud.scim2.server.controller;

import com.unboundid.scim2.common.GenericScimResource;
import com.unboundid.scim2.common.ScimResource;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.messages.ListResponse;
import com.bettercloud.scim2.server.controller.discovery.ResourceTypesController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ResourceTypesControllerTest {

    @Autowired
    private ResourceTypesController resourceTypesController;

    @Test
    public void search() throws ScimException {
        final ListResponse<GenericScimResource> response = resourceTypesController.search(null);
        assertNotNull(response);
    }

    @Test
    public void getResourceTypeById() throws ScimException {
        final ScimResource user = resourceTypesController.get("User");
        assertEquals("User", user.getId());
    }
}