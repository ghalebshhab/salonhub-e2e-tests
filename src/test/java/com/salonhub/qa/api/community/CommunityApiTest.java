package com.salonhub.qa.api.community;

import com.salonhub.qa.api.models.PostRequest;
import com.salonhub.qa.base.BaseApiTest;
import com.salonhub.qa.data.TestData;
import com.salonhub.qa.data.TestSalon;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static com.salonhub.qa.api.ApiAssertions.assertBusinessError;
import static com.salonhub.qa.api.ApiAssertions.assertStatus;
import static com.salonhub.qa.api.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Community")
@Feature("Community API")
public class CommunityApiTest extends BaseApiTest {

    private long customerPost() {
        long id = assertSuccess(communityApi.createPost(customer(), PostRequest.text(TestData.sentence())))
                .getLong("data.id");
        cleanup(() -> communityApi.deletePost(customer(), id));
        return id;
    }

    // ---------- Posts ----------

    @Test(groups = {"smoke", "api", "community"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("A user creates a post; it belongs to them and shows in the feed.")
    public void userCreatesPostAndItIsInTheFeed() {
        String text = TestData.sentence();

        JsonPath json = assertSuccess(communityApi.createPost(customer(), PostRequest.text(text)));
        long id = json.getLong("data.id");
        cleanup(() -> communityApi.deletePost(customer(), id));

        assertThat(json.getString("data.content")).isEqualTo(text);
        assertThat(json.getLong("data.author.id")).isEqualTo(customer().userId());
        assertThat(json.getBoolean("data.ownedByMe")).isTrue();
        assertThat(json.getInt("data.likeCount")).isZero();
        assertThat(assertSuccess(communityApi.feed(owner())).getList("data.content.id", Long.class)).contains(id);
    }

    @Test(groups = {"api", "community"})
    @Severity(SeverityLevel.NORMAL)
    @Description("The author edits a post, then deletes it; after that the post is not found.")
    public void authorEditsAndDeletesPost() {
        long id = customerPost();
        String edited = TestData.sentence();

        assertThat(assertSuccess(communityApi.updatePost(customer(), id, PostRequest.text(edited)))
                .getString("data.content")).isEqualTo(edited);
        assertSuccess(communityApi.deletePost(customer(), id));
        assertBusinessError(communityApi.getPost(customer(), id), "not found");
    }

    @DataProvider(name = "invalidPosts")
    public Object[][] invalidPosts() {
        return new Object[][]{
                {"empty", "", "content is required"},
                {"blank", "   ", "content is required"},
                {"over 5000 characters", TestData.text(5001), "cannot exceed 5000"},
        };
    }

    @Test(groups = {"api", "community", "negative"}, dataProvider = "invalidPosts")
    @Severity(SeverityLevel.NORMAL)
    @Description("A post must have text of 1 to 5000 characters.")
    public void invalidPostIsRejected(String scenario, String content, String expectedMessage) {
        var response = communityApi.createPost(customer(), PostRequest.text(content));
        if (response.jsonPath().getBoolean("success")) {
            communityApi.deletePost(customer(), response.jsonPath().getLong("data.id"));
        }
        assertBusinessError(response, expectedMessage);
    }

    @Test(groups = {"api", "community"})
    @Severity(SeverityLevel.NORMAL)
    @Description("A post of exactly 5000 characters is accepted (upper limit).")
    public void postAtMaximumLengthIsAccepted() {
        JsonPath json = assertSuccess(communityApi.createPost(customer(), PostRequest.text(TestData.text(5000))));
        long id = json.getLong("data.id");
        cleanup(() -> communityApi.deletePost(customer(), id));

        assertThat(json.getString("data.content")).hasSize(5000);
    }

    @Test(groups = {"api", "community", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("Another user cannot edit or delete someone else's post.")
    public void otherUserCannotEditOrDeletePost() {
        long id = customerPost();

        assertBusinessError(communityApi.updatePost(owner(), id, PostRequest.text("hacked")), "not found");
        assertBusinessError(communityApi.deletePost(owner(), id), "not found");
        assertThat(assertSuccess(communityApi.getPost(customer(), id)).getString("data.content"))
                .isNotEqualTo("hacked");
    }

    // ---------- Likes and comments ----------

    @Test(groups = {"api", "community"})
    @Severity(SeverityLevel.NORMAL)
    @Description("A like counts once, even when sent two times; unlike removes it.")
    public void likeIsIdempotentAndUnlikeRemovesIt() {
        long id = customerPost();

        assertSuccess(communityApi.like(owner(), id));
        JsonPath twice = assertSuccess(communityApi.like(owner(), id));

        assertThat(twice.getLong("data.likeCount")).isEqualTo(1);
        assertThat(twice.getBoolean("data.likedByMe")).isTrue();
        assertThat(assertSuccess(communityApi.likes(customer(), id)).getList("data.user.id", Long.class))
                .containsExactly(owner().userId());

        assertThat(assertSuccess(communityApi.unlike(owner(), id)).getLong("data.likeCount")).isZero();
    }

    @Test(groups = {"api", "community"})
    @Severity(SeverityLevel.NORMAL)
    @Description("A user comments on a post; the comment is listed and its author can delete it.")
    public void userCommentsAndDeletesComment() {
        long postId = customerPost();
        String text = TestData.sentence();

        JsonPath comment = assertSuccess(communityApi.comment(owner(), postId, text));
        long commentId = comment.getLong("data.id");

        assertThat(comment.getString("data.content")).isEqualTo(text);
        assertThat(assertSuccess(communityApi.comments(customer(), postId)).getList("data.content.id", Long.class))
                .contains(commentId);
        assertSuccess(communityApi.deleteComment(owner(), postId, commentId));
        assertThat(assertSuccess(communityApi.comments(customer(), postId)).getList("data.content.id", Long.class))
                .doesNotContain(commentId);
    }

    @Test(groups = {"api", "community", "negative"})
    @Severity(SeverityLevel.NORMAL)
    @Description("An empty comment is rejected.")
    public void emptyCommentIsRejected() {
        long postId = customerPost();

        assertThat(communityApi.comment(owner(), postId, "").jsonPath().getBoolean("success")).isFalse();
    }

    // ---------- Follows and friends ----------

    @Test(groups = {"api", "community"})
    @Severity(SeverityLevel.NORMAL)
    @Description("A customer follows the test salon, sees it in the list, then unfollows it.")
    public void customerFollowsAndUnfollowsSalon() {
        cleanup(() -> communityApi.unfollow(customer(), TestSalon.id()));

        assertSuccess(communityApi.follow(customer(), TestSalon.id()));
        assertThat(assertSuccess(communityApi.followedSalons(customer())).getList("data.salon.id", Long.class))
                .contains(TestSalon.id());

        assertSuccess(communityApi.unfollow(customer(), TestSalon.id()));
        assertThat(assertSuccess(communityApi.followedSalons(customer())).getList("data.salon.id", Long.class))
                .doesNotContain(TestSalon.id());
    }

    @Test(groups = {"api", "community", "negative"})
    @Severity(SeverityLevel.MINOR)
    @Description("A user cannot send a friend request to themself.")
    public void selfFriendRequestIsRejected() {
        assertBusinessError(communityApi.sendFriendRequest(customer(), customer().userId()), "cannot add yourself");
    }

    /** Declines every pending request from the customer to the owner. */
    private void removeFriendRequestsFromCustomerToOwner() {
        List<Long> ids = communityApi.incomingFriendRequests(owner()).jsonPath()
                .getList("data.findAll { it.user.id == " + customer().userId() + " }.id", Long.class);
        ids.forEach(id -> communityApi.deleteFriendRequest(owner(), id));
    }

    @Test(groups = {"api", "community"})
    @Severity(SeverityLevel.CRITICAL)
    @Issue("friend-request-notification-salon-id")
    @Description("A friend request reaches the other user, and a second request is refused. "
            + "Regression test: it failed when user_notification.salon_id was NOT NULL (fixed by migration V4).")
    public void friendRequestReachesTheOtherUser() {
        removeFriendRequestsFromCustomerToOwner();
        cleanup(this::removeFriendRequestsFromCustomerToOwner);

        assertSuccess(communityApi.sendFriendRequest(customer(), owner().userId()));

        JsonPath incoming = assertSuccess(communityApi.incomingFriendRequests(owner()));
        assertThat(incoming.getList("data.user.id", Long.class)).contains(customer().userId());
        assertThat(incoming.getString("data.find { it.user.id == " + customer().userId() + " }.status"))
                .isEqualTo("PENDING");
        assertBusinessError(communityApi.sendFriendRequest(customer(), owner().userId()), "already exists");
    }

    @Test(groups = {"api", "community", "security"})
    @Severity(SeverityLevel.CRITICAL)
    @Description("The community needs a signed-in user (401).")
    public void anonymousCannotUseCommunity() {
        assertStatus(communityApi.feed(null), 401);
        assertStatus(communityApi.createPost(null, PostRequest.text("anon")), 401);
    }
}
