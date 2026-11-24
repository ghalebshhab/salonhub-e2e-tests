package com.salonhub.qa.api.models;

import com.salonhub.qa.data.TestAccount;

public record LoginRequest(String email, String password) {

    public static LoginRequest of(TestAccount account) {
        return new LoginRequest(account.email(), account.password());
    }
}
