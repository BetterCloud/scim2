package com.bettercloud.scim2.common;

import com.bettercloud.scim2.common.annotations.Attribute;
import com.bettercloud.scim2.common.types.AttributeDefinition;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.net.URI;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public abstract class ComplexRef implements Serializable {

    private static final long serialVersionUID = -7193020979748942398L;

    /**
     * This should return the spring mvc context mapping for this resource.
     * It is used to build the correct $ref when a ComplexRef is used as a
     * attribute of a resource.
     * @return context mapping for the complex reference
     */
    public abstract String getContextMapping();

    @Attribute(description = "The id of the SCIM resource.",
               isCaseExact = false,
               mutability = AttributeDefinition.Mutability.READ_WRITE,
               returned = AttributeDefinition.Returned.DEFAULT)
    private String value;

    @Attribute(description = "The URI of the SCIM resource.",
               isCaseExact = false,
               mutability = AttributeDefinition.Mutability.READ_WRITE,
               returned = AttributeDefinition.Returned.DEFAULT,
               referenceTypes = {"BCSchema"})
    @JsonProperty("$ref")
    private URI ref;
}
