package com.salonhub.qa.ui.pages.owner;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.salonhub.qa.ui.pages.BasePage;
import io.qameta.allure.Step;

/** {@code /owner/services} — the owner's service list. */
public class ServicesPage extends BasePage<ServicesPage> {

    public ServicesPage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return "/owner/services";
    }

    @Override
    public ServicesPage waitUntilLoaded() {
        page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Manage Services")).waitFor();
        return this;
    }

    public Locator serviceCard(String name) {
        return page.locator(".service-card").filter(new Locator.FilterOptions().setHasText(name));
    }

    @Step("Click Add Service")
    public AddServicePage addService() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Add Service")).first().click();
        return new AddServicePage(page).waitUntilLoaded();
    }

    @Step("Disable the service {name} and confirm")
    public ServicesPage disable(String name) {
        serviceCard(name).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Disable")).click();
        confirmDialog().root().waitFor();
        confirmDialog().root().locator(".confirm-dialog-actions button").last().click();
        return this;
    }
}
