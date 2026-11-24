package com.salonhub.qa.api.notifications;

import com.salonhub.qa.api.models.BookingAction;
import com.salonhub.qa.base.BaseApiTest;
import com.salonhub.qa.data.TestSalon;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.salonhub.qa.api.ApiAssertions.assertStatus;
import static com.salonhub.qa.api.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Notifications")
@Feature("Notification API")
public class NotificationApiTest extends BaseApiTest {

    private List<Map<String, Object>> notificationsFor(long bookingId, JsonPath json) {
        return json.getList("data.content.findAll { it.bookingId == " + bookingId + " }");
    }

    @Test(groups = {"smoke", "api", "notifications"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A new booking sends a notification to the salon owner.")
    public void newBookingNotifiesOwner() {
        long id = bookingFlows.createPending(customer());
        cleanup(() -> bookingFlows.cancelQuietly(customer(), id));

        List<Map<String, Object>> found = notificationsFor(id, assertSuccess(notificationApi.list(owner())));

        assertThat(found).as("owner notifications for booking %s", id).isNotEmpty();
        assertThat(found.get(0).get("domain")).isEqualTo("BOOKING");
    }

    @Test(groups = {"api", "notifications"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Rejecting a booking notifies the customer with the event BOOKING_REJECTED.")
    public void rejectedBookingNotifiesCustomer() {
        long id = bookingFlows.createPending(customer());
        assertSuccess(bookingApi.reject(owner(), id, new BookingAction(TestSalon.PREFIX + "closed that day", null)));

        List<Map<String, Object>> found = notificationsFor(id, assertSuccess(notificationApi.list(customer())));

        assertThat(found).extracting(n -> n.get("eventType")).contains("BOOKING_REJECTED");
    }

    @Test(groups = {"api", "notifications"})
    @Severity(SeverityLevel.NORMAL)
    @Description("After 'read all', the notification of a new booking is read.")
    public void readAllMarksNotificationsRead() {
        long id = bookingFlows.createPending(customer());
        cleanup(() -> bookingFlows.cancelQuietly(customer(), id));

        assertSuccess(notificationApi.readAll(owner()));

        List<Map<String, Object>> found = notificationsFor(id, assertSuccess(notificationApi.list(owner())));
        assertThat(found).isNotEmpty();
        assertThat(found).allSatisfy(n -> assertThat(n.get("readAt")).as("readAt").isNotNull());
    }

    @Test(groups = {"api", "notifications"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The unread count is a number of zero or more.")
    public void unreadCountIsANumber() {
        assertThat(assertSuccess(notificationApi.unreadCount(customer())).getInt("data.unreadCount"))
                .isGreaterThanOrEqualTo(0);
    }

    @Test(groups = {"api", "notifications", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Notifications need a signed-in user (401).")
    public void anonymousCannotReadNotifications() {
        assertStatus(notificationApi.list(null), 401);
        assertStatus(notificationApi.unreadCount(null), 401);
    }
}
