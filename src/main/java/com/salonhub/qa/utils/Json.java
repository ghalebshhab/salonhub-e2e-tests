package com.salonhub.qa.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;

/** Jackson helper that reads generic response bodies such as {@code ApiResponse<LoginResponse>}. */
public final class Json {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Json() {
    }

    public static <T> T read(Response response, TypeReference<T> type) {
        try {
            return MAPPER.readValue(response.asString(), type);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read response body as " + type.getType()
                    + ": " + response.asString(), e);
        }
    }
}
