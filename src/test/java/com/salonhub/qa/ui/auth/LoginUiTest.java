package com.salonhub.qa.ui.auth;

import com.salonhub.qa.base.BaseUiTest;
import com.salonhub.qa.data.TestAccount;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.ui.pages.HomePage;
import com.salonhub.qa.ui.pages.LoginPage;
import com.salonhub.qa.ui.pages.MyBookingsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Authentication")
@Feature("Login page")
public class LoginUiTest extends BaseUiTest {

    @Test(groups = {"smoke", "ui", "auth"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("A customer signs in with the form and lands on the home page as a signed-in user.")
    public void customerCanSignInWithTheForm() {
        TestAccount customer = TestAccounts.customer();

        HomePage home = new LoginPage(page).open().loginAs(customer);

        assertThat(home.currentPath()).isEqualTo("/");
        assertThat(home.toast().success()).isVisible();
        assertThat(home.navbar().loginLink()).isHidden();
    }

    @Test(groups = {"ui", "auth", "negative"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A wrong password shows an error and keeps the user on the login page.")
    public void wrongPasswordShowsAnError() {
        LoginPage login = new LoginPage(page).open()
                .attemptLogin(TestAccounts.customer().email(), "Wrong-password-123!");

        assertThat(login.toast().error()).isVisible();
        assertThat(login.currentPath()).isEqualTo("/login");
        assertThat(login.passwordInput()).isVisible();
    }

    @Test(groups = {"ui", "auth", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The browser blocks an empty form: both fields are required.")
    public void emptyFormIsNotSent() {
        LoginPage login = new LoginPage(page).open();

        login.submit();

        assertThat(login.emailInput()).hasAttribute("required", "");
        assertThat(login.passwordInput()).hasAttribute("required", "");
        assertThat(login.currentPath()).isEqualTo("/login");
    }

    @Test(groups = {"smoke", "ui", "auth", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("An anonymous user who opens a protected page is sent to the login page.")
    public void protectedPageRedirectsAnonymousUserToLogin() {
        page.navigate("/my-bookings");

        new LoginPage(page).waitUntilLoaded();
        assertThat(page.url()).contains("/login");
    }

    @Test(groups = {"ui", "auth"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The API sign-in shortcut gives the browser a session: a protected page opens.")
    public void apiSessionOpensProtectedPage() {
        signInAs(TestAccounts.customer());

        MyBookingsPage bookings = new MyBookingsPage(page).open();

        assertThat(bookings.currentPath()).isEqualTo("/my-bookings");
        assertThat(bookings.heading().first()).isVisible();
    }

    @Test(groups = {"ui", "auth"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Log out from the account menu ends the session: the login link comes back and "
            + "a protected page sends the user to the login page. Uses a form sign-in, so the "
            + "cached API session of other tests stays valid.")
    public void customerCanLogOut() {
        HomePage home = new LoginPage(page).open().loginAs(TestAccounts.customer());
        assertThat(home.navbar().loginLink()).isHidden();

        home.navbar().logout();

        assertThat(home.navbar().loginLink()).isVisible();
        page.navigate("/my-bookings");
        new LoginPage(page).waitUntilLoaded();
        assertThat(page.url()).contains("/login");
    }

    @Test(groups = {"ui", "auth", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A customer who opens the owner dashboard sees the 'not for your account' message.")
    public void customerCannotOpenOwnerDashboard() {
        signInAs(TestAccounts.customer());

        page.navigate("/owner/dashboard");

        assertThat(new HomePage(page).forbiddenMessage()).isVisible();
    }
}
