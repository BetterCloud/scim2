package com.bettercloud.scim2.server.controller;

import com.bettercloud.scim2.common.types.UserResource;
import com.bettercloud.scim2.server.annotation.ScimResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ScimResource(description = "Access User Resources", name = "User", schema = UserResource.class)
@RestController
@RequestMapping("/user")
public class MockUserController {
}