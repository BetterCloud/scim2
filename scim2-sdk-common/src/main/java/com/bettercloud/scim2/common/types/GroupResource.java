package com.bettercloud.scim2.common.types;

import java.util.List;

import com.bettercloud.scim2.common.BaseScimResource;
import com.bettercloud.scim2.common.annotations.Attribute;
import com.bettercloud.scim2.common.annotations.Schema;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * SCIM provides a resource type for "{@code Group}" resources.  The core schema
 * for "{@code Group}" is identified using the URI:
 * "{@code urn:ietf:params:scim:schemas:core:2.0:Group}".
 */

@Accessors(chain = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(id = "urn:ietf:params:scim:schemas:core:2.0:Group",
        name = "Group", description = "Group")
public class GroupResource extends BaseScimResource {

    @Attribute(description = "A human-readable name for the Group.",
            isRequired = true,
            isCaseExact = false,
            mutability = AttributeDefinition.Mutability.READ_WRITE,
            returned = AttributeDefinition.Returned.DEFAULT,
            uniqueness = AttributeDefinition.Uniqueness.NONE)
    private String displayName;

    @Attribute(description = "A list of members of the Group.",
            isRequired = false,
            mutability = AttributeDefinition.Mutability.READ_WRITE,
            returned = AttributeDefinition.Returned.DEFAULT,
            multiValueClass = Group.class)
    private List<Member> members;



    @Builder
    public GroupResource(String id, Meta meta, String externalId, String displayName, List<Member> members) {
        super(id);
        setMeta(meta);
        setExternalId(externalId);
        this.displayName = displayName;
        this.members = members;
    }


}
