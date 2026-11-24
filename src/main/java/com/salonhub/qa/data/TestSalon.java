package com.salonhub.qa.data;

import com.salonhub.qa.config.Config;

/**
 * The salon that the tests own and may change: "Omar Test Salon" (owner
 * {@link TestAccounts#owner()}, employee {@link TestAccounts#salonEmployee()}).
 * Tests never change other salons. {@link #otherSalonId()} is used only for
 * read-only and "not yours" checks.
 */
public final class TestSalon {

    /** Every name, title and note the tests create starts with this, so test data is easy to find. */
    public static final String PREFIX = "E2E ";

    private TestSalon() {
    }

    public static long id() {
        return Long.parseLong(Config.get("data.salon.id"));
    }

    public static long employeeId() {
        return Long.parseLong(Config.get("data.salon.employeeId"));
    }

    /** A salon the test accounts do not own. */
    public static long otherSalonId() {
        return Long.parseLong(Config.get("data.otherSalon.id"));
    }
}
