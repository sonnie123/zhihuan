package com.zhihuan.user.service;

import com.zhihuan.user.dto.LoginDTO;
import com.zhihuan.user.dto.RegisterDTO;
import com.zhihuan.user.dto.UserUpdateDTO;
import com.zhihuan.user.entity.UserMain;
import com.zhihuan.user.vo.LoginVO;
import com.zhihuan.user.vo.UserBasicVO;
import com.zhihuan.user.vo.UserVO;

import java.util.Collection;
import java.util.List;

/**
 * 用户服务：注册/登录/资料管理/用户查询
 */
public interface UserService {

    /** 注册 */
    Long register(RegisterDTO dto);

    /** 登录，返回令牌 */
    LoginVO login(LoginDTO dto);

    /** 退出登录 */
    void logout();

    /** 当前登录用户ID */
    Long getCurrentUserId();

    /** 查询当前登录用户详情 */
    UserVO getCurrentUser();

    /** 查询用户详情 */
    UserVO getUserVO(Long userId);

    /** 修改资料 */
    void updateProfile(Long userId, UserUpdateDTO dto);

    UserMain getByUserId(Long userId);

    /** 批量查询用户基础信息（跨服务/脱敏） */
    List<UserBasicVO> listBasic(Collection<Long> userIds);
}