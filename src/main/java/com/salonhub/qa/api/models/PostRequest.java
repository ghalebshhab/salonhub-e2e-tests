package com.salonhub.qa.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Body of create / update community post. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostRequest(String content, String mediaUrl, Long salonId) {

    public static PostRequest text(String content) {
        return new PostRequest(content, null, null);
    }
}
