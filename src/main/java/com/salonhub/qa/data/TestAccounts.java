package com.salonhub.qa.data;

import com.salonhub.qa.config.Config;

/**
 * The seeded local accounts. Sign-up sends a real email and rejects {@code .test}
 * domains, so tests never register users: they use these accounts.
 */
public final class TestAccounts {

    private TestAccounts() {
    }

    /** Customer (role USER). */
    public static TestAccount customer() {
        return account("customer", "USER");
    }

    /** Employee of salon 1 (role Employee). */
    public static TestAccount employee() {
        return account("employee", "Employee");
    }

    /** Owner of the test salon (role OWNER). */
    public static TestAccount owner() {
        return account("owner", "OWNER");
    }

    /** Employee of the test salon (role Employee). Made by {@code db/seed-e2e.sql}. */
    public static TestAccount salonEmployee() {
        return account("salon2employee", "Employee");
    }

    private static TestAccount account(String key, String role) {
        return new TestAccount(
                Config.get("account." + key + ".email"),
                Config.get("account." + key + ".password"),
                role);
    }
}
