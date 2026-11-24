package com.salonhub.qa.api.endpoints;

import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** {@code /api/salons} endpoints. */
public final class SalonApi {

    public static final String SALONS = "/api/salons";
    public static final String ACTIVE = "/api/salons/active";
    public static final String MINE = "/api/salons/mine";
    public static final String BY_ID = "/api/salons/{salonId}";

    @Step("GET /api/salons/active")
    public Response getActiveSalons() {
        return ApiClient.anonymous().get(ACTIVE);
    }

    @Step("GET /api/salons/{salonId}")
    public Response getSalon(long salonId) {
        return ApiClient.anonymous().pathParam("salonId", salonId).get(BY_ID);
    }

    @Step("GET /api/salons (admin list) as anonymous")
    public Response getAllSalonsAnonymous() {
        return ApiClient.anonymous().get(SALONS);
    }

    @Step("GET /api/salons (admin list) as {session.account}")
    public Response getAllSalons(ApiSession session) {
        return ApiClient.as(session).get(SALONS);
    }

    @Step("GET /api/salons/mine as {session.account}")
    public Response getMySalons(ApiSession session) {
        return ApiClient.as(session).get(MINE);
    }
}
