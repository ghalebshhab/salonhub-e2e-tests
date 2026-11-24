package com.salonhub.qa.api.payments;

import com.salonhub.qa.api.flows.CliqSetup;
import com.salonhub.qa.api.models.BookingAction;
import com.salonhub.qa.base.BaseApiTest;
import com.salonhub.qa.data.TestSalon;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.salonhub.qa.api.ApiAssertions.assertBusinessError;
import static com.salonhub.qa.api.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * CliQ (Jordan instant transfer): the owner turns it on with an alias, the
 * customer chooses it and sees the alias, and only the owner marks the
 * booking as paid. The test salon's settings are put back after the class.
 */
@Epic("Payments")
@Feature("CliQ payments")
public class CliqPaymentApiTest extends BaseApiTest {

    private static final String ALIAS = CliqSetup.ALIAS;
    private static final String NAME = CliqSetup.NAME;

    @BeforeClass(alwaysRun = true)
    public void turnOnCliq() {
        CliqSetup.acquire(owner());
    }

    @AfterClass(alwaysRun = true)
    public void restoreSettings() {
        CliqSetup.release(owner());
    }

    private long booking(String method) {
        long id = bookingFlows.createPending(customer(), method);
        cleanup(() -> bookingFlows.cancelQuietly(customer(), id));
        return id;
    }

    private void accept(long bookingId) {
        assertSuccess(bookingApi.accept(owner(), bookingId,
                new BookingAction(TestSalon.PREFIX + "accepted", TestSalon.employeeId())));
    }

    /** The payment summary of one booking in the customer's list. */
    private Map<String, Object> summaryOf(long bookingId) {
        List<Map<String, Object>> payments = assertSuccess(bookingApi.myBookings(customer()))
                .getList("data.findAll { it.id == " + bookingId + " }.payment");
        assertThat(payments).as("booking %s in the customer's list", bookingId).hasSize(1);
        return payments.get(0);
    }

    // ---------- Settings ----------

    @Test(groups = {"api", "owner"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("With CliQ on, the public payment options list CLIQ with the alias and name.")
    public void paymentOptionsShowTheCliqAlias() {
        JsonPath json = assertSuccess(ownerApi.paymentOptions(customer(), TestSalon.id()));

        assertThat(json.getList("data.methods", String.class)).contains("CASH", "CLIQ");
        assertThat(json.getString("data.defaultMethod")).isEqualTo("CASH");
        assertThat(json.getString("data.cliqAlias")).isEqualTo(ALIAS);
        assertThat(json.getString("data.cliqName")).isEqualTo(NAME);
    }

    @Test(groups = {"api", "owner", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("CliQ cannot be turned on without an alias, and the alias must be valid.")
    public void cliqNeedsAValidAlias() {
        assertBusinessError(ownerApi.updatePaymentSettings(owner(), TestSalon.id(),
                CliqSetup.original().withCliq(true, "  ", null)), "Enter your CliQ alias");
        assertBusinessError(ownerApi.updatePaymentSettings(owner(), TestSalon.id(),
                CliqSetup.original().withCliq(true, "bad alias!", null)), "The CliQ alias must be");
    }

    // ---------- Booking with CliQ ----------

    @Test(groups = {"smoke", "api", "bookings"})
    @Severity(SeverityLevel.BLOCKER)
    @Description("Customer books with CliQ; the owner accepts and marks it paid by CliQ; the payment is PAID with a receipt.")
    public void ownerMarksCliqBookingAsPaid() {
        long id = booking("CLIQ");
        assertThat(summaryOf(id)).containsEntry("method", "CLIQ").containsEntry("status", "UNPAID")
                .containsEntry("cliqAlias", ALIAS).containsEntry("cliqName", NAME);

        assertBusinessError(bookingApi.confirmCliq(owner(), id, null),
                "after the salon accepts the booking");

        accept(id);
        JsonPath paid = assertSuccess(bookingApi.confirmCliq(owner(), id, "E2E-REF-123"));
        assertThat(paid.getString("message")).isEqualTo("CliQ payment recorded");
        assertThat(paid.getString("data.method")).isEqualTo("CLIQ");
        assertThat(paid.getString("data.status")).isEqualTo("PAID");
        assertThat(paid.getString("data.receiptNumber")).isNotBlank();
        assertThat(paid.getList("data.transactions.type", String.class)).contains("CLIQ_RECEIVED");

        JsonPath again = assertSuccess(bookingApi.confirmCliq(owner(), id, null));
        assertThat(again.getString("message")).isEqualTo("CliQ payment was already recorded");
        assertThat(again.getString("data.receiptNumber")).isEqualTo(paid.getString("data.receiptNumber"));

        JsonPath customerView = assertSuccess(bookingApi.payment(customer(), id));
        assertThat(customerView.getString("data.status")).isEqualTo("PAID");
    }

    @Test(groups = {"api", "bookings", "negative", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The employee and the customer cannot mark a CliQ transfer as paid; only the owner can.")
    public void onlyTheOwnerConfirmsCliq() {
        long id = booking("CLIQ");
        accept(id);

        assertBusinessError(bookingApi.confirmCliq(employee(), id, null),
                "Only the salon owner can confirm a CliQ transfer");
        JsonPath customerTry = bookingApi.confirmCliq(customer(), id, null).jsonPath();
        assertThat(customerTry.getBoolean("success")).isFalse();
        assertThat(summaryOf(id)).containsEntry("status", "UNPAID");
    }

    @Test(groups = {"api", "bookings", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("A cash booking cannot be marked as paid by CliQ.")
    public void cashBookingIsNotPaidByCliq() {
        long id = booking("CASH");
        accept(id);

        assertBusinessError(bookingApi.confirmCliq(owner(), id, null),
                "The customer did not choose CliQ for this booking");
    }

    // ---------- Switching method ----------

    @Test(groups = {"api", "bookings"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The customer switches an unpaid booking from cash to CliQ and back.")
    public void customerSwitchesBetweenCashAndCliq() {
        long id = booking("CASH");

        JsonPath toCliq = assertSuccess(bookingApi.changePaymentMethod(customer(), id, "CLIQ"));
        assertThat(toCliq.getString("message")).isEqualTo("You will pay by CliQ");
        assertThat(summaryOf(id)).containsEntry("method", "CLIQ").containsEntry("cliqAlias", ALIAS);

        accept(id);
        JsonPath toCash = assertSuccess(bookingApi.changePaymentMethod(customer(), id, "CASH"));
        assertThat(toCash.getString("message")).isEqualTo("You will pay in cash at the salon");
        Map<String, Object> summary = summaryOf(id);
        assertThat(summary).containsEntry("method", "CASH");
        assertThat(summary.get("cliqAlias")).isNull();
    }

    @Test(groups = {"api", "bookings", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("Only cash or CliQ can be chosen, and the owner cannot change the customer's method.")
    public void methodChangeRules() {
        long id = booking("CASH");

        assertBusinessError(bookingApi.changePaymentMethod(customer(), id, "CARD_PREPAID"),
                "Choose cash or CliQ");
        JsonPath ownerTry = bookingApi.changePaymentMethod(owner(), id, "CLIQ").jsonPath();
        assertThat(ownerTry.getBoolean("success")).isFalse();
        assertThat(summaryOf(id)).containsEntry("method", "CASH");
    }
}
