package com.salonhub.qa.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The backend wraps every body as {@code {success, message, data}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiResponse<T>(boolean success, String message, T data) {
}
