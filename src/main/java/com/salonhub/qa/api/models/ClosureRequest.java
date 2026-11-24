package com.salonhub.qa.api.models;

/** Body of {@code POST /api/salons/{id}/closures}. Dates are ISO strings. */
public record ClosureRequest(String startDate, String endDate, String reason) {
}
