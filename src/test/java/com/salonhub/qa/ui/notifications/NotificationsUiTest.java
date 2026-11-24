package com.salonhub.qa.ui.notifications;

import com.salonhub.qa.api.endpoints.NotificationApi;
import com.salonhub.qa.base.BaseUiTest;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.ui.pages.NotificationsPage;
import com.salonhub.qa.utils.Eventually;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Notifications")
@Feature("Notifications page (UI)")
public class NotificationsUiTest extends BaseUiTest {

    private final NotificationApi notificationApi = new NotificationApi();

    private long unreadCount() {
        return notificationApi.unreadCount(customerApi()).jsonPath().getLong("data.unreadCount");
    }

    @Test(groups = {"ui", "notifications"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("When the owner accepts a booking, the customer gets an unread notification; "
            + "'Mark all as read' clears the unread count.")
    public void acceptedBookingNotifiesCustomer() {
        long id = bookingFlows.createAccepted(customerApi(), ownerApi());
        cleanup(() -> bookingFlows.cancelQuietly(customerApi(), id));
        Eventually.check(() -> assertThat(unreadCount()).isPositive());
        signInAs(TestAccounts.customer());

        NotificationsPage notifications = new NotificationsPage(page).open();
        assertThat(notifications.unreadItems().first()).isVisible();
        notifications.markAllRead();

        assertThat(notifications.unreadItems()).hasCount(0);
        Eventually.check(() -> assertThat(unreadCount()).isZero());
    }
}
