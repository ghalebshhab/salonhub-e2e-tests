package com.salonhub.qa.ui.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

/** The top header shared by all pages. */
public class Navbar {

    private final Page page;

    public Navbar(Page page) {
        this.page = page;
    }

    /** The "Log in" link. It shows only when nobody is signed in. */
    public Locator loginLink() {
        return page.locator("a.header-login-link");
    }

    /** The account menu button. It shows only when a user is signed in. */
    public Locator accountMenuButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Open account menu"));
    }

    public void clickLogin() {
        loginLink().click();
    }

    @Step("Log out with the account menu")
    public void logout() {
        accountMenuButton().click();
        page.locator(".header-profile-menu")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Log out"))
                .click();
    }
}
