package com.bettercloud.scim2.common.types;

import java.util.List;

import com.bettercloud.scim2.common.BaseScimResource;
import com.bettercloud.scim2.common.annotations.Attribute;
import com.bettercloud.scim2.common.annotations.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(id = "urn:ietf:params:scim:schemas:extension:talend:2.0:Role",
        name = "User", description = "User Account")
public class RoleResource extends BaseScimResource {

    private static final long serialVersionUID = 1L;

    @Attribute(description = "A name for the Role.",
            isRequired = true,
            isCaseExact = false,
            mutability = AttributeDefinition.Mutability.READ_WRITE,
            returned = AttributeDefinition.Returned.DEFAULT,
            uniqueness = AttributeDefinition.Uniqueness.NONE)
    private String name;

    @Attribute(description = "A list of entitlements for the Role.",
            isRequired = false,
            returned = AttributeDefinition.Returned.DEFAULT,
            multiValueClass = Entitlement.class)
    private List<Entitlement> entitlements;
}
