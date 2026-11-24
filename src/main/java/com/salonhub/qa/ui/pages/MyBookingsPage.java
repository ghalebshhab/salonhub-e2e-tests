package com.salonhub.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;

/** {@code /my-bookings} — the customer's bookings. Needs a signed-in user. */
public class MyBookingsPage extends BasePage<MyBookingsPage> {

    public MyBookingsPage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return "/my-bookings";
    }

    @Override
    public MyBookingsPage waitUntilLoaded() {
        heading().first().waitFor();
        page.locator("#mb-panel").waitFor();
        return this;
    }

    /** Tabs: "Upcoming", "Past", "Cancelled". */
    @Step("Open the {name} tab")
    public MyBookingsPage openTab(String name) {
        page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(name)).click();
        return this;
    }

    public Locator tab(String name) {
        return page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(name));
    }

    public Locator card(long bookingId) {
        return page.locator("#booking-" + bookingId);
    }

    /** The status badge text, for example "Waiting for salon", "Confirmed", "Cancelled". */
    public Locator status(long bookingId) {
        return card(bookingId).locator(".mb-status");
    }

    @Step("Cancel booking {bookingId} and confirm")
    public MyBookingsPage cancel(long bookingId) {
        card(bookingId).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel booking")).click();
        confirmDialog().confirm("Cancel booking");
        return this;
    }

    @Step("Start to cancel booking {bookingId}, then keep it")
    public MyBookingsPage startCancelThenKeep(long bookingId) {
        card(bookingId).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel booking")).click();
        confirmDialog().dismiss("Keep booking");
        return this;
    }

    public Locator reviewButton(long bookingId) {
        return card(bookingId).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Write a review"));
    }

    /**
     * Opens the review dialog, picks {@code stars} (1-5), writes the comment and sends it.
     * The star buttons have no accessible name, so they are found by position.
     */
    @Step("Write a {stars}-star review for booking {bookingId}")
    public MyBookingsPage writeReview(long bookingId, int stars, String comment) {
        reviewButton(bookingId).click();
        Locator modal = page.locator(".review-modal");
        modal.waitFor();
        modal.locator(".star-button").nth(stars - 1).click();
        modal.locator("textarea").fill(comment);
        modal.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Submit Review")).click();
        modal.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
        return this;
    }

    /** The CliQ line on an unpaid CliQ booking: "Send 10.000 JOD by CliQ to ALIAS (name)". */
    public Locator cliqDetails(long bookingId) {
        return card(bookingId).locator(".pay-cliq-details");
    }

    /** The payment method label on the card, for example "CliQ transfer" or "Cash at the salon". */
    public Locator paymentMethod(long bookingId) {
        return card(bookingId).locator(".pay-strip-method");
    }

    @Step("Switch booking {bookingId} to the other payment method")
    public MyBookingsPage switchPaymentMethod(long bookingId) {
        card(bookingId).getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Change payment method")).click();
        Locator dialog = page.getByRole(AriaRole.DIALOG);
        Locator submit = dialog.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Change method"));
        com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat(submit).isEnabled();
        submit.click();
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
        return this;
    }
}
