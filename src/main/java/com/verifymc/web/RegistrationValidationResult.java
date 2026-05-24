package com.verifymc.web;

import com.google.gson.JsonObject;

/**
 * Validation result for registration requests.
 * (Preserved from original.)
 */
public record RegistrationValidationResult(boolean passed, String messageKey, JsonObject responseFields) {

    public static RegistrationValidationResult pass() {
        return new RegistrationValidationResult(true, null, new JsonObject());
    }

    public static RegistrationValidationResult reject(String messageKey) {
        return reject(messageKey, new JsonObject());
    }

    public static RegistrationValidationResult reject(String messageKey, JsonObject responseFields) {
        return new RegistrationValidationResult(false, messageKey, responseFields == null ? new JsonObject() : responseFields);
    }
}
