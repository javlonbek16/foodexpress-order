package com.foodexpress.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


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
