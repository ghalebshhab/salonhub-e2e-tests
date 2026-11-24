package com.salonhub.qa.api.endpoints;

import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.models.HiringPostRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.util.Map;

/** {@code /api/hiring-posts}. */
public final class HiringApi {

    @Step("GET /api/hiring-posts")
    public Response openPosts(ApiSession session) {
        return ApiClient.as(session).get("/api/hiring-posts");
    }

    @Step("POST /api/hiring-posts")
    public Response create(ApiSession session, HiringPostRequest body) {
        return ApiClient.as(session).body(body).post("/api/hiring-posts");
    }

    @Step("GET /api/hiring-posts/owner")
    public Response ownerPosts(ApiSession session) {
        return ApiClient.as(session).get("/api/hiring-posts/owner");
    }

    @Step("GET /api/hiring-posts/owner/{postId}")
    public Response ownerPost(ApiSession session, long postId) {
        return ApiClient.as(session).get("/api/hiring-posts/owner/{id}", postId);
    }

    @Step("POST /api/hiring-posts/owner/{postId}/close")
    public Response close(ApiSession session, long postId) {
        return ApiClient.as(session).post("/api/hiring-posts/owner/{id}/close", postId);
    }

    @Step("POST /api/hiring-posts/{postId}/applications")
    public Response apply(ApiSession session, long postId, String note) {
        return ApiClient.as(session).body(Map.of("applicationNote", note))
                .post("/api/hiring-posts/{id}/applications", postId);
    }

    @Step("GET /api/hiring-posts/applications/me")
    public Response myApplications(ApiSession session) {
        return ApiClient.as(session).get("/api/hiring-posts/applications/me");
    }
}
