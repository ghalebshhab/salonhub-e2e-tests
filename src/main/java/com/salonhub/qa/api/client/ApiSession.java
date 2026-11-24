package com.salonhub.qa.api.client;

import com.salonhub.qa.api.models.LoginResponse;
import com.salonhub.qa.data.TestAccount;
import io.restassured.http.Cookies;

/** A signed-in user: the session cookies plus the account data from the login response. */
public record ApiSession(TestAccount account, LoginResponse user, Cookies cookies) {

    public static final String ACCESS_COOKIE = "salonhub_access";
    public static final String REFRESH_COOKIE = "salonhub_refresh";
    public static final String HINT_COOKIE = "salonhub_auth";

    public long userId() {
        return user.id();
    }
}
