package com.salonhub.qa.api.endpoints;

import com.fasterxml.jackson.core.type.TypeReference;
import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.models.ApiResponse;
import com.salonhub.qa.api.models.LoginRequest;
import com.salonhub.qa.api.models.LoginResponse;
import com.salonhub.qa.data.TestAccount;
import com.salonhub.qa.utils.Json;
import io.qameta.allure.Step;
import io.restassured.http.Cookies;
import io.restassured.response.Response;

/** {@code /api/auth} endpoints. */
public final class AuthApi {

    public static final String LOGIN = "/api/auth/login";
    public static final String LOGOUT = "/api/auth/logout";
    public static final String REFRESH = "/api/auth/refresh";

    @Step("POST /api/auth/login as {email}")
    public Response login(String email, String password) {
        return ApiClient.anonymous()
                .body(new LoginRequest(email, password))
                .post(LOGIN);
    }

    public Response login(TestAccount account) {
        return login(account.email(), account.password());
    }

    /** Signs in and returns the session. Fails fast when the login does not work. */
    @Step("Sign in through the API as {account}")
    public ApiSession signIn(TestAccount account) {
        Response response = login(account);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Login failed for " + account + ": HTTP " + response.statusCode());
        }
        ApiResponse<LoginResponse> body = Json.read(response, new TypeReference<>() {
        });
        if (!body.success() || body.data() == null) {
            throw new IllegalStateException("Login failed for " + account + ": " + body.message());
        }
        return new ApiSession(account, body.data(), response.detailedCookies());
    }

    @Step("POST /api/auth/logout")
    public Response logout(Cookies cookies) {
        return ApiClient.as(cookies).post(LOGOUT);
    }

    @Step("POST /api/auth/refresh")
    public Response refresh(Cookies cookies) {
        return ApiClient.as(cookies).post(REFRESH);
    }
}
