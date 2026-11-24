package com.salonhub.qa.api.endpoints;

import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.models.ClosureRequest;
import com.salonhub.qa.api.models.PaymentSettingsRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Owner endpoints: dashboard, subscription, payments, closures and team. */
public final class OwnerApi {

    public static final String DASHBOARD = "/api/owner/dashboard";
    public static final String DASHBOARD_SUMMARY = "/api/owner/dashboard/summary";
    public static final String SUBSCRIPTION = "/api/owner/subscription";
    public static final String PAYMENTS_SUMMARY = "/api/owner/payments/summary";
    public static final String PAYMENTS_OUTSTANDING = "/api/owner/payments/outstanding";

    @Step("GET {path}")
    public Response get(ApiSession session, String path) {
        return ApiClient.as(session).get(path);
    }

    @Step("GET /api/subscription-plans")
    public Response plans() {
        return ApiClient.anonymous().get("/api/subscription-plans");
    }

    @Step("GET /api/salon-employees/my-salon")
    public Response myTeam(ApiSession session) {
        return ApiClient.as(session).get("/api/salon-employees/my-salon");
    }

    @Step("GET /api/salons/{salonId}/closures")
    public Response closures(long salonId) {
        return ApiClient.anonymous().get("/api/salons/{id}/closures", salonId);
    }

    @Step("POST /api/salons/{salonId}/closures")
    public Response addClosure(ApiSession session, long salonId, ClosureRequest body) {
        return ApiClient.as(session).body(body).post("/api/salons/{id}/closures", salonId);
    }

    @Step("DELETE /api/salons/{salonId}/closures/{closureId}")
    public Response removeClosure(ApiSession session, long salonId, long closureId) {
        return ApiClient.as(session).delete("/api/salons/{id}/closures/{closureId}", salonId, closureId);
    }

    @Step("GET /api/salons/{salonId}/payment-settings")
    public Response paymentSettings(ApiSession session, long salonId) {
        return ApiClient.as(session).get("/api/salons/{id}/payment-settings", salonId);
    }

    @Step("PUT /api/salons/{salonId}/payment-settings")
    public Response updatePaymentSettings(ApiSession session, long salonId, PaymentSettingsRequest body) {
        return ApiClient.as(session).body(body).put("/api/salons/{id}/payment-settings", salonId);
    }

    @Step("GET /api/salons/{salonId}/payment-options")
    public Response paymentOptions(ApiSession session, long salonId) {
        return ApiClient.as(session).get("/api/salons/{id}/payment-options", salonId);
    }
}
