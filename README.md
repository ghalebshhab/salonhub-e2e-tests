# SalonHub E2E Tests

End-to-end test framework for SalonHub:

- **UI tests** with Playwright for Java and the Page Object Model (POM)
- **API tests** with REST Assured
- **TestNG** for suites, groups, data providers and parallel runs
- **Allure** reports, with a screenshot and a Playwright trace for each failed UI test

## Stack

Java 17 · Maven · Playwright 1.65 · REST Assured 6 · TestNG 7 · AssertJ · Jackson · JSON Schema validation · Allure

## Project structure

```
src/main/java/com/salonhub/qa/
  config/          Config: reads config.properties, test-accounts.properties, -D and env values
  data/            TestAccount, TestAccounts (seeded customer, employee, owner)
  api/client/      ApiClient (base request spec, CSRF header, Allure filter), ApiSession
  api/endpoints/   One class per controller: AuthApi, SalonApi, ...
  api/models/      Request and response records
  ui/driver/       PlaywrightFactory (one browser per thread, one context per test), BrowserSession
  ui/pages/        Page objects: BasePage, LoginPage, HomePage, MyBookingsPage, ...
  ui/components/   Shared parts of pages: Navbar, Toast
  reporting/       AllureRunInfo: environment, categories and CI info for the Allure report
  utils/           Json helper, Eventually (retry a check)
src/test/java/com/salonhub/qa/
  base/            BaseApiTest, BaseUiTest, BaseE2ETest
  api/             API tests, one package per area
  ui/              UI tests, one package per area
  e2e/             Hybrid flows: one booking or post moves through several roles
src/main/resources/allure/categories.json   failure groups in the report
db/                ci-seed.sql (empty CI database), seed-e2e.sql and cleanup-e2e.sql (local database)
ci/seed-ci-db.sh   fills the CI database; makes the BCrypt hashes from the test passwords
.github/workflows/e2e.yml   GitHub Actions: whole stack + tests + Allure report
src/test/resources/
  config.properties                  committed defaults (URLs, browser, timeouts)
  test-accounts.example.properties   template for the local passwords
  schemas/                           JSON schemas for response checks
  suites/                            smoke.xml, api.xml, ui.xml, e2e.xml, regression.xml
```

## Rules

1. **Tests never register users.** Sign-up sends a real email and rejects `.test` emails. Use the seeded accounts in `TestAccounts`.
2. **UI tests sign in with the API** (`signInAs(...)` in `BaseUiTest`). Only the login page tests use the login form.
3. **Tests use page objects and endpoint classes.** Do not put selectors or URLs in test methods.
4. **Each test cleans up the data it makes**, so the suite can run again and again.
5. **Never commit passwords.** `test-accounts.properties` is git-ignored.

## Test data

- **Salon 2, "Omar Test Salon"**, belongs to the tests. Owner: `omar.owner@salonhub.test`. Employee: `rami.employee@salonhub.test`. Tests may change this salon.
- **Salon 1** is a real salon. Tests only read it (for "not yours" checks).
- Every name, title and note that a test makes starts with `E2E`. Each test class cancels, disables or closes what it made.
- `db/seed-e2e.sql` adds the salon 2 employee (without an employee, salon 2 has no free slots).
- `db/cleanup-e2e.sql` deletes finished `E2E` rows (cancelled, rejected and completed bookings with their reviews, disabled services, closed hiring posts). Run it now and then. The API cannot remove a completed booking, so the E2E lifecycle flow leaves one per run until you run this script.

```powershell
Get-Content db\seed-e2e.sql | & "C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p salon_dbv2
```

```powershell
Get-Content db\cleanup-e2e.sql | & "C:\Program Files\MySQL\MySQL Server 9.6\bin\mysql.exe" -uroot -p salon_dbv2
```

The backend's local profile must allow many logins: `salon.auth.rate-limit.login-max` (local default 1000, production 10).

## Setup

1. Start MySQL, the backend (port `8081`) and the frontend dev server (port `5173`).
2. Run `db/seed-e2e.sql` one time.
3. Copy `src/test/resources/test-accounts.example.properties` to `test-accounts.properties` and fill in the passwords.
4. Install the Playwright browsers (one time):

```powershell
mvn exec:java -e "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.classpathScope=test" "-Dexec.args=install chromium"
```

## Run

```powershell
mvn test -Dsuite=smoke
```

```powershell
mvn test -Dsuite=api
```

```powershell
mvn test -Dsuite=ui "-Dui.headless=false" "-Dui.slowMoMs=300"
```

```powershell
mvn test -Dsuite=e2e
```

```powershell
mvn test
```

`mvn test` runs `regression` (all tests). Other options: `-Dui.browser=firefox`, `-Dui.baseUrl=...`, `-Dapi.baseUrl=...`.

Run one class: `mvn test -Dsuite=regression -Dtest=AuthApiTest`.

## Report

```powershell
mvn clean test -Dsuite=smoke
```

```powershell
mvn allure:serve
```

Use `mvn clean` before a run: results of old runs stay in `target/allure-results` and mix into the report.

The report has:

- **Environment**: suite, UI and API URLs, browser, viewport, Java, OS, and where the run was (local or GitHub Actions).
- **Categories**: failures in groups: *Server not reachable*, *UI timeout*, *Rate limit (HTTP 429)*, *Product defect (wrong result)*, *Test defect (code error)*.
- **Steps**: every page-object action and every API call with its request and response.
- A failed UI test has a screenshot and a Playwright trace. The trace also stays in `target/traces`. Open it at https://trace.playwright.dev or with:

```powershell
mvn exec:java -e "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.classpathScope=test" "-Dexec.args=show-trace target/traces/<file>.zip"
```

## CI (GitHub Actions)

`.github/workflows/e2e.yml` runs on a push to `main`, every day at 06:00 (Jordan time), and by hand
(**Actions > E2E tests > Run workflow**, where you pick the suite and the backend and frontend branches).

What the workflow does:

1. Starts MySQL 8.4 with an empty `salon_dbv2`.
2. Checks out `Salon-hub-backend` (`master`) and `Salon-hub-frontend` (`main`).
3. Builds the backend and starts it with the `local` profile on port 8081 (emails go to the log, a new JWT secret for each run). Flyway makes the schema.
4. Runs `ci/seed-ci-db.sh`, which loads `db/ci-seed.sql`. All people and salons in it are made up. Salon 1 is a synthetic "E2E Other Salon", not the real salon.
5. Builds the frontend and serves it with `vite preview` on port 5173. The proxy is the same, and `VITE_API_BASE_URL=/api`, so the build never calls the production API. The dev server is too slow on a fresh runner.
6. Runs the suite, then uploads the **allure-report** and **test-output** artifacts (results, traces, surefire reports, backend and frontend logs). The run summary shows the counts.

No secrets are needed. The test users exist only in the new CI database of
each run, so the workflow makes a random password for each of them and hides it
in the log. Your local test passwords never go to GitHub.

Optional repository secrets:

| Secret | Value |
|---|---|
| `E2E_CUSTOMER_PASSWORD`, `E2E_EMPLOYEE_PASSWORD`, `E2E_OWNER_PASSWORD`, `E2E_SALON2EMPLOYEE_PASSWORD` | Fixed test passwords, used instead of the random ones. |
| `REPOS_TOKEN` | A token with read access to the backend and frontend repositories. Needed only if they are private. |

The workflow tests the pushed backend and frontend code. Changes that the tests need must be on `master` / `main` first.

To open the downloaded report, unzip it and run `allure open <folder>`, or serve the folder with any local web server.

## Groups

`smoke`, `api`, `ui`, `e2e`, `auth`, `salons`, `services`, `bookings`, `reviews`, `hiring`, `community`, `notifications`, `owner`, `home`, `negative`, `security`, `rate-limit`, `known-bug`. The `smoke` suite runs only the `smoke` group.

## Known bugs

Put a test that fails because of a known, not yet fixed backend bug in the `known-bug` group, and list it here. There are none now.

Fixed bugs that the tests found:

| Test | Bug | Fix |
|---|---|---|
| `CommunityApiTest.friendRequestReachesTheOtherUser` | Every friend request failed with "Something went wrong": `user_notification.salon_id` / `salon_name` were NOT NULL, but community notifications have no salon. | Backend migration `V4__community_notifications_without_salon.sql` |

## API coverage

| Area | Test class |
|---|---|
| Login, logout, roles, cookies | `AuthApiTest` |
| CSRF header, refresh rotation, forged token, rate limit 429 | `AuthSecurityApiTest` |
| Public salons | `SalonPublicApiTest` |
| Salon services (create, update, disable, validation, ownership) | `SalonServiceApiTest` |
| Bookings (availability, create, accept, reject, cancel, payment, tracking, roles) | `BookingApiTest` |
| Booking rules (no two bookings at the same time, owner cannot book at own salon) | `BookingRulesApiTest` |
| CliQ payments (settings, alias, owner marks paid, role checks, cash/CliQ switch) | `CliqPaymentApiTest` |
| Reviews (public list, summary, rules) | `ReviewApiTest` |
| Hiring posts (open, close, Free plan limit, validation, apply) | `HiringApiTest` |
| Community (posts, likes, comments, follows, friends) | `CommunityApiTest` |
| Notifications (booking events, read all) | `NotificationApiTest` |
| Owner dashboard, plan, payments, closures, team, role checks | `OwnerApiTest` |

CliQ tests turn CliQ on at the test salon through `CliqSetup`. The first class
saves the settings, and the last class puts them back.

## UI coverage

UI tests set up data through the API and check the result in the browser.
They sign in with the API session cookies, except the login page tests.

| Area | Test class |
|---|---|
| Login form, wrong password, empty form, redirect, log out, owner page guard | `LoginUiTest` |
| Home page | `HomeUiTest` |
| Salon page (services, "Sign in to book", "Book appointment", "Edit salon") | `SalonDetailsUiTest` |
| Booking wizard, My bookings (status, cancel, keep booking, confirmed) | `CustomerBookingUiTest` |
| Owner accepts and rejects bookings, employee sees assigned booking, role guard | `OwnerBookingsUiTest` |
| Owner adds and disables services | `OwnerServicesUiTest` |
| Community (publish, like, comment, delete) | `CommunityUiTest` |
| Notifications (unread item, mark all as read) | `NotificationsUiTest` |
| CliQ (customer sees the alias and switches to cash, owner marks "Paid by CliQ") | `CliqPaymentUiTest` |

## E2E flows

A flow uses one browser and moves between roles with `switchUserTo(...)`. It checks
each step in the UI and in the API. Set-up that is not under test uses the API.

| Flow | Test |
|---|---|
| Customer books in the wizard, owner accepts with Rami, Rami sees it, customer sees "Confirmed" and a notification, owner marks it completed, customer writes a 5-star review, an anonymous visitor sees the review | `BookingLifecycleE2ETest` |
| Customer books, owner rejects with a reason, customer sees "Not accepted" and gets `BOOKING_REJECTED` | `BookingOutcomesE2ETest` |
| Owner accepts, customer cancels the confirmed booking, owner sees `CANCELLED` and gets `BOOKING_CANCELLED` | `BookingOutcomesE2ETest` |
| Owner adds a service, gives it to Rami (API), customer finds it on the salon page and books it, owner sees the request | `NewServiceE2ETest` |
| Customer posts, owner likes and comments, customer sees the comment and the like | `CommunityE2ETest` |
