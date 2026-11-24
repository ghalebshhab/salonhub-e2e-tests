package com.salonhub.qa.data;

/** One seeded SalonHub user. The password never goes into a report or log. */
public record TestAccount(String email, String password, String role) {

    @Override
    public String toString() {
        return role + " <" + email + ">";
    }
}
