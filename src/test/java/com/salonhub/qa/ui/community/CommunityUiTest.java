package com.salonhub.qa.ui.community;

import com.salonhub.qa.api.models.PostRequest;
import com.salonhub.qa.base.BaseUiTest;
import com.salonhub.qa.data.TestAccounts;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.ui.pages.CommunityPage;
import com.salonhub.qa.utils.Eventually;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Community")
@Feature("Feed (UI)")
public class CommunityUiTest extends BaseUiTest {

    private List<Long> customerPostIds(String text) {
        return communityApi.feed(customerApi()).jsonPath()
                .getList("data.content.findAll { it.content == '" + text + "' }.id", Long.class);
    }

    private void deletePostsWithText(String text) {
        for (long id : customerPostIds(text)) {
            communityApi.deletePost(customerApi(), id);
        }
    }

    private long createPostByApi(String text) {
        long id = communityApi.createPost(customerApi(), PostRequest.text(text)).jsonPath().getLong("data.id");
        cleanup(() -> communityApi.deletePost(customerApi(), id));
        return id;
    }

    @Test(groups = {"smoke", "ui", "community"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A customer publishes a post from the composer and it shows at the top of the feed.")
    public void customerPublishesPost() {
        String text = TestData.name("UI post");
        cleanup(() -> deletePostsWithText(text));
        signInAs(TestAccounts.customer());

        CommunityPage community = new CommunityPage(page).open().publish(text);

        assertThat(community.post(text)).isVisible();
        Eventually.check(() -> assertThat(customerPostIds(text)).hasSize(1));
    }

    @Test(groups = {"ui", "community"})
    @Severity(SeverityLevel.NORMAL)
    @Description("Like turns into Unlike, and the like is saved.")
    public void customerLikesPost() {
        String text = TestData.name("UI like");
        long id = createPostByApi(text);
        signInAs(TestAccounts.customer());

        CommunityPage community = new CommunityPage(page).open().like(text);

        assertThat(community.unlikeButton(text)).isVisible();
        Eventually.check(() -> assertThat(communityApi.getPost(customerApi(), id).jsonPath()
                .getInt("data.likeCount")).isEqualTo(1));
    }

    @Test(groups = {"ui", "community"})
    @Severity(SeverityLevel.NORMAL)
    @Description("A comment written in the UI shows under the post.")
    public void customerCommentsOnPost() {
        String text = TestData.name("UI comment post");
        createPostByApi(text);
        String comment = TestData.name("UI comment");
        signInAs(TestAccounts.customer());

        CommunityPage community = new CommunityPage(page).open().comment(text, comment);

        assertThat(community.comments(text)).containsText(comment);
    }

    @Test(groups = {"ui", "community"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The author deletes their post after the confirm dialog; it leaves the feed.")
    public void customerDeletesPost() {
        String text = TestData.name("UI delete");
        createPostByApi(text);
        signInAs(TestAccounts.customer());

        CommunityPage community = new CommunityPage(page).open().delete(text);

        assertThat(community.post(text)).hasCount(0);
        Eventually.check(() -> assertThat(customerPostIds(text)).isEmpty());
    }
}
