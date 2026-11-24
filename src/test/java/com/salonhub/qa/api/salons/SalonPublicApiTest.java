package com.salonhub.qa.api.salons;

import com.salonhub.qa.base.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Epic("Salon discovery")
@Feature("Public salon API")
public class SalonPublicApiTest extends BaseApiTest {

    @Test(groups = {"smoke", "api", "salons"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("Anyone can list the active salons.")
    public void activeSalonsArePublic() {
        Response response = salonApi.getActiveSalons();

        response.then().statusCode(200).body("success", equalTo(true));
        List<Map<String, Object>> salons = response.jsonPath().getList("data");
        assertThat(salons).as("active salons").isNotEmpty();
        assertThat(salons).allSatisfy(salon -> assertThat(salon).containsKey("id"));
    }

    @Test(groups = {"api", "salons"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Anyone can open the details of an active salon.")
    public void salonDetailsArePublic() {
        long salonId = salonApi.getActiveSalons().jsonPath().getLong("data[0].id");

        salonApi.getSalon(salonId).then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo((int) salonId));
    }

    @Test(groups = {"api", "salons", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("A salon id that does not exist does not return a salon.")
    public void unknownSalonIsNotFound() {
        Response response = salonApi.getSalon(999_999_999L);

        assertThat(response.statusCode()).isIn(200, 404);
        assertThat(response.jsonPath().getBoolean("success")).isFalse();
    }

    @Test(groups = {"api", "salons", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The full salon list is admin-only: anonymous calls get 401.")
    public void fullSalonListNeedsAdmin() {
        salonApi.getAllSalonsAnonymous().then().statusCode(401);
    }
}
