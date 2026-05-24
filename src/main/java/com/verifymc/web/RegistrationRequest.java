package com.verifymc.web;

import com.google.gson.JsonObject;
import java.util.function.BiFunction;

public record RegistrationRequest(
        String email,
        String code,
        String username,
        String normalizedUsername,
        String password,
        String captchaToken,
        String captchaAnswer,
        String language,
        String platform,
        JsonObject questionnaire
) {
    public static RegistrationRequest fromJson(JsonObject req, BiFunction<String, String, String> usernameNormalizer) {
        String email = req.has("email") ? req.get("email").getAsString().trim().toLowerCase() : "";
        String code = req.has("code") ? req.get("code").getAsString() : "";
        String username = req.has("username") ? req.get("username").getAsString() : "";
        String password = req.has("password") ? req.get("password").getAsString() : "";
        String captchaToken = req.has("captchaToken") ? req.get("captchaToken").getAsString() : "";
        String captchaAnswer = req.has("captchaAnswer") ? req.get("captchaAnswer").getAsString() : "";
        String language = req.has("language") ? req.get("language").getAsString() : "en";
        String platform = req.has("platform") ? req.get("platform").getAsString() : "java";
        JsonObject questionnaire = req.has("questionnaire") ? req.getAsJsonObject("questionnaire") : null;
        String normalizedUsername = usernameNormalizer.apply(username, platform);
        return new RegistrationRequest(email, code, username, normalizedUsername, password, captchaToken, captchaAnswer, language, platform, questionnaire);
    }
}
