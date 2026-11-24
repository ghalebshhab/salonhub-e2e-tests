package com.salonhub.qa.api.hiring;

import com.salonhub.qa.api.models.HiringPostRequest;
import com.salonhub.qa.base.BaseApiTest;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.data.TestSalon;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static com.salonhub.qa.api.ApiAssertions.assertBusinessError;
import static com.salonhub.qa.api.ApiAssertions.assertStatus;
import static com.salonhub.qa.api.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The test salon is on the Free plan, which allows one open hiring post.
 * Tests in this class share that one slot, so they run one after another
 * (single-threaded class) and close every post they open.
 */
@Epic("Hiring")
@Feature("Hiring post API")
@Test(singleThreaded = true)
public class HiringApiTest extends BaseApiTest {

    private static HiringPostRequest validPost() {
        return new HiringPostRequest(TestData.name("Barber wanted"), TestData.sentence(), List.of("Fades", "Beard"),
                20, 45, "Amman", 1, 1,
                LocalDateTime.now().plusDays(10).truncatedTo(ChronoUnit.SECONDS).toString());
    }

    /** Closes open posts that an earlier, broken run left behind, so the Free-plan slot is free. */
    @BeforeClass(alwaysRun = true)
    public void closeLeftoverTestPosts() {
        List<Map<String, Object>> posts = assertSuccess(hiringApi.ownerPosts(owner())).getList("data.hiringPost");
        posts.stream()
                .filter(p -> "Open".equalsIgnoreCase(String.valueOf(p.get("status"))))
                .filter(p -> String.valueOf(p.get("title")).startsWith(TestSalon.PREFIX))
                .forEach(p -> hiringApi.close(owner(), ((Number) p.get("id")).longValue()));
    }

    /** Closes this test's posts at once, so the next test can open one. */
    @AfterMethod(alwaysRun = true)
    public void closePostsOfThisTest() {
        runCleanups();
    }

    private long openPost() {
        long id = assertSuccess(hiringApi.create(owner(), validPost())).getLong("data.id");
        cleanup(() -> hiringApi.close(owner(), id));
        return id;
    }

    @Test(groups = {"smoke", "api", "hiring"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("The owner opens a hiring post; it is Open and job seekers can see it.")
    public void ownerOpensHiringPostAndItIsListed() {
        HiringPostRequest body = validPost();

        JsonPath json = assertSuccess(hiringApi.create(owner(), body));
        long id = json.getLong("data.id");
        cleanup(() -> hiringApi.close(owner(), id));

        assertThat(json.getString("data.status")).isEqualTo("Open");
        assertThat(json.getString("data.title")).isEqualTo(body.title());
        assertThat(json.getList("data.requiredSkills", String.class)).containsExactlyInAnyOrderElementsOf(body.requiredSkills());
        assertThat(assertSuccess(hiringApi.openPosts(customer())).getList("data.content.hiringPost.id", Long.class)).contains(id);
        assertThat(assertSuccess(hiringApi.ownerPosts(owner())).getList("data.hiringPost.id", Long.class)).contains(id);
    }

    @Test(groups = {"api", "hiring"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The owner closes a post: it becomes Closed and leaves the public list.")
    public void ownerClosesHiringPost() {
        long id = openPost();

        assertSuccess(hiringApi.close(owner(), id));

        assertThat(assertSuccess(hiringApi.ownerPost(owner(), id)).getString("data.hiringPost.status")).isEqualTo("Closed");
        assertThat(assertSuccess(hiringApi.openPosts(customer())).getList("data.content.hiringPost.id", Long.class))
                .doesNotContain(id);
    }

    @Test(groups = {"api", "hiring", "plans"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The Free plan allows only one open hiring post.")
    public void freePlanAllowsOneOpenPost() {
        openPost();

        var second = hiringApi.create(owner(), validPost());
        if (second.jsonPath().getBoolean("success")) {
            long id = second.jsonPath().getLong("data.id");
            hiringApi.close(owner(), id);
        }
        assertBusinessError(second, "Free plan allows 1 open hiring post");
    }

    @DataProvider(name = "invalidAges")
    public Object[][] invalidAges() {
        return new Object[][]{
                {"no minimum age", null, 40},
                {"minimum age under 18", 16, 40},
        };
    }

    @Test(groups = {"api", "hiring", "negative"}, dataProvider = "invalidAges")
    @Severity(SeverityLevel.NORMAL)
    @Description("A hiring post needs a minimum age of 18 or more.")
    public void invalidMinimumAgeIsRejected(String scenario, Integer min, Integer max) {
        var response = hiringApi.create(owner(), validPost().withAges(min, max));
        if (response.jsonPath().getBoolean("success")) {
            hiringApi.close(owner(), response.jsonPath().getLong("data.id"));
        }
        assertBusinessError(response, "Minimum age");
    }

    @Test(groups = {"api", "hiring", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("A customer without a career profile cannot apply.")
    public void customerWithoutCareerProfileCannotApply() {
        long id = openPost();

        assertBusinessError(hiringApi.apply(customer(), id, TestSalon.PREFIX + "hire me"), "career");
    }

    @Test(groups = {"api", "hiring", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The description needs at least 10 characters.")
    public void shortDescriptionIsRejected() {
        HiringPostRequest body = validPost();
        HiringPostRequest shortText = new HiringPostRequest(body.title(), "E2E short", body.requiredSkills(),
                body.minimumAge(), body.maximumAge(), body.employeeLocation(),
                body.minimumYearsOfExperience(), body.numOfPositions(), body.expiresAt());

        var response = hiringApi.create(owner(), shortText);
        if (response.jsonPath().getBoolean("success")) {
            hiringApi.close(owner(), response.jsonPath().getLong("data.id"));
        }
        assertBusinessError(response, "at least 10 characters");
    }

    @Test(groups = {"api", "hiring", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A customer has no salon, so cannot open a hiring post.")
    public void customerCannotOpenHiringPost() {
        var response = hiringApi.create(customer(), validPost());

        assertThat(response.jsonPath().getBoolean("success")).isFalse();
    }

    @Test(groups = {"api", "hiring", "security"})
    @Severity(SeverityLevel.NORMAL)
    @Description("Hiring posts need a signed-in user (401).")
    public void anonymousCannotListHiringPosts() {
        assertStatus(hiringApi.openPosts(null), 401);
    }
}
