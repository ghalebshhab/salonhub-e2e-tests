package com.salonhub.qa.api.models;

import io.restassured.path.json.JsonPath;

/** Body of {@code PUT /api/salons/{id}/payment-settings}. Null fields keep JSON null. */
public record PaymentSettingsRequest(Boolean cashEnabled, Boolean cardPrepaidEnabled,
                                     Boolean cardPostpaidEnabled, Integer freeCancelHours,
                                     Integer lateCancelFeePercent, Integer noShowFeePercent,
                                     Integer noShowLimit, Boolean cliqEnabled,
                                     String cliqAlias, String cliqName) {

    /** The saved settings from a {@code GET payment-settings} response, as a request body. */
    public static PaymentSettingsRequest from(JsonPath json) {
        return new PaymentSettingsRequest(
                json.getBoolean("data.cashEnabled"),
                json.getBoolean("data.cardPrepaidEnabled"),
                json.getBoolean("data.cardPostpaidEnabled"),
                json.getInt("data.freeCancelHours"),
                json.getInt("data.lateCancelFeePercent"),
                json.getInt("data.noShowFeePercent"),
                json.get("data.noShowLimit"),
                Boolean.TRUE.equals(json.get("data.cliqEnabled")),
                json.getString("data.cliqAlias"),
                json.getString("data.cliqName"));
    }

    public PaymentSettingsRequest withCliq(Boolean enabled, String alias, String name) {
        return new PaymentSettingsRequest(cashEnabled, cardPrepaidEnabled, cardPostpaidEnabled,
                freeCancelHours, lateCancelFeePercent, noShowFeePercent, noShowLimit,
                enabled, alias, name);
    }
}
