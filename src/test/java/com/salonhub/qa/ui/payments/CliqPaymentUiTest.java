package com.salonhub.qa.ui.payments;

import com.salonhub.qa.api.flows.CliqSetup;
import com.salonhub.qa.base.BaseUiTest;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.ui.pages.MyBookingsPage;
import com.salonhub.qa.ui.pages.owner.SalonBookingsPage;
import com.salonhub.qa.utils.Eventually;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Payments")
@Feature("CliQ payments (UI)")
public class CliqPaymentUiTest extends BaseUiTest {

    @BeforeClass(alwaysRun = true)
    public void turnOnCliq() {
        CliqSetup.acquire(ownerApi());
    }

    @AfterClass(alwaysRun = true)
    public void restoreSettings() {
        CliqSetup.release(ownerApi());
    }

    private String paymentField(long bookingId, String field) {
        return bookingApi.myBookings(customerApi()).jsonPath()
                .getString("data.find { it.id == " + bookingId + " }.payment." + field);
    }

    @Test(groups = {"ui", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The customer sees where to send the CliQ transfer, then switches the booking to cash.")
    public void customerSeesAliasAndSwitchesToCash() {
        long id = bookingFlows.createPending(customerApi(), "CLIQ");
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));
        signInAs(TestAccounts.customer());

        MyBookingsPage myBookings = new MyBookingsPage(page).open();
        assertThat(myBookings.paymentMethod(id)).hasText("CliQ transfer");
        assertThat(myBookings.cliqDetails(id)).containsText(CliqSetup.ALIAS);
        assertThat(myBookings.cliqDetails(id)).containsText(CliqSetup.NAME);

        myBookings.switchPaymentMethod(id);

        Eventually.check(() -> assertThat(paymentField(id, "method")).isEqualTo("CASH"));
        assertThat(myBookings.paymentMethod(id)).hasText("Cash at the salon");
        assertThat(myBookings.cliqDetails(id)).hasCount(0);
    }

    @Test(groups = {"smoke", "ui", "owner", "bookings"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("The owner marks an accepted CliQ booking as 'Paid by CliQ'; the payment becomes PAID.")
    public void ownerMarksBookingPaidByCliq() {
        long id = bookingFlows.createPending(customerApi(), "CLIQ");
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));
        bookingFlows.createAcceptedFrom(ownerApi(), id);
        signInAs(TestAccounts.owner());

        SalonBookingsPage bookings = new SalonBookingsPage(page).open();
        bookings.markPaidByCliq(id, "E2E-UI-REF");

        Eventually.check(() -> assertThat(paymentField(id, "status")).isEqualTo("PAID"));
        assertThat(bookings.paidByCliqButton(id)).hasCount(0);
    }
}
