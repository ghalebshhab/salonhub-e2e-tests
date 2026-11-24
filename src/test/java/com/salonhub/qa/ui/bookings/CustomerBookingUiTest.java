package com.salonhub.qa.ui.bookings;

import com.salonhub.qa.base.BaseUiTest;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.data.TestSalon;
import com.salonhub.qa.ui.pages.BookingSentPage;
import com.salonhub.qa.ui.pages.BookingWizardPage;
import com.salonhub.qa.ui.pages.MyBookingsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings")
@Feature("Customer booking (UI)")
public class CustomerBookingUiTest extends BaseUiTest {

    private String firstServiceName() {
        return serviceApi.activeServicesOf(TestSalon.id()).jsonPath().getString("data[0].name");
    }

    private Map<String, Object> bookingFromApi(long id) {
        JsonPath json = bookingApi.myBookings(customerApi()).jsonPath();
        List<Map<String, Object>> found = json.getList("data.findAll { it.id == " + id + " }");
        assertThat(found).as("booking %s in the API", id).hasSize(1);
        return found.get(0);
    }

    @Test(groups = {"smoke", "ui", "bookings"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("A customer books through the 4-step wizard; the booking is saved as PENDING with the chosen "
            + "service, day, time and note, and shows as 'Waiting for salon'.")
    public void customerBooksThroughTheWizard() {
        signInAs(TestAccounts.customer());
        String service = firstServiceName();
        LocalDate day = TestData.futureDate();
        String note = TestData.name("UI booking");

        BookingWizardPage wizard = new BookingWizardPage(page, TestSalon.id()).open()
                .chooseService(service).next()
                .letSalonAssign().next()
                .chooseDate(day);
        String time = wizard.chooseFirstFreeTime();
        wizard.next();
        assertThat(wizard.summary()).containsText(service);
        BookingSentPage sent = wizard.fillDetails("Amman, Sweifieh", TestData.jordanPhone(), note).send();

        long id = sent.bookingId();
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));
        assertThat(sent.title()).isVisible();

        Map<String, Object> saved = bookingFromApi(id);
        assertThat(saved.get("status")).isEqualTo("PENDING");
        assertThat(saved.get("bookingDate")).isEqualTo(day.toString());
        assertThat(String.valueOf(saved.get("bookingTime"))).startsWith(time);
        assertThat(saved.get("note")).isEqualTo(note);
        assertThat(saved.get("customerLocation")).isEqualTo("Amman, Sweifieh");

        MyBookingsPage myBookings = sent.trackBooking();
        assertThat(myBookings.status(id)).hasText("Waiting for salon");
    }

    @Test(groups = {"ui", "bookings", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The wizard does not continue before a service is chosen.")
    public void wizardNeedsAServiceFirst() {
        signInAs(TestAccounts.customer());

        BookingWizardPage wizard = new BookingWizardPage(page, TestSalon.id()).open();

        assertThat(wizard.continueButton()).isDisabled();
        wizard.chooseService(firstServiceName());
        assertThat(wizard.continueButton()).isEnabled();
    }

    @Test(groups = {"ui", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The customer cancels a booking from 'My bookings' after the confirm dialog.")
    public void customerCancelsBookingInTheUi() {
        long id = bookingFlows.createPending(customerApi());
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));
        signInAs(TestAccounts.customer());

        MyBookingsPage myBookings = new MyBookingsPage(page).open();
        assertThat(myBookings.status(id)).hasText("Waiting for salon");
        myBookings.cancel(id);

        assertThat(myBookings.toast().success()).isVisible();
        com.salonhub.qa.utils.Eventually.check(() ->
                assertThat(bookingFromApi(id).get("status")).isEqualTo("CANCELLED"));
    }

    @Test(groups = {"ui", "bookings"})
    @Severity(SeverityLevel.NORMAL)
    @Description("'Keep booking' in the confirm dialog keeps the booking.")
    public void keepBookingInDialogDoesNotCancel() {
        long id = bookingFlows.createPending(customerApi());
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));
        signInAs(TestAccounts.customer());

        MyBookingsPage myBookings = new MyBookingsPage(page).open().startCancelThenKeep(id);

        assertThat(myBookings.confirmDialog().root()).isHidden();
        assertThat(myBookings.status(id)).hasText("Waiting for salon");
        assertThat(bookingFromApi(id).get("status")).isEqualTo("PENDING");
    }

    @Test(groups = {"ui", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("After the owner accepts, the customer sees 'Confirmed'.")
    public void acceptedBookingShowsConfirmed() {
        long id = bookingFlows.createAccepted(customerApi(), ownerApi());
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));
        signInAs(TestAccounts.customer());

        MyBookingsPage myBookings = new MyBookingsPage(page).open();

        assertThat(myBookings.status(id)).hasText("Confirmed");
    }
}
