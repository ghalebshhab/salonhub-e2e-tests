package com.salonhub.qa.api.services;

import com.salonhub.qa.api.models.ServiceRequest;
import com.salonhub.qa.base.BaseApiTest;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.data.TestSalon;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static com.salonhub.qa.api.ApiAssertions.assertBusinessError;
import static com.salonhub.qa.api.ApiAssertions.assertRefused;
import static com.salonhub.qa.api.ApiAssertions.assertStatus;
import static com.salonhub.qa.api.ApiAssertions.assertSuccess;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Salon management")
@Feature("Salon services API")
public class SalonServiceApiTest extends BaseApiTest {

    private long createService(ServiceRequest body) {
        long id = assertSuccess(serviceApi.create(owner(), body)).getLong("data.id");
        cleanup(() -> serviceApi.disable(owner(), id));
        return id;
    }

    @Test(groups = {"smoke", "api", "services"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("The owner creates a service; it is active, matches the schema and shows in the public list.")
    public void ownerCreatesServiceAndItIsPublic() {
        ServiceRequest body = ServiceRequest.of(TestData.name("Service"), 9.5, 30);

        var response = serviceApi.create(owner(), body);

        response.then().body(matchesJsonSchemaInClasspath("schemas/service-response.schema.json"));
        JsonPath json = assertSuccess(response);
        long id = json.getLong("data.id");
        cleanup(() -> serviceApi.disable(owner(), id));
        assertThat(json.getString("data.name")).isEqualTo(body.name());
        assertThat(json.getLong("data.salonId")).isEqualTo(TestSalon.id());
        assertThat(new BigDecimal(json.getString("data.price"))).isEqualByComparingTo("9.5");
        assertThat(json.getInt("data.durationMinutes")).isEqualTo(30);
        assertThat(json.getBoolean("data.isActive")).isTrue();
        assertThat(serviceApi.activeServicesOf(TestSalon.id()).jsonPath().getList("data.id", Long.class))
                .as("public service list").contains(id);
    }

    @Test(groups = {"api", "services"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The owner updates name, price and duration of a service.")
    public void ownerUpdatesService() {
        long id = createService(ServiceRequest.of(TestData.name("Service"), 10, 30));
        ServiceRequest changes = ServiceRequest.of(TestData.name("Updated"), 12.75, 45).active(true);

        JsonPath json = assertSuccess(serviceApi.update(owner(), id, changes));

        assertThat(json.getString("data.name")).isEqualTo(changes.name());
        assertThat(new BigDecimal(json.getString("data.price"))).isEqualByComparingTo("12.75");
        assertThat(json.getInt("data.durationMinutes")).isEqualTo(45);
    }

    @Test(groups = {"api", "services", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("An update must say if the service is active.")
    public void updateWithoutActiveFlagIsRejected() {
        long id = createService(ServiceRequest.of(TestData.name("Service"), 10, 30));

        assertBusinessError(serviceApi.update(owner(), id, ServiceRequest.of(TestData.name("X"), 11, 30)),
                "active status is required");
    }

    @Test(groups = {"api", "services"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A disabled service is gone from the public list but stays in the owner's list as inactive.")
    public void disabledServiceIsHiddenFromCustomers() {
        long id = createService(ServiceRequest.of(TestData.name("Service"), 10, 30));

        JsonPath json = assertSuccess(serviceApi.disable(owner(), id));

        assertThat(json.getBoolean("data.isActive")).isFalse();
        assertThat(serviceApi.activeServicesOf(TestSalon.id()).jsonPath().getList("data.id", Long.class))
                .doesNotContain(id);
        assertThat(assertSuccess(serviceApi.mySalonServices(owner())).getList("data.id", Long.class))
                .contains(id);
    }

    @DataProvider(name = "invalidServices")
    public Object[][] invalidServices() {
        return new Object[][]{
                {"no name", new ServiceRequest(null, "x", BigDecimal.TEN, 30, null), "name is required"},
                {"blank name", new ServiceRequest("  ", "x", BigDecimal.TEN, 30, null), "name is required"},
                {"negative price", ServiceRequest.of(TestData.name("Bad"), -1, 30), "price must be greater than 0"},
                {"zero price", ServiceRequest.of(TestData.name("Bad"), 0, 30), "price must be greater than 0"},
                {"zero duration", ServiceRequest.of(TestData.name("Bad"), 5, 0), "duration must be greater than 0"},
                {"negative duration", ServiceRequest.of(TestData.name("Bad"), 5, -15), "duration must be greater than 0"},
        };
    }

    @Test(groups = {"api", "services", "negative"}, dataProvider = "invalidServices")
    @Severity(SeverityLevel.NORMAL)
    @Description("The backend validates the service fields.")
    public void invalidServiceIsRejected(String scenario, ServiceRequest body, String expectedMessage) {
        var response = serviceApi.create(owner(), body);
        if (response.jsonPath().getBoolean("success")) {
            long id = response.jsonPath().getLong("data.id");
            serviceApi.disable(owner(), id);
        }
        assertBusinessError(response, expectedMessage);
    }

    @Test(groups = {"api", "services", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A customer has no salon, so cannot create a service.")
    public void customerCannotCreateService() {
        assertBusinessError(serviceApi.create(customer(), ServiceRequest.of(TestData.name("Hack"), 5, 30)),
                "salon owned by you is required");
    }

    @Test(groups = {"api", "services", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("An owner cannot change a service of another salon.")
    public void ownerCannotUpdateServiceOfAnotherSalon() {
        long foreignServiceId = assertSuccess(serviceApi.activeServicesOf(TestSalon.otherSalonId()))
                .getLong("data[0].id");

        assertBusinessError(serviceApi.update(owner(), foreignServiceId,
                ServiceRequest.of(TestData.name("Hack"), 1, 10).active(true)), "not found");
        assertBusinessError(serviceApi.disable(owner(), foreignServiceId), "not found");
    }

    @Test(groups = {"api", "services", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Anonymous users cannot create services (401).")
    public void anonymousCannotCreateService() {
        assertStatus(serviceApi.create(null, ServiceRequest.of(TestData.name("Anon"), 5, 30)), 401);
    }

    @Test(groups = {"api", "services", "security"})
    @Severity(SeverityLevel.NORMAL)
    @Description("Only owners and employees can read the 'my salon' service list.")
    public void customerCannotReadMySalonServices() {
        assertRefused(serviceApi.mySalonServices(customer()));
    }
}
