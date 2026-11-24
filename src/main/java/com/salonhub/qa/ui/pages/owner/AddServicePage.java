package com.salonhub.qa.ui.pages.owner;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.salonhub.qa.ui.pages.BasePage;
import io.qameta.allure.Step;

/** {@code /owner/create-service} — the new-service form. */
public class AddServicePage extends BasePage<AddServicePage> {

    public AddServicePage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return "/owner/create-service";
    }

    @Override
    public AddServicePage waitUntilLoaded() {
        nameInput().waitFor();
        return this;
    }

    public Locator nameInput() {
        return page.locator("input[name='name']");
    }

    public Locator descriptionInput() {
        return page.locator("textarea[name='description']");
    }

    public Locator priceInput() {
        return page.locator("input[name='price']");
    }

    public Locator durationInput() {
        return page.locator("input[name='durationMinutes']");
    }

    public Locator createButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create Service"));
    }

    @Step("Fill the service form: {name}, {price} JD, {minutes} min")
    public AddServicePage fill(String name, String description, String price, String minutes) {
        nameInput().fill(name);
        descriptionInput().fill(description);
        priceInput().fill(price);
        durationInput().fill(minutes);
        return this;
    }

    @Step("Click Create Service")
    public void submit() {
        createButton().click();
    }

    /** Submits valid data and waits for the service list. */
    public ServicesPage create(String name, String description, String price, String minutes) {
        fill(name, description, price, minutes).submit();
        page.waitForURL("**/owner/services");
        return new ServicesPage(page).waitUntilLoaded();
    }
}
