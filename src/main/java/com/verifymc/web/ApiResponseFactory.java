package com.verifymc.web;

import com.google.gson.JsonObject;

/**
 * Factory for building standardised JSON API responses.
 */
public final class ApiResponseFactory {
    private ApiResponseFactory() {}

    public static JsonObject success(String message) {
        return create(true, message);
    }

    public static JsonObject failure(String message) {
        return create(false, message);
    }

    public static JsonObject create(boolean success, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("success", success);
        response.addProperty("message", message);
        return response;
    }
}
