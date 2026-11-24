package com.salonhub.qa.api.endpoints;

import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.models.ServiceRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** {@code /api/salon-services}. A {@code null} session means an anonymous call. */
public final class ServiceApi {

    @Step("POST /api/salon-services")
    public Response create(ApiSession session, ServiceRequest body) {
        return ApiClient.as(session).body(body).post("/api/salon-services");
    }

    @Step("PUT /api/salon-services/{serviceId}")
    public Response update(ApiSession session, long serviceId, ServiceRequest body) {
        return ApiClient.as(session).body(body).put("/api/salon-services/{id}", serviceId);
    }

    /** Soft delete: the service becomes inactive. */
    @Step("DELETE /api/salon-services/{serviceId}")
    public Response disable(ApiSession session, long serviceId) {
        return ApiClient.as(session).delete("/api/salon-services/{id}", serviceId);
    }

    @Step("GET /api/salon-services/salon/{salonId}")
    public Response activeServicesOf(long salonId) {
        return ApiClient.anonymous().get("/api/salon-services/salon/{id}", salonId);
    }

    @Step("GET /api/salon-services/my-salon")
    public Response mySalonServices(ApiSession session) {
        return ApiClient.as(session).get("/api/salon-services/my-salon");
    }
}
