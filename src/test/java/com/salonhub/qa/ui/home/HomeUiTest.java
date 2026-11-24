package com.salonhub.qa.ui.home;

import com.salonhub.qa.base.BaseUiTest;
import com.salonhub.qa.ui.pages.HomePage;
import com.salonhub.qa.ui.pages.LoginPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Epic("Salon discovery")
@Feature("Home page")
public class HomeUiTest extends BaseUiTest {

    @Test(groups = {"smoke", "ui", "home"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("The home page loads for an anonymous user with the title, a main heading and the Log in link.")
    public void homePageLoadsForAnonymousUser() {
        HomePage home = new HomePage(page).open();

        assertThat(page).hasTitle(Pattern.compile("SalonHub"));
        assertThat(home.heading().first()).isVisible();
        assertThat(home.navbar().loginLink()).isVisible();
    }

    @Test(groups = {"ui", "home"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The Log in link in the header opens the login page.")
    public void headerLoginLinkOpensLoginPage() {
        HomePage home = new HomePage(page).open();

        home.navbar().clickLogin();

        LoginPage login = new LoginPage(page).waitUntilLoaded();
        assertThat(login.emailInput()).isVisible();
        assertThat(page).hasURL(Pattern.compile(".*/login$"));
    }
}
