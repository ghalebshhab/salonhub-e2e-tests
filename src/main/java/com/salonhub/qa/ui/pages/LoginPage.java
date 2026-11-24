package com.salonhub.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.salonhub.qa.data.TestAccount;
import io.qameta.allure.Step;

/** {@code /login} */
public class LoginPage extends BasePage<LoginPage> {

    private final Locator emailInput;
    private final Locator passwordInput;
    private final Locator signInButton;
    private final Locator forgotPasswordLink;
    private final Locator registerLink;

    public LoginPage(Page page) {
        super(page);
        this.emailInput = page.locator("#login-email");
        this.passwordInput = page.locator("#login-password");
        this.signInButton = page.locator("form.auth-form button[type='submit']");
        this.forgotPasswordLink = page.locator("a[href='/forgot-password']");
        this.registerLink = page.locator(".auth-footer a[href='/register']");
    }

    @Override
    protected String path() {
        return "/login";
    }

    @Override
    public LoginPage waitUntilLoaded() {
        emailInput.waitFor();
        return this;
    }

    @Step("Type the email {email}")
    public LoginPage enterEmail(String email) {
        emailInput.fill(email);
        return this;
    }

    @Step("Type the password")
    public LoginPage enterPassword(String password) {
        passwordInput.fill(password);
        return this;
    }

    @Step("Click Sign in")
    public void submit() {
        signInButton.click();
    }

    /** Fills the form with valid data and signs in. Returns the home page. */
    @Step("Sign in with the form as {account}")
    public HomePage loginAs(TestAccount account) {
        enterEmail(account.email()).enterPassword(account.password()).submit();
        page.waitForURL(url -> !url.contains("/login"));
        return new HomePage(page).waitUntilLoaded();
    }

    /** Fills the form and signs in, but stays on this page (for invalid data). */
    @Step("Try to sign in as {email}")
    public LoginPage attemptLogin(String email, String password) {
        enterEmail(email).enterPassword(password).submit();
        return this;
    }

    public Locator emailInput() {
        return emailInput;
    }

    public Locator passwordInput() {
        return passwordInput;
    }

    public Locator signInButton() {
        return signInButton;
    }

    public Locator forgotPasswordLink() {
        return forgotPasswordLink;
    }

    public Locator registerLink() {
        return registerLink;
    }
}
