package com.salonhub.qa.api.flows;

import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.endpoints.OwnerApi;
import com.salonhub.qa.api.models.PaymentSettingsRequest;
import com.salonhub.qa.data.TestSalon;
import io.qameta.allure.Step;

import static com.salonhub.qa.api.ApiAssertions.assertSuccess;

/**
 * Turns CliQ on at the test salon for the classes that need it. Test classes
 * run in parallel, so the first {@link #acquire} saves the settings and turns
 * CliQ on, and the last {@link #release} puts the saved settings back.
 */
public final class CliqSetup {

    public static final String ALIAS = "E2E-SALON";
    public static final String NAME = "E2E Test Salon";

    private static final OwnerApi OWNER_API = new OwnerApi();
    private static int users;
    private static PaymentSettingsRequest original;

    private CliqSetup() {
    }

    /** The salon settings before CliQ was turned on. */
    public static synchronized PaymentSettingsRequest original() {
        return original;
    }

    @Step("Turn CliQ on at the test salon")
    public static synchronized void acquire(ApiSession owner) {
        if (users == 0) {
            original = PaymentSettingsRequest.from(
                    assertSuccess(OWNER_API.paymentSettings(owner, TestSalon.id())));
        }
        // Set it every time: a validation test may have sent a refused update in between.
        assertSuccess(OWNER_API.updatePaymentSettings(owner, TestSalon.id(),
                original.withCliq(true, ALIAS, NAME)));
        users++;
    }

    @Step("Put the test salon payment settings back")
    public static synchronized void release(ApiSession owner) {
        if (users == 0) {
            return;
        }
        users--;
        if (users == 0 && original != null) {
            OWNER_API.updatePaymentSettings(owner, TestSalon.id(), original);
            original = null;
        }
    }
}
