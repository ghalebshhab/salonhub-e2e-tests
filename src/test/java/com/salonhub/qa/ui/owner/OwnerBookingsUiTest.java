package com.salonhub.qa.ui.owner;

import com.salonhub.qa.base.BaseUiTest;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.ui.pages.EmployeeBookingsPage;
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

@Epic("Bookings")
@Feature("Owner and employee bookings (UI)")
public class OwnerBookingsUiTest extends BaseUiTest {

    private static final String EMPLOYEE_NAME = "Rami";

    private String statusFromApi(long id) {
        return bookingApi.myBookings(customerApi()).jsonPath()
                .getString("data.find { it.id == " + id + " }.status");
    }

    @Test(groups = {"smoke", "ui", "owner", "bookings"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("The owner accepts a pending booking with an employee; the booking becomes ACCEPTED "
            + "and the customer sees 'Confirmed'.")
    public void ownerAcceptsBookingWithEmployee() {
        long id = bookingFlows.createPending(customerApi());
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));
        signInAs(TestAccounts.owner());

        SalonBookingsPage bookings = new SalonBookingsPage(page).open();
        assertThat(bookings.acceptButton(id)).isVisible();
        bookings.accept(id, EMPLOYEE_NAME, TestData.name("UI accept note"));

        Eventually.check(() -> assertThat(statusFromApi(id)).isEqualTo("ACCEPTED"));
        assertThat(bookings.acceptButton(id)).hasCount(0);
    }

    @Test(groups = {"ui", "owner", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The owner rejects a pending booking with a reason; the customer sees 'Not accepted'.")
    public void ownerRejectsBooking() {
        long id = bookingFlows.createPending(customerApi());
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));
        signInAs(TestAccounts.owner());

        new SalonBookingsPage(page).open().reject(id, TestData.name("UI reject reason"));

        Eventually.check(() -> assertThat(statusFromApi(id)).isEqualTo("REJECTED"));
        page.context().clearCookies();
        signInAs(TestAccounts.customer());
        MyBookingsPage myBookings = new MyBookingsPage(page).open();
        myBookings.openTab("Cancelled");
        assertThat(myBookings.status(id)).hasText("Not accepted");
    }

    @Test(groups = {"ui", "employee", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("After the owner assigns a booking to the employee, the employee sees it on their bookings page.")
    public void employeeSeesAssignedBooking() {
        long id = bookingFlows.createAccepted(customerApi(), ownerApi());
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));
        signInAs(TestAccounts.salonEmployee());

        EmployeeBookingsPage employeeBookings = new EmployeeBookingsPage(page).open();

        assertThat(employeeBookings.card(id)).isVisible();
    }

    @Test(groups = {"ui", "security", "negative"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A customer who opens an owner page sees the 'not for your account' message.")
    public void customerCannotOpenOwnerBookings() {
        signInAs(TestAccounts.customer());

        page.navigate("/owner/bookings");

        assertThat(new SalonBookingsPage(page).forbiddenMessage()).isVisible();
    }
}
