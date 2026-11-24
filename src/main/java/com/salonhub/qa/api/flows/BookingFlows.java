package com.salonhub.qa.api.flows;

import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.endpoints.BookingApi;
import com.salonhub.qa.api.endpoints.ServiceApi;
import com.salonhub.qa.api.models.AvailabilityRequest;
import com.salonhub.qa.api.models.BookingAction;
import com.salonhub.qa.api.models.BookingRequest;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.data.TestSalon;
import io.qameta.allure.Step;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.salonhub.qa.api.ApiAssertions.assertSuccess;

/**
 * Multi-step booking set-ups that many tests need. Every booking is made at
 * {@link TestSalon} and carries an "E2E" note.
 */
public final class BookingFlows {

    private final BookingApi bookingApi = new BookingApi();
    private final ServiceApi serviceApi = new ServiceApi();

    /** The id of the first active service of the test salon. */
    public long anyServiceId() {
        JsonPath json = assertSuccess(serviceApi.activeServicesOf(TestSalon.id()));
        List<Long> ids = json.getList("data.id", Long.class);
        if (ids.isEmpty()) {
            throw new IllegalStateException("Test salon " + TestSalon.id() + " has no active services");
        }
        return ids.get(0);
    }

    /** A valid booking body on a free slot, found with the availability endpoint. */
    @Step("Find a free slot and build a booking request")
    public BookingRequest validRequest(ApiSession customer) {
        long serviceId = anyServiceId();
        for (int attempt = 0; attempt < 10; attempt++) {
            LocalDate date = TestData.futureDate();
            JsonPath json = assertSuccess(bookingApi.availability(customer,
                    new AvailabilityRequest(TestSalon.id(), List.of(serviceId), date.toString(), null)));
            List<String> times = json.getList("data.slots.time", String.class);
            if (!times.isEmpty()) {
                String time = times.get(ThreadLocalRandom.current().nextInt(times.size()));
                return new BookingRequest(TestSalon.id(), List.of(serviceId), null, TestData.jordanPhone(),
                        "Amman, Abdoun", date.toString(), time, TestSalon.PREFIX + "booking");
            }
        }
        throw new IllegalStateException("No free slot at salon " + TestSalon.id()
                + ". Did you run db/seed-e2e.sql?");
    }

    /** The server's answer when the customer already has a booking at that time. */
    public static final String OVERLAP_MESSAGE = "You have another booking at this time";

    /** Creates a PENDING booking and returns its id. */
    @Step("Customer creates a pending booking")
    public long createPending(ApiSession customer) {
        return createPending(customer, null);
    }

    /**
     * Creates a PENDING booking with this payment method (null = salon default).
     * Parallel tests book for the same customer, so a random slot can overlap
     * one of their bookings; then it tries another slot.
     */
    @Step("Customer creates a pending booking")
    public long createPending(ApiSession customer, String paymentMethod) {
        for (int attempt = 0; attempt < 5; attempt++) {
            Response response = bookingApi.create(customer,
                    validRequest(customer).withPaymentMethod(paymentMethod));
            if (OVERLAP_MESSAGE.equals(response.jsonPath().getString("message"))) {
                continue;
            }
            return assertSuccess(response).getLong("data.id");
        }
        throw new IllegalStateException("Every slot tried overlaps a booking of this customer");
    }

    /** Creates a booking and lets the owner accept it with the test employee. */
    @Step("Customer books and the owner accepts with the test employee")
    public long createAccepted(ApiSession customer, ApiSession owner) {
        return createAcceptedFrom(owner, createPending(customer));
    }

    /** The owner accepts an existing pending booking with the test employee. */
    @Step("The owner accepts booking {bookingId} with the test employee")
    public long createAcceptedFrom(ApiSession owner, long bookingId) {
        assertSuccess(bookingApi.accept(owner, bookingId,
                new BookingAction(TestSalon.PREFIX + "accepted", TestSalon.employeeId())));
        return bookingId;
    }

    /** Cancels a booking as its customer, and ignores "already cancelled" errors. For clean-up. */
    public void cancelQuietly(ApiSession customer, long bookingId) {
        bookingApi.cancel(customer, bookingId);
    }
}
