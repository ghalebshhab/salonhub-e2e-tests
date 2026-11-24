package com.salonhub.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The "Request sent" screen after the booking wizard. */
public class BookingSentPage extends BasePage<BookingSentPage> {

    private static final Pattern BOOKING_ID = Pattern.compile("bookingId=(\\d+)");

    public BookingSentPage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        throw new UnsupportedOperationException("This screen has no own URL");
    }

    @Override
    public BookingSentPage waitUntilLoaded() {
        page.locator("#bk-sent-title").waitFor();
        return this;
    }

    public Locator title() {
        return page.locator("#bk-sent-title");
    }

    public Locator trackLink() {
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Track my booking"));
    }

    /** The new booking's id, read from the "Track my booking" link. */
    public long bookingId() {
        String href = trackLink().getAttribute("href");
        Matcher matcher = BOOKING_ID.matcher(href == null ? "" : href);
        if (!matcher.find()) {
            throw new IllegalStateException("No booking id in the link: " + href);
        }
        return Long.parseLong(matcher.group(1));
    }

    public MyBookingsPage trackBooking() {
        trackLink().click();
        return new MyBookingsPage(page).waitUntilLoaded();
    }
}
