package com.salonhub.qa.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Body of {@code POST /api/bookings/availability}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailabilityRequest(Long salonId, List<Long> serviceIds, String bookingDate, Long employeeId) {
}
