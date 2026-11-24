package com.salonhub.qa.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Body of accept / reject: an owner note and, for accept, the employee who serves. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingAction(String ownerNote, Long assignedEmployeeId) {
}
