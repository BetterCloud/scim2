package com.bettercloud.scim2.common.types;

import java.net.URI;

import com.bettercloud.scim2.common.annotations.Attribute;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * A member of a Group resource.
 */

@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode
public class Member {
    @Attribute(description = "The identifier of a group member.",
            isRequired = true,
            isCaseExact = false,
            mutability = AttributeDefinition.Mutability.IMMUTABLE,
            returned = AttributeDefinition.Returned.DEFAULT,
            uniqueness = AttributeDefinition.Uniqueness.NONE)
    private String value;

    @Attribute(description = "The URI of the member resource.",
            isRequired = true,
            referenceTypes = { "User", "Group" },
            mutability = AttributeDefinition.Mutability.IMMUTABLE,
            returned = AttributeDefinition.Returned.DEFAULT,
            uniqueness = AttributeDefinition.Uniqueness.NONE)
    @JsonProperty("$ref")
    private URI ref;

    @Attribute(description = "A human readable name, primarily used for " +
            "display purposes.",
            isRequired = false,
            isCaseExact = false,
            mutability = AttributeDefinition.Mutability.IMMUTABLE,
            returned = AttributeDefinition.Returned.DEFAULT,
            uniqueness = AttributeDefinition.Uniqueness.NONE)
    private String display;
}
