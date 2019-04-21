package com.bettercloud.scim2.server.controller;

import com.bettercloud.scim2.common.GenericScimResource;
import com.bettercloud.scim2.common.ScimResource;
import com.bettercloud.scim2.common.exceptions.ScimException;
import com.bettercloud.scim2.common.messages.ListResponse;
import com.bettercloud.scim2.server.controller.discovery.SchemasController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SchemasControllerTest {

    @Autowired
    private SchemasController schemasController;

    @Test
    public void search() throws ScimException {
        final ListResponse<GenericScimResource> response = schemasController.search(null);
        assertNotNull(response);
    }

    @Test
    public void getSchemaById() throws ScimException {
        final ScimResource response = schemasController.get("urn:ietf:params:scim:schemas:core:2.0:User");
        assertNotNull(response);
        assertEquals("urn:ietf:params:scim:schemas:core:2.0:User", response.getId());
    }
}