package com.bettercloud.scim2.server.controller;

import com.bettercloud.scim2.common.GenericScimResource;
import com.bettercloud.scim2.common.exceptions.ScimException;
import com.bettercloud.scim2.common.types.AuthenticationScheme;
import com.bettercloud.scim2.common.types.ServiceProviderConfigResource;
import com.bettercloud.scim2.common.utils.JsonUtils;
import com.bettercloud.scim2.server.controller.discovery.ServiceProviderConfigController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.net.URISyntaxException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ServiceProviderConfigControllerTest {

    private static final boolean AUTHENTICATION_SCHEMES_PRIMARY = true;
    private static final int MAX_OPERATIONS = 10000;
    private static final int MAX_PAYLOAD_SIZE = 1000;
    private static final int MAX_RESULTS = 100;
    private static final int AUTHENTICATION_SCHEMES = 1;
    private static final String AUTHENTICATION_SCHEMES_NAME = "sample name";
    private static final String AUTHENTICATION_SCHEMES_DESCRIPTION = "sample description";
    private static final String AUTHENTICATION_SCHEMES_SPEC = "http://localhost";
    private static final String AUTHENTICATION_SCHEMES_DOCUMENTATION = "http://localhost";
    private static final String AUTHENTICATION_SCHEMES_TYPE = "oauth2";


    @Autowired
    private ServiceProviderConfigController serviceProviderConfigController;

    @Test
    public void getServiceProviderConfig() throws Exception {
        final GenericScimResource genericScimResource = serviceProviderConfigController.get();
        assertNotNull(genericScimResource);

        final ObjectNode object = JsonUtils.valueToNode(genericScimResource);
        final ServiceProviderConfigResource serviceProviderConfigResource = JsonUtils.nodeToValue(object, ServiceProviderConfigResource.class);
        assertTrue(serviceProviderConfigResource.getPatch().isSupported());
        assertTrue(serviceProviderConfigResource.getBulk().isSupported());
        assertTrue(serviceProviderConfigResource.getBulk().isSupported());
        assertEquals(MAX_PAYLOAD_SIZE, serviceProviderConfigResource.getBulk().getMaxOperations());
        assertEquals(MAX_OPERATIONS, serviceProviderConfigResource.getBulk().getMaxPayloadSize());
        assertFalse(serviceProviderConfigResource.getFilter().isSupported());
        assertEquals(MAX_RESULTS, serviceProviderConfigResource.getFilter().getMaxResults());
        assertTrue(serviceProviderConfigResource.getChangePassword().isSupported());
        assertTrue(serviceProviderConfigResource.getSort().isSupported());
        assertTrue(serviceProviderConfigResource.getEtag().isSupported());

        assertEquals(AUTHENTICATION_SCHEMES, serviceProviderConfigResource.getAuthenticationSchemes().size());
        final AuthenticationScheme authenticationScheme = serviceProviderConfigResource.getAuthenticationSchemes().get(0);
        assertEquals(AUTHENTICATION_SCHEMES_NAME, authenticationScheme.getName());
        assertEquals(AUTHENTICATION_SCHEMES_DESCRIPTION, authenticationScheme.getDescription());
        assertEquals(new URI(AUTHENTICATION_SCHEMES_SPEC), authenticationScheme.getSpecUri());
        assertEquals(new URI(AUTHENTICATION_SCHEMES_DOCUMENTATION), authenticationScheme.getDocumentationUri());
        assertEquals(AUTHENTICATION_SCHEMES_TYPE, authenticationScheme.getType());
        assertEquals(AUTHENTICATION_SCHEMES_PRIMARY, authenticationScheme.isPrimary());
    }
}