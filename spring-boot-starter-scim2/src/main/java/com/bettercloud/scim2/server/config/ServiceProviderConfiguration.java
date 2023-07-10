package com.bettercloud.scim2.server.config;

import com.unboundid.scim2.common.types.ServiceProviderConfigResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
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
 */
@Configuration
public class ServiceProviderConfiguration {

    @Bean
    public ServiceProviderConfigResource getServiceProviderConfiguration(final Scim2Properties scim2Properties) {
        return scim2Properties.getServiceProviderConfig().getServiceProviderConfiguration();
    }
}