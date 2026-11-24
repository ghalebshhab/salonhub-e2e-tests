package com.salonhub.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import java.time.LocalDate;

/**
 * {@code /salons/{salonId}/book} — the 4-step booking wizard:
 * 1 services, 2 professional, 3 day and time, 4 details and send.
 */
public class BookingWizardPage extends BasePage<BookingWizardPage> {

    private final long salonId;

    public BookingWizardPage(Page page, long salonId) {
        super(page);
        this.salonId = salonId;
    }

    @Override
    protected String path() {
        return "/salons/" + salonId + "/book";
    }

    @Override
    public BookingWizardPage waitUntilLoaded() {
        page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Book an appointment")).waitFor();
        return this;
    }

    // ---------- Common ----------

    public Locator continueButton() {
        return page.locator(".bk-actions").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Continue"));
    }

    public Locator sendRequestButton() {
        return page.locator(".bk-actions").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Send request"));
    }

    public Locator summary() {
        return page.getByRole(AriaRole.COMPLEMENTARY, new Page.GetByRoleOptions().setName("Booking summary"));
    }

    public Locator stepTitle() {
        return page.locator(".bk-step-title");
    }

    @Step("Click Continue")
    public BookingWizardPage next() {
        continueButton().click();
        return this;
    }

    // ---------- Step 1: services ----------

    public Locator serviceCard(String name) {
        return page.locator("label.bk-service").filter(new Locator.FilterOptions().setHasText(name));
    }

    @Step("Choose the service {name}")
    public BookingWizardPage chooseService(String name) {
        serviceCard(name).click();
        return this;
    }

    // ---------- Step 2: professional ----------

    @Step("Let the salon choose the professional")
    public BookingWizardPage letSalonAssign() {
        page.locator(".bk-pro-grid label").filter(new Locator.FilterOptions().setHasText("Let the salon assign")).click();
        return this;
    }

    @Step("Choose the professional {name}")
    public BookingWizardPage chooseProfessional(String name) {
        page.locator(".bk-pro-grid label").filter(new Locator.FilterOptions().setHasText(name)).click();
        return this;
    }

    // ---------- Step 3: day and time ----------

    public Locator timeSlots() {
        return page.locator("button.bk-slot[role='radio']");
    }

    @Step("Choose the day {date}")
    public BookingWizardPage chooseDate(LocalDate date) {
        page.getByLabel("Choose another date").fill(date.toString());
        return this;
    }

    /** Clicks the first free time and returns it as {@code HH:mm}. */
    @Step("Choose the first free time")
    public String chooseFirstFreeTime() {
        Locator slot = timeSlots().first();
        slot.waitFor();
        String time = slot.locator("strong").innerText().trim();
        slot.click();
        return time;
    }

    // ---------- Step 4: details ----------

    public Locator areaInput() {
        return page.getByLabel("Where are you coming from?");
    }

    public Locator phoneInput() {
        return page.locator("input[type='tel']");
    }

    public Locator noteInput() {
        return page.locator(".bk-form textarea");
    }

    @Step("Fill the visit details")
    public BookingWizardPage fillDetails(String area, String phone, String note) {
        areaInput().fill(area);
        if (phone != null) {
            phoneInput().fill(phone);
        }
        if (note != null) {
            noteInput().fill(note);
        }
        return this;
    }

    @Step("Click Send request")
    public BookingSentPage send() {
        sendRequestButton().click();
        return new BookingSentPage(page).waitUntilLoaded();
    }
}
