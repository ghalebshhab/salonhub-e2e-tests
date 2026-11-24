package com.salonhub.qa.ui.pages;

import com.microsoft.playwright.Page;

/** {@code /} — the public landing page. */
public class HomePage extends BasePage<HomePage> {

    public HomePage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return "/";
    }

    @Override
    public HomePage waitUntilLoaded() {
        heading().first().waitFor();
        return this;
    }
}
