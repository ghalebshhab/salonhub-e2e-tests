package com.salonhub.qa.api.bookings;

import com.salonhub.qa.api.models.AvailabilityRequest;
import com.salonhub.qa.api.models.BookingAction;
import com.salonhub.qa.api.models.BookingRequest;
import com.salonhub.qa.base.BaseApiTest;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.data.TestSalon;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;

import static com.salonhub.qa.api.ApiAssertions.assertBusinessError;
import static com.salonhub.qa.api.ApiAssertions.assertRefused;
import static com.salonhub.qa.api.ApiAssertions.assertStatus;
import static com.salonhub.qa.api.ApiAssertions.assertSuccess;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings")
@Feature("Booking API")
public class BookingApiTest extends BaseApiTest {

    /** Creates a pending booking and cancels it after the class. */
    private long pendingBooking() {
        long id = bookingFlows.createPending(customer());
        cleanup(() -> bookingFlows.cancelQuietly(customer(), id));
        return id;
    }

    private long acceptedBooking() {
        long id = bookingFlows.createAccepted(customer(), owner());
        cleanup(() -> bookingFlows.cancelQuietly(customer(), id));
        return id;
    }

    private String statusOf(long bookingId) {
        List<Object> statuses = assertSuccess(bookingApi.myBookings(customer()))
                .getList("data.findAll { it.id == " + bookingId + " }.status");
        assertThat(statuses).as("booking %s in the customer's list", bookingId).hasSize(1);
        return String.valueOf(statuses.get(0));
    }

    // ---------- Availability ----------

    @Test(groups = {"smoke", "api", "bookings"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("The test salon has free slots, and each slot names the employee who can serve.")
    public void availabilityListsFreeSlotsWithEmployees() {
        long serviceId = bookingFlows.anyServiceId();
        JsonPath json = assertSuccess(bookingApi.availability(customer(), new AvailabilityRequest(
                TestSalon.id(), List.of(serviceId), TestData.futureDate().toString(), null)));

        assertThat(json.getList("data.slots")).as("free slots").isNotEmpty();
        assertThat(json.getList("data.slots[0].eligibleEmployees.id", Long.class)).contains(TestSalon.employeeId());
        assertThat(json.getInt("data.slotIntervalMinutes")).isPositive();
    }

    // ---------- Create ----------

    @Test(groups = {"smoke", "api", "bookings"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("A customer books a free slot: the booking is PENDING, matches the schema and has the right total.")
    public void customerCreatesPendingBooking() {
        BookingRequest request = bookingFlows.validRequest(customer());

        Response response = bookingApi.create(customer(), request);

        response.then().body(matchesJsonSchemaInClasspath("schemas/booking-response.schema.json"));
        JsonPath json = assertSuccess(response);
        long id = json.getLong("data.id");
        cleanup(() -> bookingFlows.cancelQuietly(customer(), id));
        assertThat(json.getString("data.status")).isEqualTo("PENDING");
        assertThat(json.getLong("data.salonId")).isEqualTo(TestSalon.id());
        assertThat(json.getLong("data.customerId")).isEqualTo(customer().userId());
        assertThat(json.getString("data.bookingDate")).isEqualTo(request.bookingDate());
        assertThat(json.getString("data.bookingTime")).isEqualTo(request.bookingTime());
        assertThat(json.getList("data.services.serviceId", Long.class)).isEqualTo(request.serviceIds());
        assertThat(json.getDouble("data.totalPrice")).isEqualTo(json.getDouble("data.services[0].price"));
    }

    @Test(groups = {"api", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A new booking shows in the owner's pending list, but not yet in the employee's list.")
    public void newBookingIsPendingForOwner() {
        long id = pendingBooking();

        assertThat(assertSuccess(bookingApi.mySalonPending(owner())).getList("data.id", Long.class)).contains(id);
        assertThat(assertSuccess(bookingApi.employeeBookings(employee())).getList("data.id", Long.class))
                .doesNotContain(id);
    }

    @Test(groups = {"api", "bookings", "negative"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A booking in the past is rejected.")
    public void bookingInThePastIsRejected() {
        BookingRequest request = bookingFlows.validRequest(customer())
                .withDate(LocalDate.now().minusDays(3).toString());

        assertBusinessError(bookingApi.create(customer(), request), "must be in the future");
    }

    @Test(groups = {"api", "bookings", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The customer location is required.")
    public void bookingWithoutLocationIsRejected() {
        BookingRequest request = bookingFlows.validRequest(customer()).withLocation(null);

        assertBusinessError(bookingApi.create(customer(), request), "location is required");
    }

    @Test(groups = {"api", "bookings", "negative"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A booking cannot use a service of another salon.")
    public void bookingWithServiceOfAnotherSalonIsRejected() {
        long foreignServiceId = assertSuccess(serviceApi.activeServicesOf(TestSalon.otherSalonId()))
                .getLong("data[0].id");
        BookingRequest request = bookingFlows.validRequest(customer()).withServices(List.of(foreignServiceId));

        assertBusinessError(bookingApi.create(customer(), request), "belong to this salon");
    }

    @Test(groups = {"api", "bookings", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Anonymous users cannot book (401).")
    public void anonymousCannotBook() {
        BookingRequest request = bookingFlows.validRequest(customer());

        assertStatus(bookingApi.create(null, request), 401);
    }

    // ---------- Owner decisions ----------

    @Test(groups = {"api", "bookings", "negative"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The owner must choose an employee before accepting.")
    public void acceptWithoutEmployeeIsRejected() {
        long id = pendingBooking();

        assertBusinessError(bookingApi.accept(owner(), id, null), "Choose the employee");
        assertThat(statusOf(id)).isEqualTo("PENDING");
    }

    @Test(groups = {"smoke", "api", "bookings"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("The owner accepts with an employee: customer sees ACCEPTED, the employee sees the booking.")
    public void ownerAcceptsBookingWithEmployee() {
        long id = pendingBooking();
        assertThat(assertSuccess(bookingApi.assignableEmployees(owner(), id)).getList("data.id", Long.class))
                .contains(TestSalon.employeeId());

        JsonPath json = assertSuccess(bookingApi.accept(owner(), id,
                new BookingAction(TestSalon.PREFIX + "see you", TestSalon.employeeId())));

        assertThat(json.getString("data.status")).isEqualTo("ACCEPTED");
        assertThat(json.getLong("data.assignedEmployeeId")).isEqualTo(TestSalon.employeeId());
        assertThat(statusOf(id)).isEqualTo("ACCEPTED");
        assertThat(assertSuccess(bookingApi.employeeBookings(employee())).getList("data.id", Long.class))
                .contains(id);
        assertThat(assertSuccess(bookingApi.mySalonPending(owner())).getList("data.id", Long.class))
                .doesNotContain(id);
    }

    @Test(groups = {"api", "bookings", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("A rejection needs a reason.")
    public void rejectWithoutReasonIsRejected() {
        long id = pendingBooking();

        assertBusinessError(bookingApi.reject(owner(), id, new BookingAction(null, null)), "reason is required");
    }

    @Test(groups = {"api", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The owner rejects with a reason; the customer sees REJECTED and cannot cancel it any more.")
    public void ownerRejectsBooking() {
        long id = pendingBooking();

        JsonPath json = assertSuccess(bookingApi.reject(owner(), id,
                new BookingAction(TestSalon.PREFIX + "fully booked", null)));

        assertThat(json.getString("data.status")).isEqualTo("REJECTED");
        assertThat(statusOf(id)).isEqualTo("REJECTED");
        assertBusinessError(bookingApi.cancel(customer(), id), "Only a pending or accepted booking");
    }

    @Test(groups = {"api", "bookings", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A customer cannot accept a booking, even their own.")
    public void customerCannotAcceptBooking() {
        long id = pendingBooking();

        assertRefused(bookingApi.accept(customer(), id, new BookingAction(null, TestSalon.employeeId())));
        assertThat(statusOf(id)).isEqualTo("PENDING");
    }

    // ---------- Customer actions ----------

    @Test(groups = {"api", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The customer cancels a pending booking; a second cancel is refused.")
    public void customerCancelsPendingBooking() {
        long id = pendingBooking();

        assertThat(assertSuccess(bookingApi.cancel(customer(), id)).getString("data.status")).isEqualTo("CANCELLED");
        assertBusinessError(bookingApi.cancel(customer(), id), "Only a pending or accepted booking");
    }

    @Test(groups = {"api", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The customer confirms attendance, then cancels an accepted booking.")
    public void customerConfirmsThenCancelsAcceptedBooking() {
        long id = acceptedBooking();

        assertSuccess(bookingApi.confirmAttendance(customer(), id));
        assertThat(assertSuccess(bookingApi.cancel(customer(), id)).getString("data.status")).isEqualTo("CANCELLED");
    }

    @Test(groups = {"api", "bookings", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Only the customer who booked can cancel: the owner and the employee cannot.")
    public void othersCannotCancelCustomerBooking() {
        long id = pendingBooking();

        assertBusinessError(bookingApi.cancel(owner(), id), "not found");
        assertBusinessError(bookingApi.cancel(employee(), id), "not found");
        assertThat(statusOf(id)).isEqualTo("PENDING");
    }

    @Test(groups = {"api", "bookings"})
    @Severity(SeverityLevel.NORMAL)
    @Description("An accepted booking has an unpaid cash payment and a tracking view for the customer.")
    public void acceptedBookingHasPaymentAndTracking() {
        long id = acceptedBooking();

        JsonPath payment = assertSuccess(bookingApi.payment(customer(), id));
        assertThat(payment.getString("data.status")).isEqualTo("UNPAID");
        assertThat(payment.getString("data.currency")).isEqualTo("JOD");
        assertThat(payment.getInt("data.amountFils")).isPositive();

        JsonPath tracking = assertSuccess(bookingApi.tracking(customer(), id));
        assertThat(tracking.getString("data.viewerRole")).isEqualTo("CUSTOMER");
        assertThat(tracking.getString("data.status")).isEqualTo("ACCEPTED");
    }

    @Test(groups = {"api", "bookings", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A customer cannot read the salon booking lists.")
    public void customerCannotReadSalonBookings() {
        assertRefused(bookingApi.mySalonBookings(customer()));
        assertRefused(bookingApi.mySalonPending(customer()));
    }
}
