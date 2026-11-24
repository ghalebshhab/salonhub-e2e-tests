package com.salonhub.qa.e2e;

import com.salonhub.qa.base.BaseE2ETest;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.ui.pages.MyBookingsPage;
import com.salonhub.qa.ui.pages.owner.SalonBookingsPage;
import com.salonhub.qa.utils.Eventually;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("End-to-end flows")
@Feature("Booking rejected or cancelled")
public class BookingOutcomesE2ETest extends BaseE2ETest {

    @Test(groups = {"e2e", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Customer books in the wizard -> owner rejects it with a reason -> the customer gets "
            + "BOOKING_REJECTED and sees the booking as 'Not accepted' in the Cancelled tab.")
    public void ownerRejectsWhatTheCustomerBooked() {
        signInAs(TestAccounts.customer());
        long id = bookThroughWizard(firstServiceName(), TestData.name("E2E reject flow"));

        switchUserTo(TestAccounts.owner());
        new SalonBookingsPage(page).open().reject(id, TestData.name("Fully booked that day"));
        Eventually.check(() -> assertThat(ownerSeesStatus(id)).isEqualTo("REJECTED"));

        switchUserTo(TestAccounts.customer());
        MyBookingsPage myBookings = new MyBookingsPage(page).open().openTab("Cancelled");
        assertThat(myBookings.status(id)).hasText("Not accepted");
        Eventually.check(() -> assertThat(eventTypes(customerApi(), id)).contains("BOOKING_REJECTED"));
    }

    @Test(groups = {"e2e", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Owner accepts in the UI -> customer cancels the confirmed booking in the UI -> "
            + "the owner sees CANCELLED and gets BOOKING_CANCELLED; the accept button is gone.")
    public void customerCancelsAfterOwnerAccepted() {
        long id = bookingFlows.createPending(customerApi());
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));

        signInAs(TestAccounts.owner());
        new SalonBookingsPage(page).open().accept(id, "Rami", null);
        Eventually.check(() -> assertThat(customerSeesStatus(id)).isEqualTo("ACCEPTED"));

        switchUserTo(TestAccounts.customer());
        MyBookingsPage myBookings = new MyBookingsPage(page).open();
        assertThat(myBookings.status(id)).hasText("Confirmed");
        myBookings.cancel(id);
        Eventually.check(() -> assertThat(ownerSeesStatus(id)).isEqualTo("CANCELLED"));
        Eventually.check(() -> assertThat(eventTypes(ownerApi(), id)).contains("BOOKING_CANCELLED"));

        switchUserTo(TestAccounts.owner());
        SalonBookingsPage salonBookings = new SalonBookingsPage(page).open();
        assertThat(salonBookings.card(id)).isVisible();
        assertThat(salonBookings.acceptButton(id)).hasCount(0);
    }
}
