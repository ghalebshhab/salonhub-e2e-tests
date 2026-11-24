package com.salonhub.qa.ui.salons;

import com.salonhub.qa.base.BaseUiTest;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.data.TestSalon;
import com.salonhub.qa.ui.pages.LoginPage;
import com.salonhub.qa.ui.pages.SalonDetailsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Epic("Salon discovery")
@Feature("Salon page")
public class SalonDetailsUiTest extends BaseUiTest {

    private List<String> activeServiceNames() {
        return serviceApi.activeServicesOf(TestSalon.id()).jsonPath().getList("data.name", String.class);
    }

    @Test(groups = {"smoke", "ui", "salons"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("The salon page shows the salon name and every active service.")
    public void salonPageShowsNameAndServices() {
        String salonName = new com.salonhub.qa.api.endpoints.SalonApi().getSalon(TestSalon.id()).jsonPath()
                .getString("data.name");

        List<String> before = activeServiceNames();
        SalonDetailsPage salon = new SalonDetailsPage(page, TestSalon.id()).open();
        assertThat(salon.heading().first()).containsText(salonName);
        // Parallel tests add and remove temporary services. Check only the
        // services that were active both before and after the page loaded.
        List<String> stable = new java.util.ArrayList<>(before);
        stable.retainAll(activeServiceNames());

        for (String service : stable) {
            assertThat(salon.service(service).first()).isVisible();
        }
    }

    @Test(groups = {"ui", "salons"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("An anonymous visitor sees 'Sign in to book', and it opens the login page.")
    public void anonymousVisitorIsAskedToSignInToBook() {
        SalonDetailsPage salon = new SalonDetailsPage(page, TestSalon.id()).open();

        assertThat(salon.bookLink()).hasCount(0);
        salon.signInToBookLink().click();

        new LoginPage(page).waitUntilLoaded();
        assertThat(page).hasURL(Pattern.compile(".*/login.*"));
    }

    @Test(groups = {"ui", "salons"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A signed-in customer sees 'Book appointment', and it opens the booking wizard.")
    public void customerCanStartBookingFromSalonPage() {
        signInAs(TestAccounts.customer());

        var wizard = new SalonDetailsPage(page, TestSalon.id()).open().startBooking();

        assertThat(page).hasURL(Pattern.compile(".*/salons/" + TestSalon.id() + "/book.*"));
        assertThat(wizard.stepTitle().first()).containsText("What would you like to book?");
    }

    @Test(groups = {"ui", "salons"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The owner sees 'Edit salon' on their own salon instead of a booking button.")
    public void ownerSeesEditSalonOnOwnSalon() {
        signInAs(TestAccounts.owner());

        SalonDetailsPage salon = new SalonDetailsPage(page, TestSalon.id()).open();

        assertThat(salon.editSalonLink()).isVisible();
        assertThat(salon.bookLink()).hasCount(0);
    }
}
