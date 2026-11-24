package com.salonhub.qa.api.endpoints;

import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.models.AvailabilityRequest;
import com.salonhub.qa.api.models.BookingAction;
import com.salonhub.qa.api.models.BookingRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

/** {@code /api/bookings}. Business errors come back as HTTP 200 with {@code success=false}. */
public final class BookingApi {

    private static final String BY_ID = "/api/bookings/{id}";

    @Step("POST /api/bookings/availability")
    public Response availability(ApiSession session, AvailabilityRequest body) {
        return ApiClient.as(session).body(body).post("/api/bookings/availability");
    }

    @Step("POST /api/bookings")
    public Response create(ApiSession session, BookingRequest body) {
        return ApiClient.as(session).body(body).post("/api/bookings");
    }

    @Step("GET /api/bookings/my")
    public Response myBookings(ApiSession session) {
        return ApiClient.as(session).get("/api/bookings/my");
    }

    @Step("GET /api/bookings/my-salon")
    public Response mySalonBookings(ApiSession session) {
        return ApiClient.as(session).get("/api/bookings/my-salon");
    }

    @Step("GET /api/bookings/my-salon/pending")
    public Response mySalonPending(ApiSession session) {
        return ApiClient.as(session).get("/api/bookings/my-salon/pending");
    }

    @Step("GET /api/bookings/employee/my")
    public Response employeeBookings(ApiSession session) {
        return ApiClient.as(session).get("/api/bookings/employee/my");
    }

    @Step("PUT /api/bookings/{bookingId}/accept")
    public Response accept(ApiSession session, long bookingId, BookingAction action) {
        return ApiClient.as(session).body(action == null ? new BookingAction(null, null) : action)
                .put(BY_ID + "/accept", bookingId);
    }

    @Step("PUT /api/bookings/{bookingId}/reject")
    public Response reject(ApiSession session, long bookingId, BookingAction action) {
        return ApiClient.as(session).body(action).put(BY_ID + "/reject", bookingId);
    }

    @Step("PUT /api/bookings/{bookingId}/cancel")
    public Response cancel(ApiSession session, long bookingId) {
        return ApiClient.as(session).put(BY_ID + "/cancel", bookingId);
    }

    @Step("PUT /api/bookings/{bookingId}/confirm-attendance")
    public Response confirmAttendance(ApiSession session, long bookingId) {
        return ApiClient.as(session).put(BY_ID + "/confirm-attendance", bookingId);
    }

    @Step("GET /api/bookings/{bookingId}/assignable-employees")
    public Response assignableEmployees(ApiSession session, long bookingId) {
        return ApiClient.as(session).get(BY_ID + "/assignable-employees", bookingId);
    }

    @Step("GET /api/bookings/{bookingId}/payment")
    public Response payment(ApiSession session, long bookingId) {
        return ApiClient.as(session).get(BY_ID + "/payment", bookingId);
    }

    @Step("POST /api/bookings/{bookingId}/payment/cliq")
    public Response confirmCliq(ApiSession session, long bookingId, String reference) {
        RequestSpecification request = ApiClient.as(session);
        if (reference != null) {
            request = request.body(Map.of("reference", reference));
        }
        return request.post(BY_ID + "/payment/cliq", bookingId);
    }

    @Step("PUT /api/bookings/{bookingId}/payment/method")
    public Response changePaymentMethod(ApiSession session, long bookingId, String method) {
        return ApiClient.as(session).body(Map.of("method", method))
                .put(BY_ID + "/payment/method", bookingId);
    }

    @Step("GET /api/bookings/{bookingId}/tracking")
    public Response tracking(ApiSession session, long bookingId) {
        return ApiClient.as(session).get(BY_ID + "/tracking", bookingId);
    }

    @Step("PUT /api/bookings/{bookingId}/complete")
    public Response complete(ApiSession session, long bookingId) {
        return ApiClient.as(session).put(BY_ID + "/complete", bookingId);
    }
}
