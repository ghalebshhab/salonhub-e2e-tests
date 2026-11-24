package com.salonhub.qa.api.endpoints;

import com.salonhub.qa.api.client.ApiClient;
import com.salonhub.qa.api.client.ApiSession;
import com.salonhub.qa.api.models.PostRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;

import java.util.Map;

/** {@code /api/community}: posts, likes, comments, friends and salon follows. */
public final class CommunityApi {

    private static final String POST = "/api/community/posts/{id}";

    @Step("GET /api/community/feed")
    public Response feed(ApiSession session) {
        return ApiClient.as(session).get("/api/community/feed");
    }

    @Step("GET /api/community/people")
    public Response people(ApiSession session) {
        return ApiClient.as(session).get("/api/community/people");
    }

    @Step("POST /api/community/posts")
    public Response createPost(ApiSession session, PostRequest body) {
        return ApiClient.as(session).body(body).post("/api/community/posts");
    }

    @Step("GET /api/community/posts/{postId}")
    public Response getPost(ApiSession session, long postId) {
        return ApiClient.as(session).get(POST, postId);
    }

    @Step("PUT /api/community/posts/{postId}")
    public Response updatePost(ApiSession session, long postId, PostRequest body) {
        return ApiClient.as(session).body(body).put(POST, postId);
    }

    @Step("DELETE /api/community/posts/{postId}")
    public Response deletePost(ApiSession session, long postId) {
        return ApiClient.as(session).delete(POST, postId);
    }

    @Step("PUT /api/community/posts/{postId}/like")
    public Response like(ApiSession session, long postId) {
        return ApiClient.as(session).put(POST + "/like", postId);
    }

    @Step("DELETE /api/community/posts/{postId}/like")
    public Response unlike(ApiSession session, long postId) {
        return ApiClient.as(session).delete(POST + "/like", postId);
    }

    @Step("GET /api/community/posts/{postId}/likes")
    public Response likes(ApiSession session, long postId) {
        return ApiClient.as(session).get(POST + "/likes", postId);
    }

    @Step("POST /api/community/posts/{postId}/comments")
    public Response comment(ApiSession session, long postId, String content) {
        return ApiClient.as(session).body(Map.of("content", content)).post(POST + "/comments", postId);
    }

    @Step("GET /api/community/posts/{postId}/comments")
    public Response comments(ApiSession session, long postId) {
        return ApiClient.as(session).get(POST + "/comments", postId);
    }

    @Step("DELETE /api/community/posts/{postId}/comments/{commentId}")
    public Response deleteComment(ApiSession session, long postId, long commentId) {
        return ApiClient.as(session).delete(POST + "/comments/{commentId}", postId, commentId);
    }

    @Step("PUT /api/community/salons/{salonId}/follow")
    public Response follow(ApiSession session, long salonId) {
        return ApiClient.as(session).put("/api/community/salons/{id}/follow", salonId);
    }

    @Step("DELETE /api/community/salons/{salonId}/follow")
    public Response unfollow(ApiSession session, long salonId) {
        return ApiClient.as(session).delete("/api/community/salons/{id}/follow", salonId);
    }

    @Step("GET /api/community/salons/following")
    public Response followedSalons(ApiSession session) {
        return ApiClient.as(session).get("/api/community/salons/following");
    }

    @Step("POST /api/community/friends/requests/{userId}")
    public Response sendFriendRequest(ApiSession session, long userId) {
        return ApiClient.as(session).post("/api/community/friends/requests/{id}", userId);
    }

    @Step("GET /api/community/friends/requests")
    public Response incomingFriendRequests(ApiSession session) {
        return ApiClient.as(session).get("/api/community/friends/requests");
    }

    /** Declines an incoming request, or cancels one that this user sent. */
    @Step("DELETE /api/community/friends/requests/{friendshipId}")
    public Response deleteFriendRequest(ApiSession session, long friendshipId) {
        return ApiClient.as(session).delete("/api/community/friends/requests/{id}", friendshipId);
    }
}
