package com.salonhub.qa.api;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks for the backend's {@code {success, message, data}} body.
 * The backend sends business errors as HTTP 200 with {@code success=false},
 * and security errors as 401 / 403.
 */
public final class ApiAssertions {

    private ApiAssertions() {
    }

    /** HTTP 200 and {@code success=true}. Returns the body for more checks. */
    public static JsonPath assertSuccess(Response response) {
        assertThat(response.statusCode()).as("HTTP status. Body: %s", response.asString()).isEqualTo(200);
        JsonPath json = response.jsonPath();
        assertThat(json.getBoolean("success")).as("success. Message: %s", json.getString("message")).isTrue();
        return json;
    }

    /** HTTP 200, {@code success=false} and a message that contains {@code expectedMessagePart}. */
    public static JsonPath assertBusinessError(Response response, String expectedMessagePart) {
        assertThat(response.statusCode()).as("HTTP status. Body: %s", response.asString()).isEqualTo(200);
        JsonPath json = response.jsonPath();
        assertThat(json.getBoolean("success")).as("success. Body: %s", response.asString()).isFalse();
        assertThat(json.getString("message")).as("error message").containsIgnoringCase(expectedMessagePart);
        return json;
    }

    /** The call is refused: {@code success=false}, with HTTP 200, 401 or 403. */
    public static void assertRefused(Response response) {
        assertThat(response.statusCode()).as("HTTP status. Body: %s", response.asString()).isIn(200, 401, 403);
        assertThat(response.jsonPath().getBoolean("success")).as("success. Body: %s", response.asString()).isFalse();
    }

    public static void assertStatus(Response response, int status) {
        assertThat(response.statusCode()).as("HTTP status. Body: %s", response.asString()).isEqualTo(status);
    }
}
