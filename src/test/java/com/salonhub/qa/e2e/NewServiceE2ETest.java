package com.salonhub.qa.e2e;

import com.salonhub.qa.api.endpoints.EmployeeApi;
import com.salonhub.qa.base.BaseE2ETest;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.data.TestSalon;
import com.salonhub.qa.ui.pages.SalonDetailsPage;
import com.salonhub.qa.ui.pages.owner.SalonBookingsPage;
import com.salonhub.qa.ui.pages.owner.ServicesPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("End-to-end flows")
@Feature("New service to first booking")
public class NewServiceE2ETest extends BaseE2ETest {

    private final EmployeeApi employeeApi = new EmployeeApi();

    private long serviceIdByName(String name) {
        return serviceApi.mySalonServices(ownerApi()).jsonPath()
                .getLong("data.find { it.name == '" + name + "' }.id");
    }

    private List<Long> employeeServiceIds() {
        return employeeApi.services(ownerApi(), TestSalon.employeeId()).jsonPath()
                .getList("data.services.id", Long.class);
    }

    @Test(groups = {"e2e", "services", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Owner adds a service in the UI -> owner gives it to Rami (API) -> the service shows on the "
            + "public salon page -> customer books it in the wizard -> owner sees the request with that service.")
    public void customerBooksANewService() {
        String name = TestData.name("New service");
        List<Long> skillsBefore = employeeServiceIds();

        // 1. Owner adds the service
        signInAs(TestAccounts.owner());
        new ServicesPage(page).open().addService().create(name, "Added by the E2E flow.", "15", "30");
        long serviceId = serviceIdByName(name);
        cleanup(() -> serviceApi.disable(ownerApi(), serviceId));

        // 2. Rami can do it (set-up through the API; the team page is not under test here)
        Allure.step("Give the new service to Rami", () -> {
            List<Long> skills = new ArrayList<>(skillsBefore);
            skills.add(serviceId);
            assertThat(employeeApi.setServices(ownerApi(), TestSalon.employeeId(), skills).jsonPath()
                    .getBoolean("success")).isTrue();
        });
        cleanup(() -> employeeApi.setServices(ownerApi(), TestSalon.employeeId(), skillsBefore));

        // 3. Customer finds and books it
        switchUserTo(TestAccounts.customer());
        assertThat(new SalonDetailsPage(page, TestSalon.id()).open().service(name).first()).isVisible();
        long bookingId = bookThroughWizard(name, TestData.name("E2E new service"));
        assertThat(customerSeesStatus(bookingId)).isEqualTo("PENDING");

        // 4. Owner sees the request for the new service
        switchUserTo(TestAccounts.owner());
        assertThat(new SalonBookingsPage(page).open().card(bookingId)).containsText(name);
    }
}
