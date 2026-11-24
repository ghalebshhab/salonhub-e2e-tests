package com.salonhub.qa.api.reviews;

import com.salonhub.qa.api.models.ReviewRequest;
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

import static com.salonhub.qa.api.ApiAssertions.assertBusinessError;
import static com.salonhub.qa.api.ApiAssertions.assertRefused;
import static com.salonhub.qa.api.ApiAssertions.assertStatus;
import static com.salonhub.qa.api.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reviews need a COMPLETED booking, and a booking completes only through the
 * live service flow on the booking day. So these tests cover the public read
 * side and the rules that stop early reviews.
 */
@Epic("Reviews")
@Feature("Review API")
public class ReviewApiTest extends BaseApiTest {

    @Test(groups = {"smoke", "api", "reviews"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Anyone can read the reviews of a salon; every rating is 1 to 5.")
    public void salonReviewsArePublic() {
        JsonPath json = assertSuccess(reviewApi.ofSalon(TestSalon.otherSalonId()));

        List<Map<String, Object>> reviews = json.getList("data");
        assertThat(reviews).allSatisfy(review -> {
            assertThat(review.get("salonId")).isEqualTo((int) TestSalon.otherSalonId());
            assertThat((Integer) review.get("rating")).isBetween(1, 5);
        });
    }

    @Test(groups = {"api", "reviews"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The review summary matches the review list: same count and same average.")
    public void reviewSummaryMatchesReviewList() {
        long salonId = TestSalon.otherSalonId();
        List<Integer> ratings = assertSuccess(reviewApi.ofSalon(salonId)).getList("data.rating", Integer.class);

        JsonPath summary = assertSuccess(reviewApi.summaryOf(salonId));

        assertThat(summary.getInt("data.totalReviews")).isEqualTo(ratings.size());
        double average = ratings.stream().mapToInt(Integer::intValue).average().orElse(0);
        assertThat(summary.getDouble("data.averageRating")).isCloseTo(average, org.assertj.core.data.Offset.offset(0.05));
    }

    @Test(groups = {"api", "reviews", "negative"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A customer cannot review a booking that is not completed.")
    public void reviewOfUncompletedBookingIsRejected() {
        long id = bookingFlows.createPending(customer());
        cleanup(() -> bookingFlows.cancelQuietly(customer(), id));

        assertBusinessError(reviewApi.create(customer(), new ReviewRequest(id, 5, TestSalon.PREFIX + "great")),
                "Only a completed booking can be reviewed");
    }

    @Test(groups = {"api", "reviews", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A user cannot review a booking of another customer.")
    public void ownerCannotReviewCustomerBooking() {
        long id = bookingFlows.createPending(customer());
        cleanup(() -> bookingFlows.cancelQuietly(customer(), id));

        assertRefused(reviewApi.create(owner(), new ReviewRequest(id, 1, TestSalon.PREFIX + "fake")));
    }

    @Test(groups = {"api", "reviews"})
    @Severity(SeverityLevel.NORMAL)
    @Description("A signed-in customer can list their own reviews.")
    public void customerCanListOwnReviews() {
        JsonPath json = assertSuccess(reviewApi.myReviews(customer()));

        assertThat(json.getList("data.customerId", Long.class)).allMatch(id -> id == customer().userId());
    }

    @Test(groups = {"api", "reviews", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Anonymous users cannot write reviews (401).")
    public void anonymousCannotReview() {
        assertStatus(reviewApi.create(null, new ReviewRequest(1L, 5, "x")), 401);
    }
}
