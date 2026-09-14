package com.zhihuan.user.service;

/**
 * 关注关系服务
 */
public interface UserFollowService {

    /** 关注目标用户 */
    void follow(Long userId, Long targetUserId);

    /** 取消关注 */
    void unfollow(Long userId, Long targetUserId);

    /** 是否已关注 */
    boolean isFollowing(Long userId, Long targetUserId);
}