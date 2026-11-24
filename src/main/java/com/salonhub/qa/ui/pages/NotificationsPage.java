package com.salonhub.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

/** {@code /notifications}. */
public class NotificationsPage extends BasePage<NotificationsPage> {

    public NotificationsPage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return "/notifications";
    }

    @Override
    public NotificationsPage waitUntilLoaded() {
        page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Notifications").setLevel(1)).waitFor();
        return this;
    }

    public Locator items() {
        return page.locator(".notifications-page-item");
    }

    public Locator unreadItems() {
        return page.locator(".notifications-page-item.is-unread");
    }

    public Locator itemWithText(String text) {
        return items().filter(new Locator.FilterOptions().setHasText(text));
    }

    public Locator markAllReadButton() {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Mark all as read"));
    }

    @Step("Click Mark all as read")
    public NotificationsPage markAllRead() {
        markAllReadButton().click();
        return this;
    }
}
