package com.salonhub.qa.base;

import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.client.SessionCache;
import com.salonhub.qa.api.endpoints.AuthApi;
import com.salonhub.qa.api.endpoints.BookingApi;
import com.salonhub.qa.api.endpoints.CommunityApi;
import com.salonhub.qa.api.endpoints.HiringApi;
import com.salonhub.qa.api.endpoints.NotificationApi;
import com.salonhub.qa.api.endpoints.OwnerApi;
import com.salonhub.qa.api.endpoints.ReviewApi;
import com.salonhub.qa.api.endpoints.SalonApi;
import com.salonhub.qa.api.endpoints.ServiceApi;
import com.salonhub.qa.api.flows.BookingFlows;
import com.salonhub.qa.data.TestAccounts;
import io.restassured.RestAssured;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeSuite;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Parent of all API tests: endpoint objects, cached sessions and a clean-up list.
 * A test that creates data calls {@link #cleanup(Runnable)}; the actions run
 * after the class, newest first, even when a test fails.
 */
public abstract class BaseApiTest {

    protected final AuthApi authApi = new AuthApi();
    protected final SalonApi salonApi = new SalonApi();
    protected final ServiceApi serviceApi = new ServiceApi();
    protected final BookingApi bookingApi = new BookingApi();
    protected final ReviewApi reviewApi = new ReviewApi();
    protected final HiringApi hiringApi = new HiringApi();
    protected final CommunityApi communityApi = new CommunityApi();
    protected final NotificationApi notificationApi = new NotificationApi();
    protected final OwnerApi ownerApi = new OwnerApi();
    protected final BookingFlows bookingFlows = new BookingFlows();

    private final Deque<Runnable> cleanups = new ArrayDeque<>();

    @BeforeSuite(alwaysRun = true)
    public void configureRestAssured() {
        // Print the full request and response only when an assertion fails.
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    protected ApiSession customer() {
        return SessionCache.get(TestAccounts.customer());
    }

    protected ApiSession owner() {
        return SessionCache.get(TestAccounts.owner());
    }

    protected ApiSession employee() {
        return SessionCache.get(TestAccounts.salonEmployee());
    }

    /** Registers an action that removes test data after this class. */
    protected void cleanup(Runnable action) {
        synchronized (cleanups) {
            cleanups.push(action);
        }
    }

    @AfterClass(alwaysRun = true)
    public void runCleanups() {
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
}
