package com.salonhub.qa.ui.owner;

import com.salonhub.qa.base.BaseUiTest;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.data.TestSalon;
import com.salonhub.qa.ui.pages.owner.ServicesPage;
import com.salonhub.qa.utils.Eventually;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Salon management")
@Feature("Services (UI)")
public class OwnerServicesUiTest extends BaseUiTest {

    private List<Long> ownServiceIds(String name) {
        return serviceApi.mySalonServices(ownerApi()).jsonPath()
                .getList("data.findAll { it.name == '" + name + "' }.id", Long.class);
    }

    private void disableByName(String name) {
        for (long id : ownServiceIds(name)) {
            serviceApi.disable(ownerApi(), id);
        }
    }

    private List<String> publicServiceNames() {
        return serviceApi.activeServicesOf(TestSalon.id()).jsonPath().getList("data.name", String.class);
    }

    @Test(groups = {"smoke", "ui", "owner", "services"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The owner adds a service in the UI; it shows on the services page and in the public menu.")
    public void ownerAddsService() {
        String name = TestData.name("UI service");
        cleanup(() -> disableByName(name));
        signInAs(TestAccounts.owner());

        ServicesPage services = new ServicesPage(page).open().addService()
                .create(name, "Created by the UI test.", "12", "30");

        assertThat(services.serviceCard(name)).isVisible();
        assertThat(ownServiceIds(name)).hasSize(1);
        assertThat(publicServiceNames()).contains(name);
    }

    @Test(groups = {"ui", "owner", "services"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The owner disables a service in the UI; it leaves the public menu.")
    public void ownerDisablesService() {
        String name = TestData.name("UI disable");
        long id = serviceApi.create(ownerApi(),
                com.salonhub.qa.api.models.ServiceRequest.of(name, 10, 20)).jsonPath().getLong("data.id");
        cleanup(() -> serviceApi.disable(ownerApi(), id));
        signInAs(TestAccounts.owner());

        new ServicesPage(page).open().disable(name);

        Eventually.check(() -> assertThat(publicServiceNames()).doesNotContain(name));
    }
}
