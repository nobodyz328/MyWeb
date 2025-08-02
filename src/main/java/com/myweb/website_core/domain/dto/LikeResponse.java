package com.myweb.website_core.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class LikeResponse {
    @JsonProperty("liked")
    private boolean liked;
    
    @JsonProperty("likeCount")
    private int likeCount;
    
    public LikeResponse() {}
    
    public LikeResponse(boolean liked, int likeCount) {
        this.liked = liked;
        this.likeCount = likeCount;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }
    
    @Override
    public String toString() {
        return "LikeResponse{" +
                "liked=" + liked +
                ", likeCount=" + likeCount +
                '}';
    }
}