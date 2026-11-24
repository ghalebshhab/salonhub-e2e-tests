package com.salonhub.qa.ui.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

/** The app's shared "Are you sure?" dialog ({@code role="alertdialog"}). */
public class ConfirmDialog {

    private final Locator root;

    public ConfirmDialog(Page page) {
        this.root = page.getByRole(AriaRole.ALERTDIALOG);
    }

    public Locator root() {
        return root;
    }

    public Locator title() {
        return root.getByRole(AriaRole.HEADING);
    }

    /** Clicks the confirm button, whose text is the action, for example "Cancel booking". */
    @Step("Confirm the dialog with \"{confirmText}\"")
    public void confirm(String confirmText) {
        root.locator(".confirm-dialog-actions")
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(confirmText).setExact(true))
                .click();
        root.waitFor(new Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
    }

    @Step("Close the dialog without action")
    public void dismiss(String cancelText) {
        root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(cancelText).setExact(true)).click();
    }
}
