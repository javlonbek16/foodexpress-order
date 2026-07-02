package com.foodexpress.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * test controller to test JWT tokens are now issued by the NestJS Auth service.
 */
@RestController
@RequestMapping("/auth")
public class TestAuthController {

    @GetMapping("/token")
    public Map<String, String> getTestToken() {
        return Map.of(
                "message", "Tokens are issued by the Auth service (NestJS). " +
                        "Sign in via the Auth service to get a JWT, then use it here as Bearer token.");
    }
}
