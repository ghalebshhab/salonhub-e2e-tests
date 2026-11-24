package com.salonhub.qa.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Body of {@code POST /api/hiring-posts}. {@code expiresAt} is an ISO date-time string. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HiringPostRequest(String title, String description, List<String> requiredSkills,
                                Integer minimumAge, Integer maximumAge, String employeeLocation,
                                Integer minimumYearsOfExperience, Integer numOfPositions, String expiresAt) {

    public HiringPostRequest withAges(Integer min, Integer max) {
        return new HiringPostRequest(title, description, requiredSkills, min, max, employeeLocation,
                minimumYearsOfExperience, numOfPositions, expiresAt);
    }
}
