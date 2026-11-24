package com.salonhub.qa.api.endpoints;

import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

/** {@code /api/salon-employees} — the owner's view of the team. */
public final class EmployeeApi {

    @Step("GET /api/salon-employees/{employeeId}/services")
    public Response services(ApiSession owner, long employeeId) {
        return ApiClient.as(owner).get("/api/salon-employees/{id}/services", employeeId);
    }

    /** Replaces the whole list of services the employee can do. */
    @Step("PUT /api/salon-employees/{employeeId}/services")
    public Response setServices(ApiSession owner, long employeeId, List<Long> serviceIds) {
        return ApiClient.as(owner).body(Map.of("serviceIds", serviceIds))
                .put("/api/salon-employees/{id}/services", employeeId);
    }
}
