package com.salonhub.qa.api.bookings;

import com.salonhub.qa.api.flows.BookingFlows;
import com.salonhub.qa.api.models.AvailabilityRequest;
import com.salonhub.qa.api.models.BookingRequest;
import com.salonhub.qa.base.BaseApiTest;
import com.salonhub.qa.data.TestSalon;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.path.json.JsonPath;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.time.LocalTime;
import java.util.List;

import static com.salonhub.qa.api.ApiAssertions.assertBusinessError;
import static com.salonhub.qa.api.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who may book which time: a customer cannot hold two bookings at the same
 * time, and a salon owner cannot book at their own salon.
 */
@Epic("Bookings")
@Feature("Booking rules")
public class BookingRulesApiTest extends BaseApiTest {

    private static final String OWN_SALON_MESSAGE = "You are the owner of this salon";

    /** Books the request (retrying other slots on a clash with a parallel test) and cleans up after. */
    private BookingRequest bookFirst() {
        for (int attempt = 0; attempt < 5; attempt++) {
            BookingRequest request = bookingFlows.validRequest(customer());
            JsonPath json = bookingApi.create(customer(), request).jsonPath();
            if (BookingFlows.OVERLAP_MESSAGE.equals(json.getString("message"))) {
                continue;
            }
            assertThat(json.getBoolean("success")).as("first booking: %s", json.getString("message")).isTrue();
            long id = json.getLong("data.id");
            cleanup(() -> bookingFlows.cancelQuietly(customer(), id));
            return request;
        }
        throw new IllegalStateException("Could not book a first slot");
    }

    private int durationOf(long serviceId) {
        List<Integer> durations = assertSuccess(serviceApi.activeServicesOf(TestSalon.id()))
                .getList("data.findAll { it.id == " + serviceId + " }.durationMinutes", Integer.class);
        assertThat(durations).as("duration of service %s", serviceId).hasSize(1);
        return durations.get(0);
    }

    @Test(groups = {"api", "bookings", "negative"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The same customer cannot book the same time twice: 'You have another booking at this time'.")
    public void customerCannotBookTheSameTimeTwice() {
        BookingRequest first = bookFirst();

        assertBusinessError(bookingApi.create(customer(), first), BookingFlows.OVERLAP_MESSAGE);
    }

    @Test(groups = {"api", "bookings", "negative"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Another service at the same time is also refused: the customer cannot be in two places.")
    public void customerCannotBookAnotherServiceAtTheSameTime() {
        BookingRequest first = bookFirst();
        List<Long> serviceIds = assertSuccess(serviceApi.activeServicesOf(TestSalon.id()))
                .getList("data.id", Long.class);
        Long other = serviceIds.stream().filter(id -> !first.serviceIds().contains(id)).findFirst()
                .orElseThrow(() -> new SkipException("The test salon has only one active service"));

        assertBusinessError(bookingApi.create(customer(), first.withServices(List.of(other))),
                BookingFlows.OVERLAP_MESSAGE);
    }

    @Test(groups = {"api", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A time that does not overlap the first booking on the same day is allowed.")
    public void customerCanBookAnotherTimeOnTheSameDay() {
        BookingRequest first = bookFirst();
        long serviceId = first.serviceIds().get(0);
        LocalTime start = LocalTime.parse(first.bookingTime());
        LocalTime end = start.plusMinutes(durationOf(serviceId));

        List<String> times = assertSuccess(bookingApi.availability(customer(), new AvailabilityRequest(
                TestSalon.id(), List.of(serviceId), first.bookingDate(), null)))
                .getList("data.slots.time", String.class);
        String free = times.stream()
                .filter(time -> !LocalTime.parse(time).isBefore(end))
                .findFirst()
                .orElseThrow(() -> new SkipException("No later free slot on " + first.bookingDate()));

        JsonPath json = bookingApi.create(customer(), first.withTime(free)).jsonPath();
        if (BookingFlows.OVERLAP_MESSAGE.equals(json.getString("message"))) {
            throw new SkipException("A parallel test booked " + free + " for the same customer");
        }
        assertThat(json.getBoolean("success")).as("second booking: %s", json.getString("message")).isTrue();
        long id = json.getLong("data.id");
        cleanup(() -> bookingFlows.cancelQuietly(customer(), id));
        assertThat(json.getString("data.status")).isEqualTo("PENDING");
    }

    @Test(groups = {"api", "bookings", "negative"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The salon owner cannot book at their own salon: 'You are the owner of this salon'.")
    public void ownerCannotBookAtOwnSalon() {
        BookingRequest request = bookingFlows.validRequest(customer());

        assertBusinessError(bookingApi.create(owner(), request), OWN_SALON_MESSAGE);
    }
}
