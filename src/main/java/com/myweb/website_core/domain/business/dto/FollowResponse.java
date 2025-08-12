package com.myweb.website_core.domain.business.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class FollowResponse {
    @JsonProperty("followed")
    private boolean followed;
    
    @JsonProperty("followersCount")
    private int followersCount;
    
    public FollowResponse() {}
    
    public FollowResponse(boolean followed, int followersCount) {
        this.followed = followed;
        this.followersCount = followersCount;
    }

    public void setFollowed(boolean followed) {
        this.followed = followed;
    }

    public void setFollowersCount(int followersCount) {
        this.followersCount = followersCount;
    }
    
    @Override
    public String toString() {
        return "FollowResponse{" +
                "followed=" + followed +
                ", followersCount=" + followersCount +
                '}';
    }
}