package com.salonhub.qa.api.endpoints;

import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.models.ReviewRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** {@code /api/reviews}. */
public final class ReviewApi {

    @Step("POST /api/reviews")
    public Response create(ApiSession session, ReviewRequest body) {
        return ApiClient.as(session).body(body).post("/api/reviews");
    }

    @Step("GET /api/reviews/salon/{salonId}")
    public Response ofSalon(long salonId) {
        return ApiClient.anonymous().get("/api/reviews/salon/{id}", salonId);
    }

    @Step("GET /api/reviews/salon/{salonId}/summary")
    public Response summaryOf(long salonId) {
        return ApiClient.anonymous().get("/api/reviews/salon/{id}/summary", salonId);
    }

    @Step("GET /api/reviews/my")
    public Response myReviews(ApiSession session) {
        return ApiClient.as(session).get("/api/reviews/my");
    }

    @Step("DELETE /api/reviews/{reviewId}")
    public Response delete(ApiSession session, long reviewId) {
        return ApiClient.as(session).delete("/api/reviews/{id}", reviewId);
    }
}
