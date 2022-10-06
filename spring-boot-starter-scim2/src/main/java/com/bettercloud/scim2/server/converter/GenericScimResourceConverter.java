package com.bettercloud.scim2.server.converter;

import com.bettercloud.scim2.common.GenericScimResource;
import com.bettercloud.scim2.common.ScimResource;
import com.bettercloud.scim2.common.exceptions.BadRequestException;
import com.bettercloud.scim2.common.types.Meta;
import com.bettercloud.scim2.server.BaseUrlProvider;
import com.bettercloud.scim2.server.ResourcePreparer;
import com.bettercloud.scim2.server.ResourceTypeDefinition;
import lombok.AllArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@AllArgsConstructor
public class GenericScimResourceConverter<RESOURCE extends ScimResource> {

    private final ResourceTypeDefinition resourceTypeDefinition;

    private final BaseUrlProvider baseUrlProvider;

    /**
     * Convert a resource to a GenericScimResource.
     *
     * @param resource The resource to be converted.
     *
     * @return A generic resource that has had all the correct types and locations set.
     *
     * @throws BadRequestException This will never be thrown.
     */
    public GenericScimResource convert(final RESOURCE resource) throws BadRequestException {
        return convert(null, null, resource, (r, u) -> {
        });
    }

    /**
     * Convert a resource after preparing it with the supplied BiConsumer.
     *
     * @param resource        The resource to be converted.
     * @param prepareResource A BiConsumer that will do prepare the resource.  Usually used to set the references of {@link
     *                        com.bettercloud.scim2.common.ComplexRef}
     *
     * @return A generic resource that has had all the correct types and locations set.
     *
     * @throws BadRequestException This will never be thrown.
     */
    public GenericScimResource convert(final RESOURCE resource, final BiConsumer<RESOURCE, URI> prepareResource) throws BadRequestException {
        return convert(null, null, resource, prepareResource);
    }

    /**
     * Convert a resource and the trim the results based on the attributes string.
     *
     * @param attributes Attributes filter. ex type,schemaBlob
     * @param excludedAttributes Exclude attributes filter. ex type,schemaBlob  This is not used if the attributes filter is defined.
     * @param resource   The resource to be converted.
     *
     * @return A generic resource that has had all the correct types and locations set.
     *
     * @throws BadRequestException This can be thrown if the attributes parameter is invalid.
     */
    public GenericScimResource convert(final String attributes, final String excludedAttributes, final RESOURCE resource) throws BadRequestException {
        return convert(attributes, excludedAttributes, resource, (r, u) -> {
        });
    }

    /**
     * Convert a resource after preparing it with the supplied BiConsumer and the trim the results based on the attributes string.
     *
     * @param attributes      Attributes filter. ex type,schemaBlob
     * @param excludedAttributes Exclude attributes filter. ex type,schemaBlob  This is not used if the attributes filter is defined.
     * @param resource        The resource to be converted.
     * @param prepareResource A BiConsumer that will prepare the resource.  Usually used to set the references of {@link
     *                        com.bettercloud.scim2.common.ComplexRef}
     *
     * @return A generic resource that has had all the correct types and locations set.
     *
     * @throws BadRequestException This can be thrown if the attributes parameter is invalid.
     */
    public GenericScimResource convert(final String attributes, final String excludedAttributes,
                                       final RESOURCE resource,
                                       final BiConsumer<RESOURCE, URI> prepareResource) throws BadRequestException {
        final ResourcePreparer<GenericScimResource> resourcePreparer = prepare(attributes, excludedAttributes);

        prepareResource.accept(resource, getBaseUri());

        final GenericScimResource genericScimResource = resource.asGenericScimResource();

        if (genericScimResource.getMeta() == null) {
            genericScimResource.setMeta(new Meta());
        }

        genericScimResource.getMeta().setLocation(getLocationUri());
        resourcePreparer.setResourceTypeAndLocation(genericScimResource);

        return resourcePreparer.trimRetrievedResource(genericScimResource);
    }

    /**
     * Convert a list of resources and the trim the results based on the attributes string.
     *
     * @param attributes   Attributes filter. ex type,schemaBlob
     * @param excludedAttributes Exclude attributes filter. ex type,schemaBlob  This is not used if the attributes filter is defined.
     * @param resourceList The resources to be converted.
     *
     * @return A generic list of resources that has had all the correct types and locations set.
     *
     * @throws BadRequestException This can be thrown if the attributes parameter is invalid.
     */
    public List<GenericScimResource> convert(final String attributes,
                                             final String excludedAttributes,
                                             final List<RESOURCE> resourceList) throws BadRequestException {
        return convert(attributes, excludedAttributes, resourceList, (r, u) -> {
        });
    }

    /**
     * Convert a list of resources after preparing them with the supplied BiConsumer and then trim the results based on the attributes string.
     *
     * @param attributes         Attributes filter. ex type,schemaBlob
     * @param excludedAttributes Excluded attributes filter. ex type,schemaBlob
     * @param resourceList       The resources to be converted.
     * @param prepareResource    A BiConsumer that will prepare the resource.  Usually used to set the references of {@link
     *                           com.bettercloud.scim2.common.ComplexRef}
     *
     * @return A list of generic resources that have had all the correct types and locations set.
     *
     * @throws BadRequestException This can be thrown if the attributes parameter is invalid.
     */
    public List<GenericScimResource> convert(final String attributes, final String excludedAttributes,
                                             final List<RESOURCE> resourceList,
                                             final BiConsumer<RESOURCE, URI> prepareResource) throws BadRequestException {
        final ResourcePreparer<GenericScimResource> resourcePreparer = prepare(attributes, excludedAttributes);

        return resourceList.stream().map(resource -> {
            prepareResource.accept(resource, getBaseUri());

            final GenericScimResource genericScimResource = resource.asGenericScimResource();
            resourcePreparer.setResourceTypeAndLocation(genericScimResource);

            return resourcePreparer.trimRetrievedResource(genericScimResource);
        }).collect(Collectors.toList());
    }

    private ResourcePreparer<GenericScimResource> prepare(final String attributes, final String excludedAttributes) throws BadRequestException {
        return new ResourcePreparer<>(resourceTypeDefinition, attributes, excludedAttributes, getLocationUri());
    }

    private static String stripLeadingSeparator(String contextPath) {
        if (contextPath.startsWith("/")) {
            return contextPath.substring(1);
        }
        return contextPath;
    }

    private URI getBaseUri() {
        return UriComponentsBuilder
                .fromHttpUrl(baseUrlProvider.getBaseUrl())
                .pathSegment(stripLeadingSeparator(getCurrentRequest().getContextPath()))
                .build().toUri();
    }


    private URI getLocationUri() {
        final HttpServletRequest request = getCurrentRequest();
        return UriComponentsBuilder
                .fromHttpUrl(baseUrlProvider.getBaseUrl())
                .pathSegment(stripLeadingSeparator(request.getContextPath()))
                .pathSegment(stripLeadingSeparator(request.getServletPath()))
                .build().toUri();
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
        return ((ServletRequestAttributes) attrs).getRequest();
    }
}
