package com.salonhub.qa.api.auth;

import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.models.PostRequest;
import com.salonhub.qa.base.BaseApiTest;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.data.TestSalon;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.salonhub.qa.api.ApiAssertions.assertStatus;
import static com.salonhub.qa.api.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Authentication")
@Feature("Session security")
public class AuthSecurityApiTest extends BaseApiTest {

    @Test(groups = {"smoke", "api", "auth", "security"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("A signed-in write without the X-Requested-With header is blocked (CSRF guard, 403).")
    public void writeWithoutCsrfHeaderIsBlocked() {
        Response response = ApiClient.withoutCsrfHeader()
                .cookies(customer().cookies())
                .body(PostRequest.text(TestSalon.PREFIX + "csrf"))
                .post("/api/community/posts");

        if (response.statusCode() == 200 && response.jsonPath().getBoolean("success")) {
            communityApi.deletePost(customer(), response.jsonPath().getLong("data.id"));
        }
        assertStatus(response, 403);
        assertThat(response.jsonPath().getBoolean("success")).isFalse();
    }

    @Test(groups = {"api", "auth", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Refresh rotates the refresh cookie and gives an access cookie that works. "
            + "(The access token can be the same when both are made in the same second.)")
    public void refreshIssuesANewWorkingAccessCookie() {
        ApiSession session = authApi.signIn(TestAccounts.customer());

        Response refresh = authApi.refresh(session.cookies());

        assertSuccess(refresh);
        Cookie newAccess = refresh.detailedCookie(ApiSession.ACCESS_COOKIE);
        Cookie newRefresh = refresh.detailedCookie(ApiSession.REFRESH_COOKIE);
        assertThat(newAccess).as("new access cookie").isNotNull();
        assertThat(newRefresh).as("new refresh cookie").isNotNull();
        assertThat(newRefresh.getValue()).as("refresh token is rotated")
                .isNotEqualTo(session.cookies().getValue(ApiSession.REFRESH_COOKIE));
        assertSuccess(ApiClient.as(new Cookies(newAccess)).get("/api/notifications/unread-count"));
    }

    @Test(groups = {"api", "auth", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A refresh token works only one time: using the old one again is refused (token rotation).")
    public void oldRefreshTokenCannotBeReused() {
        ApiSession session = authApi.signIn(TestAccounts.customer());
        assertSuccess(authApi.refresh(session.cookies()));

        Response reuse = authApi.refresh(session.cookies());

        assertStatus(reuse, 401);
    }

    @Test(groups = {"api", "auth", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Refresh without a refresh cookie is refused (401).")
    public void refreshWithoutCookieIsRefused() {
        assertStatus(authApi.refresh(new Cookies()), 401);
    }

    @Test(groups = {"api", "auth", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A made-up access cookie is refused (401).")
    public void forgedAccessCookieIsRefused() {
        Cookies forged = new Cookies(new Cookie.Builder(ApiSession.ACCESS_COOKIE, "eyJhbGciOiJIUzI1NiJ9.e30.forged").build());

        assertStatus(ApiClient.as(forged).get("/api/salons/mine"), 401);
    }

    /**
     * The code-check endpoints allow 15 calls per 15 minutes per client. The
     * test uses a random fake client IP (X-Forwarded-For), so it does not block
     * your own local IP. This works only without a real proxy (local runs).
     */
    @Test(groups = {"api", "auth", "security", "rate-limit"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Too many verification-code tries get HTTP 429 with a Retry-After header.")
    public void tooManyCodeChecksAreRateLimited() {
        String fakeIp = "10.%d.%d.%d".formatted(ThreadLocalRandom.current().nextInt(256),
                ThreadLocalRandom.current().nextInt(256), ThreadLocalRandom.current().nextInt(1, 255));
        Response last = null;
        for (int i = 0; i < 20; i++) {
            last = ApiClient.anonymous()
                    .header("X-Forwarded-For", fakeIp)
                    .body(Map.of("email", "nobody.e2e@salonhub.test", "code", "000000"))
                    .post("/api/auth/email/verify");
            if (last.statusCode() == 429) {
                break;
            }
        }

        assertStatus(last, 429);
        assertThat(last.header("Retry-After")).as("Retry-After header").isNotBlank();
        assertThat(last.jsonPath().getString("message")).containsIgnoringCase("Too many attempts");
    }
}
