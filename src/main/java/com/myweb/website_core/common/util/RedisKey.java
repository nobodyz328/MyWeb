package com.myweb.website_core.common.util;

public class RedisKey {
    public static final String POST_LIKE_COUNT = "post:like:count:";
    public static final String POST_COLLECT_COUNT = "post:collect:count:";
    public static String likeKey(Long postId ,Long userId){
        return "user:like:" + userId + ":" + postId;
    }
    public static String collectKey(Long postId ,Long userId){
        return "user:collect:" + userId + ":" + postId;
    }
}
