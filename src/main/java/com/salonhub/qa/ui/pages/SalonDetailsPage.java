package com.salonhub.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

/** {@code /salons/{salonId}} — the public salon page. */
public class SalonDetailsPage extends BasePage<SalonDetailsPage> {

    private final long salonId;

    public SalonDetailsPage(Page page, long salonId) {
        super(page);
        this.salonId = salonId;
    }

    @Override
    protected String path() {
        return "/salons/" + salonId;
    }

    @Override
    public SalonDetailsPage waitUntilLoaded() {
        heading().first().waitFor();
        return this;
    }

    /** "Book appointment" for signed-in users. */
    public Locator bookLink() {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Book appointment")).first();
    }

    /** "Sign in to book" for anonymous users. */
    public Locator signInToBookLink() {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in to book")).first();
    }

    /** "Edit salon" for the salon's owner. */
    public Locator editSalonLink() {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Edit salon")).first();
    }

    /** One row of the service menu, found by the service name. */
    public Locator service(String name) {
        return page.locator(".menu-item").filter(new Locator.FilterOptions().setHasText(name));
    }

    @Step("Click Book appointment")
    public BookingWizardPage startBooking() {
        bookLink().click();
        return new BookingWizardPage(page, salonId).waitUntilLoaded();
    }

    /** A review card in the "What guests are saying" section, found by its comment. */
    public Locator review(String comment) {
        return page.locator("#reviews .rv-card").filter(new Locator.FilterOptions().setHasText(comment));
    }
}
