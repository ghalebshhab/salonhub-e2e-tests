package com.salonhub.qa.e2e;

import com.salonhub.qa.base.BaseE2ETest;
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

@Epic("End-to-end flows")
@Feature("Community between users")
public class CommunityE2ETest extends BaseE2ETest {

    private List<Long> postIds(String text) {
        return communityApi.feed(customerApi()).jsonPath()
                .getList("data.content.findAll { it.content == '" + text + "' }.id", Long.class);
    }

    @Test(groups = {"e2e", "community"})
    @Severity(SeverityLevel.NORMAL)
    @Description("Customer publishes a post -> the owner sees it, likes it and comments -> "
            + "the customer sees the owner's comment and one like.")
    public void ownerRespondsToCustomerPost() {
        String text = TestData.name("Community flow post");
        String reply = TestData.name("Owner reply");
        cleanup(() -> postIds(text).forEach(id -> communityApi.deletePost(customerApi(), id)));

        signInAs(TestAccounts.customer());
        new CommunityPage(page).open().publish(text);

        switchUserTo(TestAccounts.owner());
        CommunityPage ownerView = new CommunityPage(page).open();
        assertThat(ownerView.post(text)).isVisible();
        ownerView.like(text).comment(text, reply);
        assertThat(ownerView.unlikeButton(text)).isVisible();

        switchUserTo(TestAccounts.customer());
        CommunityPage customerView = new CommunityPage(page).open();
        Eventually.check(() -> {
            List<Long> ids = postIds(text);
            assertThat(ids).hasSize(1);
            assertThat(communityApi.getPost(customerApi(), ids.get(0)).jsonPath().getInt("data.likeCount"))
                    .isEqualTo(1);
        });
        customerView.openComments(text);
        assertThat(customerView.comments(text)).containsText(reply);
    }
}
