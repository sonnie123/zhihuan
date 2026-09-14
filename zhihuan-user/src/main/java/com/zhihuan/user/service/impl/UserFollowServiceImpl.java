package com.zhihuan.user.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhihuan.common.exception.BizException;
import com.zhihuan.common.result.ResultCode;
import com.zhihuan.user.entity.UserFollow;
import com.zhihuan.user.entity.UserMain;
import com.zhihuan.user.mapper.UserFollowMapper;
import com.zhihuan.user.mapper.UserMainMapper;
import com.zhihuan.user.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 关注关系服务实现
 */
@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {

    private final UserFollowMapper userFollowMapper;
    private final UserMainMapper userMainMapper;

    @Override
    public void follow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "不能关注自己");
        }
        UserMain target = userMainMapper.selectById(targetUserId);
        if (target == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "目标用户不存在");
        }

        UserFollow exist = userFollowMapper.selectOne(new LambdaQueryWrapper<UserFollow>()
            .eq(UserFollow::getUserId, userId)
            .eq(UserFollow::getTargetUserId, targetUserId));
        if (exist != null) {
            if (exist.getStatus() != null && exist.getStatus() == 1) {
                return;
            }
            userFollowMapper.update(null, new LambdaUpdateWrapper<UserFollow>()
                .eq(UserFollow::getId, exist.getId())
                .set(UserFollow::getStatus, 1));
            return;
        }

        UserFollow follow = new UserFollow();
        follow.setId(IdUtil.getSnowflakeNextId());
        follow.setUserId(userId);
        follow.setTargetUserId(targetUserId);
        follow.setStatus(1);
        userFollowMapper.insert(follow);
    }

    @Override
    public void unfollow(Long userId, Long targetUserId) {
        userFollowMapper.update(null, new LambdaUpdateWrapper<UserFollow>()
            .eq(UserFollow::getUserId, userId)
            .eq(UserFollow::getTargetUserId, targetUserId)
            .set(UserFollow::getStatus, 0));
    }

    @Override
    public boolean isFollowing(Long userId, Long targetUserId) {
        Long count = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
            .eq(UserFollow::getUserId, userId)
            .eq(UserFollow::getTargetUserId, targetUserId)
            .eq(UserFollow::getStatus, 1));
        return count != null && count > 0;
    }
}