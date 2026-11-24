package com.salonhub.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.salonhub.qa.ui.components.Navbar;
import com.salonhub.qa.ui.components.Toast;

/**
 * Parent of all page objects. A page object keeps the locators and actions of
 * one screen; tests only call its methods and never use raw selectors.
 */
public abstract class BasePage<T extends BasePage<T>> {

    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
    }

    /** The route of this page, for example {@code /login}. */
    protected abstract String path();

    /** Opens this page and waits until it is ready. */
    @SuppressWarnings("unchecked")
    public T open() {
        page.navigate(path());
        waitUntilLoaded();
        return (T) this;
    }

    /** Waits for a locator that shows the page is ready. */
    public abstract T waitUntilLoaded();

    public Locator heading() {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1));
    }

    public Navbar navbar() {
        return new Navbar(page);
    }

    public Toast toast() {
        return new Toast(page);
    }

    public com.salonhub.qa.ui.components.ConfirmDialog confirmDialog() {
        return new com.salonhub.qa.ui.components.ConfirmDialog(page);
    }

    /** The "not for your account" page that role guards show. */
    public Locator forbiddenMessage() {
        return page.getByText("This area is not for your account");
    }

    public String currentPath() {
        return java.net.URI.create(page.url()).getPath();
    }

    public Page page() {
        return page;
    }
}
