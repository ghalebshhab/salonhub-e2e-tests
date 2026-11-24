package com.salonhub.qa.ui.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** The react-toastify messages (success and error notices). */
public class Toast {

    private final Page page;

    public Toast(Page page) {
        this.page = page;
    }

    public Locator all() {
        return page.locator(".Toastify__toast");
    }

    public Locator latest() {
        return all().last();
    }

    public Locator error() {
        return page.locator(".Toastify__toast--error").last();
    }

    public Locator success() {
        return page.locator(".Toastify__toast--success").last();
    }
}
