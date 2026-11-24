package com.salonhub.qa.api.client;

import com.salonhub.qa.config.Config;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.http.Cookies;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Builds REST Assured requests for the SalonHub backend.
 * Every request sends {@code X-Requested-With: XMLHttpRequest}, because the
 * backend rejects cookie-authenticated writes without it (CSRF guard).
 */
public final class ApiClient {

    public static final String CSRF_HEADER = "X-Requested-With";
    public static final String CSRF_VALUE = "XMLHttpRequest";

    private static final RequestSpecification BASE_SPEC = new RequestSpecBuilder()
            .setBaseUri(Config.apiBaseUrl())
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .addHeader(CSRF_HEADER, CSRF_VALUE)
            .addFilter(new AllureRestAssured())
            .log(LogDetail.METHOD)
            .log(LogDetail.URI)
            .build();

    private ApiClient() {
    }

    /** A request with no session (anonymous user). */
    public static RequestSpecification anonymous() {
        return given().spec(BASE_SPEC);
    }

    /** A request signed in with the cookies of an {@link ApiSession}; {@code null} means anonymous. */
    public static RequestSpecification as(ApiSession session) {
        return session == null ? anonymous() : as(session.cookies());
    }

    public static RequestSpecification as(Cookies cookies) {
        return given().spec(BASE_SPEC).cookies(cookies);
    }

    /** A request without the CSRF header, for negative security tests. */
    public static RequestSpecification withoutCsrfHeader() {
        return given()
                .baseUri(Config.apiBaseUrl())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .filter(new AllureRestAssured());
    }
}
