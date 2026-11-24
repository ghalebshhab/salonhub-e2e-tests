package com.salonhub.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** {@code /employee/bookings} — an employee's own appointments. */
public class EmployeeBookingsPage extends BasePage<EmployeeBookingsPage> {

    public EmployeeBookingsPage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return "/employee/bookings";
    }

    @Override
    public EmployeeBookingsPage waitUntilLoaded() {
        heading().first().waitFor();
        return this;
    }

    public Locator card(long bookingId) {
        return page.locator("#booking-" + bookingId);
    }
}
