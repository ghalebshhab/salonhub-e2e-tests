package com.salonhub.qa.api.auth;

import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.client.SessionCache;
import com.salonhub.qa.api.endpoints.SalonApi;
import com.salonhub.qa.base.BaseApiTest;
import com.salonhub.qa.data.TestAccount;
import com.salonhub.qa.data.TestAccounts;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Epic("Authentication")
@Feature("Login API")
public class AuthApiTest extends BaseApiTest {

    @Test(groups = {"smoke", "api", "auth"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("A valid customer signs in: the body has the account, the session is in httpOnly cookies, not in the body.")
    public void loginWithValidCustomerReturnsAccountAndSessionCookies() {
        TestAccount customer = TestAccounts.customer();

        Response response = authApi.login(customer);

        response.then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/login-response.schema.json"))
                .body("success", equalTo(true))
                .body("data.email", equalTo(customer.email()))
                .body("data.userRole", equalTo("USER"));

        assertThat(response.asString()).as("the body must not hold a token").doesNotContainIgnoringCase("token");

        Cookie access = response.detailedCookie(ApiSession.ACCESS_COOKIE);
        Cookie refresh = response.detailedCookie(ApiSession.REFRESH_COOKIE);
        assertThat(access).as("access cookie").isNotNull();
        assertThat(access.isHttpOnly()).as("access cookie is httpOnly").isTrue();
        assertThat(access.getPath()).isEqualTo("/api");
        assertThat(refresh).as("refresh cookie").isNotNull();
        assertThat(refresh.isHttpOnly()).as("refresh cookie is httpOnly").isTrue();
        assertThat(refresh.getPath()).isEqualTo("/api/auth");
    }

    @DataProvider(name = "rolesAndAccounts")
    public Object[][] rolesAndAccounts() {
        return new Object[][]{
                {TestAccounts.customer(), "USER"},
                {TestAccounts.employee(), "Employee"},
                {TestAccounts.owner(), "OWNER"},
        };
    }

    @Test(groups = {"api", "auth"}, dataProvider = "rolesAndAccounts")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Each seeded account signs in and gets its own role.")
    public void loginReturnsTheRoleOfEachAccount(TestAccount account, String expectedRole) {
        ApiSession session = SessionCache.get(account);

        assertThat(session.user().userRole()).isEqualTo(expectedRole);
        assertThat(session.user().email()).isEqualTo(account.email());
        assertThat(session.userId()).isPositive();
    }

    /** One invalid login. {@code toString} gives only the scenario, so no password reaches the report. */
    record LoginAttempt(String scenario, String email, String password) {
        @Override
        public String toString() {
            return scenario;
        }
    }

    @DataProvider(name = "invalidCredentials")
    public Object[][] invalidCredentials() {
        TestAccount customer = TestAccounts.customer();
        return new Object[][]{
                {new LoginAttempt("wrong password", customer.email(), "Wrong-password-123!")},
                {new LoginAttempt("unknown email", "nobody.e2e@salonhub.test", "Some-password-123!")},
                {new LoginAttempt("empty email", "", customer.password())},
                {new LoginAttempt("empty password", customer.email(), "")},
                {new LoginAttempt("both empty", "", "")},
        };
    }

    @Test(groups = {"api", "auth", "negative"}, dataProvider = "invalidCredentials")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Invalid credentials never sign in and never set a session cookie.")
    public void loginWithInvalidCredentialsIsRejected(LoginAttempt attempt) {
        String scenario = attempt.scenario();
        Response response = authApi.login(attempt.email(), attempt.password());

        assertThat(response.statusCode()).as(scenario + ": HTTP status").isIn(200, 400, 401);
        assertThat(response.jsonPath().getBoolean("success")).as(scenario + ": success").isFalse();
        assertThat(response.jsonPath().getString("message")).as(scenario + ": message").isNotBlank();
        assertThat(response.detailedCookie(ApiSession.ACCESS_COOKIE)).as(scenario + ": no access cookie").isNull();
    }

    @Test(groups = {"api", "auth"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Logout clears the session cookies and the old access cookie no longer works.")
    public void logoutEndsTheSession() {
        ApiSession session = authApi.signIn(TestAccounts.customer());
        salonApi.getMySalons(session).then().statusCode(200);

        Response logout = authApi.logout(session.cookies());

        logout.then().statusCode(200).body("success", equalTo(true));
        Cookie cleared = logout.detailedCookie(ApiSession.ACCESS_COOKIE);
        assertThat(cleared).isNotNull();
        assertThat(cleared.getValue()).isEmpty();
        assertThat(salonApi.getMySalons(session).statusCode())
                .as("the old access cookie is revoked after logout")
                .isEqualTo(401);
    }

    @Test(groups = {"smoke", "api", "auth", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A protected endpoint refuses an anonymous call with 401.")
    public void protectedEndpointWithoutSessionReturns401() {
        ApiClient.anonymous()
                .get(SalonApi.MINE)
                .then()
                .statusCode(401)
                .body("success", equalTo(false));
    }

    @Test(groups = {"api", "auth", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A customer cannot call an admin-only endpoint (403).")
    public void customerCannotCallAdminEndpoint() {
        ApiSession customer = SessionCache.get(TestAccounts.customer());

        salonApi.getAllSalons(customer).then().statusCode(403);
    }
}
