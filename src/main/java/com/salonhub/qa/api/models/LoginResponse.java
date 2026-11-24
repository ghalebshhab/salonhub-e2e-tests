package com.salonhub.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** {@code data} of a successful {@code POST /api/auth/login}. The body holds no token. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginResponse(long id, String username, String userRole, String email) {
}
