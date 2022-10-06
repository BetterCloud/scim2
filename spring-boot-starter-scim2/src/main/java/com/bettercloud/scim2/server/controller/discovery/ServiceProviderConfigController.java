package com.bettercloud.scim2.server.controller.discovery;

import com.bettercloud.scim2.common.GenericScimResource;
import com.bettercloud.scim2.common.exceptions.ScimException;
import com.bettercloud.scim2.common.types.ServiceProviderConfigResource;
import com.bettercloud.scim2.common.utils.ApiConstants;
import com.bettercloud.scim2.server.BaseUrlProvider;
import com.bettercloud.scim2.server.annotation.ScimResource;
import com.bettercloud.scim2.server.config.Scim2Properties;
import com.bettercloud.scim2.server.controller.BaseResourceController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The ServiceProviderConfig is populated through application properties.  Please review the readme for further details.
 *
 * SCIM provides a schema for representing the service provider's
 * configuration, identified using the following schema URI:
 * "urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig".
 * <p>
 * The service provider configuration resource enables a service
 * provider to discover SCIM specification features in a standardized
 * form as well as provide additional implementation details to clients.
 * All attributes have a mutability of "readOnly".  Unlike other core
 * resources, the "id" attribute is not required for the service
 * provider configuration resource.
 * <p>
 * RFC 7643
 * SCIM Core Schema
 * September 2015
 * https://tools.ietf.org/html/rfc7643#page-26
 */
@ScimResource(description = "SCIM 2.0 Service Provider Config",
              name = "ServiceProviderConfig",
              schema = ServiceProviderConfigResource.class,
              discoverable = false)
@RestController
@RequestMapping(value = ApiConstants.SERVICE_PROVIDER_CONFIG_ENDPOINT)
public class ServiceProviderConfigController extends BaseResourceController<ServiceProviderConfigResource> {

    private ServiceProviderConfigResource serviceProviderConfigResource;

    @Autowired
    public ServiceProviderConfigController(final BaseUrlProvider baseUrlProvider,
                                           final ServiceProviderConfigResource serviceProviderConfigResource) {
        super(baseUrlProvider);
        this.serviceProviderConfigResource = serviceProviderConfigResource;
    }

    /**
     * Service request to retrieve the Service Provider Config.
     *
     * @return The Service Provider Config.
     *
     * @throws ScimException if an error occurs.
     */
    @GetMapping
    public GenericScimResource get() throws ScimException {
        return genericScimResourceConverter.convert(serviceProviderConfigResource);
    }
}
