package com.salonhub.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

/** {@code /community} — the feed, the composer and the post cards. */
public class CommunityPage extends BasePage<CommunityPage> {

    public CommunityPage(Page page) {
        super(page);
    }

    @Override
    protected String path() {
        return "/community";
    }

    @Override
    public CommunityPage waitUntilLoaded() {
        postInput().waitFor();
        return this;
    }

    public Locator postInput() {
        return page.getByLabel("Post content");
    }

    public Locator publishButton() {
        return page.locator("#community-composer").getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName("Publish post"));
    }

    /** A post card, found by a unique piece of its text. */
    public Locator post(String text) {
        return page.locator("article.community-post-card").filter(new Locator.FilterOptions().setHasText(text));
    }

    @Step("Publish a post")
    public CommunityPage publish(String text) {
        postInput().fill(text);
        publishButton().click();
        post(text).waitFor();
        return this;
    }

    public Locator likeButton(String postText) {
        return post(postText).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Like post"));
    }

    public Locator unlikeButton(String postText) {
        return post(postText).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Unlike post"));
    }

    @Step("Like the post")
    public CommunityPage like(String postText) {
        likeButton(postText).click();
        return this;
    }

    @Step("Open the comments of the post")
    public CommunityPage openComments(String postText) {
        post(postText).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Comment").setExact(true))
                .click();
        return this;
    }

    @Step("Write a comment on the post")
    public CommunityPage comment(String postText, String comment) {
        Locator card = post(postText);
        card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Comment").setExact(true)).click();
        card.getByLabel("Comment", new Locator.GetByLabelOptions().setExact(true)).fill(comment);
        card.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Send comment")).click();
        return this;
    }

    public Locator comments(String postText) {
        return post(postText).getByRole(AriaRole.REGION, new Locator.GetByRoleOptions().setName("Comments"));
    }

    @Step("Delete the post and confirm")
    public CommunityPage delete(String postText) {
        post(postText).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Delete post")).click();
        confirmDialog().root().waitFor();
        confirmDialog().root().locator(".confirm-dialog-actions button").last().click();
        return this;
    }
}
