package com.salonhub.qa.api.owner;

import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.models.ClosureRequest;
import com.salonhub.qa.base.BaseApiTest;
import com.salonhub.qa.data.TestSalon;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

import static com.salonhub.qa.api.ApiAssertions.assertBusinessError;
import static com.salonhub.qa.api.ApiAssertions.assertStatus;
import static com.salonhub.qa.api.ApiAssertions.assertSuccess;
import static com.salonhub.qa.api.endpoints.OwnerApi.DASHBOARD;
import static com.salonhub.qa.api.endpoints.OwnerApi.DASHBOARD_SUMMARY;
import static com.salonhub.qa.api.endpoints.OwnerApi.PAYMENTS_OUTSTANDING;
import static com.salonhub.qa.api.endpoints.OwnerApi.PAYMENTS_SUMMARY;
import static com.salonhub.qa.api.endpoints.OwnerApi.SUBSCRIPTION;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Salon management")
@Feature("Owner dashboard, plan and payments API")
public class OwnerApiTest extends BaseApiTest {

    @DataProvider(name = "ownerOnlyPaths")
    public Object[][] ownerOnlyPaths() {
        return new Object[][]{{DASHBOARD}, {DASHBOARD_SUMMARY}, {SUBSCRIPTION}, {PAYMENTS_SUMMARY}, {PAYMENTS_OUTSTANDING}};
    }

    @Test(groups = {"smoke", "api", "owner"}, dataProvider = "ownerOnlyPaths")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The owner can open each owner page.")
    public void ownerCanReadOwnerEndpoint(String path) {
        assertSuccess(ownerApi.get(owner(), path));
    }

    @Test(groups = {"api", "owner", "security"}, dataProvider = "ownerOnlyPaths")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Customers and employees get 403 on owner pages; anonymous users get 401.")
    public void otherRolesCannotReadOwnerEndpoint(String path) {
        for (ApiSession other : new ApiSession[]{customer(), employee()}) {
            assertStatus(ownerApi.get(other, path), 403);
        }
        assertStatus(ownerApi.get(null, path), 401);
    }

    @Test(groups = {"api", "owner"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The dashboard is about the owner's own salon.")
    public void dashboardShowsOwnSalon() {
        JsonPath json = assertSuccess(ownerApi.get(owner(), DASHBOARD));

        assertThat(json.getLong("data.salon.id")).isEqualTo(TestSalon.id());
    }

    @Test(groups = {"api", "owner"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The dashboard summary compares the current period with the period before it.")
    public void dashboardSummaryHasTwoPeriods() {
        JsonPath json = assertSuccess(ownerApi.get(owner(), DASHBOARD_SUMMARY));

        LocalDate from = LocalDate.parse(json.getString("data.from"));
        LocalDate to = LocalDate.parse(json.getString("data.to"));
        LocalDate compareTo = LocalDate.parse(json.getString("data.compareTo"));
        assertThat(from).isBeforeOrEqualTo(to);
        assertThat(compareTo).isBefore(from);
        assertThat(json.getInt("data.current.bookings")).isGreaterThanOrEqualTo(0);
    }

    @Test(groups = {"api", "owner"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The subscription belongs to the test salon and names a known plan.")
    public void subscriptionNamesAKnownPlan() {
        JsonPath json = assertSuccess(ownerApi.get(owner(), SUBSCRIPTION));
        var planCodes = assertSuccess(ownerApi.plans()).getList("data.code", String.class);

        assertThat(json.getLong("data.salonId")).isEqualTo(TestSalon.id());
        assertThat(planCodes).contains(json.getString("data.plan"));
    }

    @Test(groups = {"smoke", "api", "owner"})
    @Severity(SeverityLevel.NORMAL)
    @Description("Anyone can read the plans; the Free plan costs 0 and every plan has limits.")
    public void plansArePublic() {
        JsonPath json = assertSuccess(ownerApi.plans());

        assertThat(json.getList("data.code", String.class)).contains("FREE");
        assertThat(json.getInt("data.find { it.code == 'FREE' }.monthlyPriceFils")).isZero();
        assertThat(json.getInt("data.find { it.code == 'FREE' }.maxServices")).as("Free plan service limit").isPositive();
        assertThat(json.getList("data.maxServices", Integer.class)).as("limits: a number above 0, or null for no limit")
                .allMatch(n -> n == null || n > 0);
    }

    @Test(groups = {"api", "owner"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The owner's team list holds the test employee.")
    public void teamListHasTheTestEmployee() {
        assertThat(assertSuccess(ownerApi.myTeam(owner())).getList("data.id", Long.class))
                .contains(TestSalon.employeeId());
    }

    @Test(groups = {"api", "owner"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The owner can read the salon payment settings.")
    public void ownerReadsPaymentSettings() {
        JsonPath json = assertSuccess(ownerApi.paymentSettings(owner(), TestSalon.id()));

        assertThat(json.getLong("data.salonId")).isEqualTo(TestSalon.id());
        assertThat(json.getInt("data.freeCancelHours")).isGreaterThanOrEqualTo(0);
    }

    @Test(groups = {"api", "owner"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The owner adds a closure day; it is public; the owner removes it.")
    public void ownerAddsAndRemovesClosure() {
        String day = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(60, 120)).toString();

        JsonPath json = assertSuccess(ownerApi.addClosure(owner(), TestSalon.id(),
                new ClosureRequest(day, day, TestSalon.PREFIX + "holiday")));
        long id = json.getLong("data.id");
        cleanup(() -> ownerApi.removeClosure(owner(), TestSalon.id(), id));

        assertThat(json.getString("data.startDate")).isEqualTo(day);
        assertThat(assertSuccess(ownerApi.closures(TestSalon.id())).getList("data.id", Long.class)).contains(id);

        assertSuccess(ownerApi.removeClosure(owner(), TestSalon.id(), id));
        assertThat(assertSuccess(ownerApi.closures(TestSalon.id())).getList("data.id", Long.class))
                .doesNotContain(id);
    }

    @Test(groups = {"api", "owner", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A customer cannot add a closure to a salon.")
    public void customerCannotAddClosure() {
        String day = LocalDate.now().plusDays(90).toString();

        assertBusinessError(ownerApi.addClosure(customer(), TestSalon.id(),
                new ClosureRequest(day, day, TestSalon.PREFIX + "hack")), "salon owned by you is required");
    }
}
