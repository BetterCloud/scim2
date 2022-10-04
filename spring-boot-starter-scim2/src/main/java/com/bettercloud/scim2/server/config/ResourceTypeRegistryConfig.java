package com.bettercloud.scim2.server.config;


import com.bettercloud.scim2.server.BaseUrlProvider;
import com.bettercloud.scim2.server.ResourceTypeDefinition;
import com.bettercloud.scim2.server.StaticBaseUrlProvider;
import com.bettercloud.scim2.server.annotation.ScimResource;
import com.bettercloud.scim2.server.resourcetypes.ResourceTypeRegistry;
import com.bettercloud.scim2.server.resourcetypes.SimpleResourceTypeRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashSet;
import java.util.Set;

/**
 * This configuration is designed to be a registry of all the existing SCIM resources in the system.
 * All beans annotated with the {@link ScimResource} and {@link RequestMapping} will be loaded as resource type definitions.
 */
@Configuration
@Slf4j
public class ResourceTypeRegistryConfig {

    @Bean
    @ConditionalOnMissingBean(BaseUrlProvider.class)
    public BaseUrlProvider baseUrlProvider(final Scim2Properties scim2Properties) {
        return new StaticBaseUrlProvider(scim2Properties.getBaseUrl());
    }


    @Bean
    @ConditionalOnMissingBean(ResourceTypeRegistry.class)
    public ResourceTypeRegistry resourceTypeRegistry(final Set<ResourceTypeDefinition> definitions) {
        return new SimpleResourceTypeRegistry(definitions);
    }

    @Bean
    public Set<ResourceTypeDefinition> getResourceDefinitions(final Scim2Properties scim2Properties) throws ClassNotFoundException {
        final TypeFilter scimResourceFilter = new AnnotationTypeFilter(ScimResource.class);
        final TypeFilter requestMappingFilter = new AnnotationTypeFilter(RequestMapping.class);
        final TypeFilter andFilter = (metadataReader, metadataReaderFactory) -> scimResourceFilter.match(metadataReader, metadataReaderFactory)
                                                                                && requestMappingFilter.match(metadataReader, metadataReaderFactory);

        final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(andFilter);

        final Set<ResourceTypeDefinition> resourceTypeDefinitions = new HashSet<>();
        for (final BeanDefinition beanDefinition : provider.findCandidateComponents(scim2Properties.getResourcesPackage())) {
            final Class<?> className = Class.forName(beanDefinition.getBeanClassName());
            resourceTypeDefinitions.add(ResourceTypeDefinition.fromScimResource(className.getAnnotation(ScimResource.class),
                                                                                className.getAnnotation(RequestMapping.class)));
        }

        return resourceTypeDefinitions;
    }
}
