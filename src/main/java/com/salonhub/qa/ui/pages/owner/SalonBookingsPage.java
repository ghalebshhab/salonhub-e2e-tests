package com.salonhub.qa.ui.pages.owner;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.salonhub.qa.ui.pages.BasePage;
import io.qameta.allure.Step;

/** {@code /owner/bookings} — the salon's booking requests (owner or salon manager). */
public class SalonBookingsPage extends BasePage<SalonBookingsPage> {

    public SalonBookingsPage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return "/owner/bookings";
    }

    @Override
    public SalonBookingsPage waitUntilLoaded() {
        page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Salon Bookings")).waitFor();
        return this;
    }

    public Locator card(long bookingId) {
        return page.locator("#booking-" + bookingId);
    }

    private Locator dialog() {
        return page.getByRole(AriaRole.DIALOG);
    }

    @Step("Accept booking {bookingId} with employee {employeeName}")
    public SalonBookingsPage accept(long bookingId, String employeeName, String note) {
        card(bookingId).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Assign and accept")).click();
        dialog().locator("label.accept-employee").filter(new Locator.FilterOptions().setHasText(employeeName)).click();
        if (note != null) {
            dialog().locator("textarea").fill(note);
        }
        dialog().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Confirm and notify")).click();
        dialog().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
        return this;
    }

    @Step("Reject booking {bookingId}")
    public SalonBookingsPage reject(long bookingId, String reason) {
        card(bookingId).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Reject")).click();
        page.locator("#booking-rejection-reason").fill(reason);
        dialog().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Send Rejection")).click();
        dialog().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
        return this;
    }

    public Locator acceptButton(long bookingId) {
        return card(bookingId).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Assign and accept"));
    }

    @Step("Mark booking {bookingId} as completed and confirm")
    public SalonBookingsPage complete(long bookingId) {
        card(bookingId).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Mark Completed")).click();
        confirmDialog().confirm("Mark completed");
        return this;
    }

    public Locator paidByCliqButton(long bookingId) {
        return card(bookingId).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Paid by CliQ"));
    }

    @Step("Mark booking {bookingId} as paid by CliQ")
    public SalonBookingsPage markPaidByCliq(long bookingId, String reference) {
        paidByCliqButton(bookingId).click();
        if (reference != null) {
            dialog().getByLabel("Transfer reference (optional)").fill(reference);
        }
        dialog().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Yes, mark as paid")).click();
        dialog().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
        return this;
    }
}
