package com.salonhub.qa.base;

import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.endpoints.NotificationApi;
import com.salonhub.qa.api.endpoints.ReviewApi;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.data.TestSalon;
import com.salonhub.qa.ui.pages.BookingSentPage;
import com.salonhub.qa.ui.pages.BookingWizardPage;
import io.qameta.allure.Step;

import java.time.LocalDate;
import java.util.List;

/**
 * Parent of the hybrid end-to-end flows. A flow moves one booking (or post)
 * through several roles in the browser, and checks each step in the UI and
 * in the API. Steps that are not under test (giving an employee a skill, for
 * example) use the API to stay fast.
 */
public abstract class BaseE2ETest extends BaseUiTest {

    protected final ReviewApi reviewApi = new ReviewApi();
    protected final NotificationApi notificationApi = new NotificationApi();

    /** The first active service of the test salon. */
    protected String firstServiceName() {
        return serviceApi.activeServicesOf(TestSalon.id()).jsonPath().getString("data[0].name");
    }

    /**
     * The signed-in customer books {@code serviceName} through the wizard and
     * lets the salon pick the employee. The booking is cancelled after the class
     * if it is still open. Returns the new booking id.
     */
    @Step("Customer books \"{serviceName}\" through the wizard")
    protected long bookThroughWizard(String serviceName, String note) {
        LocalDate day = TestData.futureDate();
        BookingWizardPage wizard = new BookingWizardPage(page, TestSalon.id()).open()
                .chooseService(serviceName).next()
                .letSalonAssign().next()
                .chooseDate(day);
        wizard.chooseFirstFreeTime();
        wizard.next();
        BookingSentPage sent = wizard.fillDetails("Amman, Abdoun", TestData.jordanPhone(), note).send();
        long id = sent.bookingId();
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));
        return id;
    }

    /** The booking status that the customer sees in the API. */
    protected String customerSeesStatus(long bookingId) {
        return bookingApi.myBookings(customerApi()).jsonPath()
                .getString("data.find { it.id == " + bookingId + " }.status");
    }

    /** The booking status that the salon owner sees in the API. */
    protected String ownerSeesStatus(long bookingId) {
        return bookingApi.mySalonBookings(ownerApi()).jsonPath()
                .getString("data.find { it.id == " + bookingId + " }.status");
    }

    /** The notification event types that {@code session} got for one booking. */
    protected List<String> eventTypes(ApiSession session, long bookingId) {
        return notificationApi.list(session).jsonPath()
                .getList("data.content.findAll { it.bookingId == " + bookingId + " }.eventType", String.class);
    }
}
