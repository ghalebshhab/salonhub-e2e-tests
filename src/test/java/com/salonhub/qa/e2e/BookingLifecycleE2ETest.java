package com.salonhub.qa.e2e;

import com.salonhub.qa.base.BaseE2ETest;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.data.TestSalon;
import com.salonhub.qa.ui.pages.EmployeeBookingsPage;
import com.salonhub.qa.ui.pages.MyBookingsPage;
import com.salonhub.qa.ui.pages.NotificationsPage;
import com.salonhub.qa.ui.pages.SalonDetailsPage;
import com.salonhub.qa.ui.pages.owner.SalonBookingsPage;
import com.salonhub.qa.utils.Eventually;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("End-to-end flows")
@Feature("Booking lifecycle")
public class BookingLifecycleE2ETest extends BaseE2ETest {

    private static final String EMPLOYEE_NAME = "Rami";

    private Long reviewIdFor(long bookingId) {
        List<Long> ids = reviewApi.myReviews(customerApi()).jsonPath()
                .getList("data.findAll { it.bookingId == " + bookingId + " }.id", Long.class);
        return ids.isEmpty() ? null : ids.get(0);
    }

    @Test(groups = {"smoke", "e2e", "bookings"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("Customer books in the wizard -> owner accepts with Rami -> Rami sees the booking -> "
            + "customer sees 'Confirmed' and a notification -> owner marks it completed -> "
            + "customer writes a 5-star review -> the review shows on the public salon page.")
    public void bookingFromRequestToReview() {
        String note = TestData.name("E2E lifecycle");
        String reviewText = TestData.name("Great visit, review");

        // 1. Customer
        signInAs(TestAccounts.customer());
        long id = bookThroughWizard(firstServiceName(), note);
        cleanup(() -> {
            Long reviewId = reviewIdFor(id);
            if (reviewId != null) {
                reviewApi.delete(customerApi(), reviewId);
            }
        });
        Allure.step("Booking " + id + " is PENDING for customer and owner", () -> {
            assertThat(customerSeesStatus(id)).isEqualTo("PENDING");
            assertThat(ownerSeesStatus(id)).isEqualTo("PENDING");
        });

        // 2. Owner accepts and assigns Rami
        switchUserTo(TestAccounts.owner());
        SalonBookingsPage salonBookings = new SalonBookingsPage(page).open();
        assertThat(salonBookings.card(id)).containsText(note);
        salonBookings.accept(id, EMPLOYEE_NAME, TestData.name("See you soon"));
        Eventually.check(() -> assertThat(customerSeesStatus(id)).isEqualTo("ACCEPTED"));

        // 3. Employee sees the booking
        switchUserTo(TestAccounts.salonEmployee());
        assertThat(new EmployeeBookingsPage(page).open().card(id)).isVisible();

        // 4. Customer sees the confirmation and the notification
        switchUserTo(TestAccounts.customer());
        assertThat(new MyBookingsPage(page).open().status(id)).hasText("Confirmed");
        Eventually.check(() -> assertThat(eventTypes(customerApi(), id)).contains("BOOKING_ACCEPTED"));
        assertThat(new NotificationsPage(page).open().items().first()).isVisible();

        // 5. Owner marks the visit completed
        switchUserTo(TestAccounts.owner());
        new SalonBookingsPage(page).open().complete(id);
        Eventually.check(() -> assertThat(customerSeesStatus(id)).isEqualTo("COMPLETED"));

        // 6. Customer reviews the visit
        switchUserTo(TestAccounts.customer());
        MyBookingsPage myBookings = new MyBookingsPage(page).open().openTab("Past");
        assertThat(myBookings.status(id)).hasText("Completed");
        myBookings.writeReview(id, 5, reviewText);

        Eventually.check(() -> {
            Long reviewId = reviewIdFor(id);
            assertThat(reviewId).as("review of booking %s", id).isNotNull();
            assertThat(reviewApi.myReviews(customerApi()).jsonPath()
                    .getInt("data.find { it.bookingId == " + id + " }.rating")).isEqualTo(5);
        });

        // 7. An anonymous visitor sees the review on the salon page
        signOutBrowser();
        assertThat(new SalonDetailsPage(page, TestSalon.id()).open().review(reviewText)).isVisible();
    }
}
