package com.zhihuan.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihuan.common.exception.BizException;
import com.zhihuan.common.result.ResultCode;
import com.zhihuan.user.dto.LoginDTO;
import com.zhihuan.user.dto.RegisterDTO;
import com.zhihuan.user.dto.UserUpdateDTO;
import com.zhihuan.user.entity.UserAuth;
import com.zhihuan.user.entity.UserMain;
import com.zhihuan.user.entity.UserProfile;
import com.zhihuan.user.mapper.UserAuthMapper;
import com.zhihuan.user.mapper.UserMainMapper;
import com.zhihuan.user.mapper.UserProfileMapper;
import com.zhihuan.user.service.UserService;
import com.zhihuan.user.vo.LoginVO;
import com.zhihuan.user.vo.UserBasicVO;
import com.zhihuan.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String IDENTITY_PASSWORD = "PASSWORD";

    private final UserMainMapper userMainMapper;
    private final UserAuthMapper userAuthMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO dto) {
        checkIdentifierUnique(dto.getIdentifier());

        long userId = IdUtil.getSnowflakeNextId();

        // 1. 用户主表
        UserMain user = new UserMain();
        user.setId(userId);
        user.setUsername(dto.getIdentifier());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setUserType(dto.getUserType() == null ? 1 : dto.getUserType());
        user.setStatus(1);
        user.setRealNameStatus(0);
        user.setCreditScore(80);
        user.setRegisterTime(LocalDateTime.now());
        userMainMapper.insert(user);

        // 2. 认证信息（BCrypt 哈希密码）
        UserAuth auth = new UserAuth();
        auth.setId(IdUtil.getSnowflakeNextId());
        auth.setUserId(userId);
        auth.setIdentityType(IDENTITY_PASSWORD);
        auth.setIdentifier(dto.getIdentifier());
        auth.setCredential(BCrypt.hashpw(dto.getPassword()));
        userAuthMapper.insert(auth);

        // 3. 用户画像（空初始化）
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        userProfileMapper.insert(profile);

        return userId;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        UserAuth auth = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuth>()
            .eq(UserAuth::getIdentifier, dto.getIdentifier())
            .eq(UserAuth::getIdentityType, IDENTITY_PASSWORD));
        if (auth == null || !BCrypt.checkpw(dto.getPassword(), auth.getCredential())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "账号或密码错误");
        }

        UserMain user = userMainMapper.selectById(auth.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 2) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被封禁");
        }
        if (user.getStatus() != null && user.getStatus() == 3) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已注销");
        }

        // 刷新最近登录时间
        UserMain update = new UserMain();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userMainMapper.updateById(update);

        // Sa-Token 登录
        StpUtil.login(user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setUserType(user.getUserType());
        return vo;
    }

    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    @Override
    public Long getCurrentUserId() {
        long loginId = StpUtil.getLoginIdAsLong();
        return loginId;
    }

    @Override
    public UserVO getCurrentUser() {
        return getUserVO(getCurrentUserId());
    }

    @Override
    public UserVO getUserVO(Long userId) {
        UserMain user = userMainMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void updateProfile(Long userId, UserUpdateDTO dto) {
        UserMain update = new UserMain();
        update.setId(userId);
        update.setNickname(dto.getNickname());
        update.setAvatar(dto.getAvatar());
        update.setGender(dto.getGender());
        update.setUserType(dto.getUserType());
        userMainMapper.updateById(update);
    }

    @Override
    public UserMain getByUserId(Long userId) {
        return userMainMapper.selectById(userId);
    }

    @Override
    public List<UserBasicVO> listBasic(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return List.of();
        }
        List<UserMain> users = userMainMapper.selectBatchIds(userIds);
        return users.stream().map(u -> {
            UserBasicVO vo = new UserBasicVO();
            vo.setId(u.getId());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setUserType(u.getUserType());
            vo.setCreditScore(u.getCreditScore());
            return vo;
        }).collect(Collectors.toList());
    }

    private void checkIdentifierUnique(String identifier) {
        Long count = userAuthMapper.selectCount(new LambdaQueryWrapper<UserAuth>()
            .eq(UserAuth::getIdentifier, identifier));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "账号已存在");
        }
    }
}