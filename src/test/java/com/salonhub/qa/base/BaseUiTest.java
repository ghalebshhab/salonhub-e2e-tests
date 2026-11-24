package com.salonhub.qa.base;

import com.microsoft.playwright.Page;
import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.client.SessionCache;
import com.salonhub.qa.api.endpoints.BookingApi;
import com.salonhub.qa.api.endpoints.CommunityApi;
import com.salonhub.qa.api.endpoints.ServiceApi;
import com.salonhub.qa.api.flows.BookingFlows;
import com.salonhub.qa.data.TestAccount;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.ui.driver.BrowserSession;
import com.salonhub.qa.ui.driver.PlaywrightFactory;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Parent of all UI tests. Each test method gets a new browser context.
 * Tests set up data through the API (fast and stable) and check the result
 * in the browser. When a test fails, a screenshot and a Playwright trace are
 * attached to the Allure report and the trace is kept in {@code target/traces}.
 * Suites run UI classes with {@code parallel="classes"}, so one class uses one thread.
 */
public abstract class BaseUiTest {

    protected Page page;

    protected final BookingApi bookingApi = new BookingApi();
    protected final ServiceApi serviceApi = new ServiceApi();
    protected final CommunityApi communityApi = new CommunityApi();
    protected final BookingFlows bookingFlows = new BookingFlows();

    private final Deque<Runnable> cleanups = new ArrayDeque<>();

    @BeforeMethod(alwaysRun = true)
    public void openBrowser() {
        page = PlaywrightFactory.start();
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser(ITestResult result) {
        Path trace = null;
        if (!result.isSuccess() && page != null) {
            attachScreenshot();
            trace = Path.of("target", "traces",
                    result.getTestClass().getRealClass().getSimpleName() + "-" + result.getName() + ".zip");
        }
        PlaywrightFactory.stop(trace);
        if (trace != null && Files.exists(trace)) {
            try (InputStream in = Files.newInputStream(trace)) {
                Allure.attachment("Playwright trace (open at trace.playwright.dev)", "application/zip", in,
                        AttachmentOptions.withFileExtension(".zip"));
            } catch (IOException ignored) {
                // The trace stays in target/traces.
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void quitBrowserAndCleanUp() {
        PlaywrightFactory.quit();
        synchronized (cleanups) {
            while (!cleanups.isEmpty()) {
                try {
                    cleanups.pop().run();
                } catch (RuntimeException e) {
                    System.err.println("Clean-up step failed: " + e.getMessage());
                }
            }
        }
    }

    /** Registers an action that removes test data after this class. */
    protected void cleanup(Runnable action) {
        synchronized (cleanups) {
            cleanups.push(action);
        }
    }

    /** Puts the session cookies of {@code account} into this test's browser. */
    protected void signInAs(TestAccount account) {
        BrowserSession.signIn(PlaywrightFactory.context(), account);
    }

    /**
     * Signs the same browser in as another account: removes all cookies and
     * storage, then adds the session of {@code account}. Use it in flows that
     * move between customer, owner and employee.
     */
    protected void switchUserTo(TestAccount account) {
        signOutBrowser();
        signInAs(account);
    }

    /** Removes all cookies and storage, so the browser is an anonymous visitor. */
    protected void signOutBrowser() {
        PlaywrightFactory.context().clearCookies();
        page.evaluate("() => { try { localStorage.clear(); sessionStorage.clear(); } catch (e) {} }");
    }

    protected ApiSession customerApi() {
        return SessionCache.get(TestAccounts.customer());
    }

    protected ApiSession ownerApi() {
        return SessionCache.get(TestAccounts.owner());
    }

    protected ApiSession employeeApi() {
        return SessionCache.get(TestAccounts.salonEmployee());
    }

    private void attachScreenshot() {
        try {
            byte[] png = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            Allure.attachment("Screenshot on failure", "image/png", new ByteArrayInputStream(png),
                    AttachmentOptions.withFileExtension(".png"));
        } catch (RuntimeException ignored) {
            // The page may already be closed.
        }
    }
}
