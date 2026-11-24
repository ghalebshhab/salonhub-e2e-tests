package com.salonhub.qa.api.models;

/** Body of {@code POST /api/reviews}. */
public record ReviewRequest(Long bookingId, Integer rating, String comment) {
}
