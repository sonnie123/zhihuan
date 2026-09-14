package com.zhihuan.user.api.impl;

import com.zhihuan.user.api.UserDubboService;
import com.zhihuan.user.entity.UserMain;
import com.zhihuan.user.service.UserService;
import com.zhihuan.user.vo.UserBasicVO;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 用户 Dubbo 服务实现
 */
@DubboService
@RequiredArgsConstructor
public class UserDubboServiceImpl implements UserDubboService {

    private final UserService userService;

    @Override
    public UserBasicVO getBasicById(Long userId) {
        List<UserBasicVO> list = userService.listBasic(List.of(userId));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<UserBasicVO> listBasicByIds(Collection<Long> userIds) {
        return userService.listBasic(userIds);
    }

    @Override
    public boolean isUserValid(Long userId) {
        UserMain user = userService.getByUserId(userId);
        return user != null && Objects.equals(user.getStatus(), 1);
    }
}