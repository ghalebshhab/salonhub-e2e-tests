package com.salonhub.qa.api.endpoints;

import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** {@code /api/notifications}. */
public final class NotificationApi {

    @Step("GET /api/notifications")
    public Response list(ApiSession session) {
        return ApiClient.as(session).get("/api/notifications");
    }

    @Step("GET /api/notifications/unread-count")
    public Response unreadCount(ApiSession session) {
        return ApiClient.as(session).get("/api/notifications/unread-count");
    }

    @Step("PUT /api/notifications/read-all")
    public Response readAll(ApiSession session) {
        return ApiClient.as(session).put("/api/notifications/read-all");
    }
}
