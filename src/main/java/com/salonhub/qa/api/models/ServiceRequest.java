package com.salonhub.qa.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/** Body of {@code POST /api/salon-services} and {@code PUT /api/salon-services/{id}}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceRequest(String name, String description, BigDecimal price,
                             Integer durationMinutes, Boolean isActive) {

    public static ServiceRequest of(String name, double price, int durationMinutes) {
        return new ServiceRequest(name, "Made by the E2E tests", BigDecimal.valueOf(price), durationMinutes, null);
    }

    public ServiceRequest active(boolean active) {
        return new ServiceRequest(name, description, price, durationMinutes, active);
    }
}
