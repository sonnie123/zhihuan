package com.zhihuan.user.controller;

import com.zhihuan.common.result.Result;
import com.zhihuan.user.dto.UserUpdateDTO;
import com.zhihuan.user.service.UserService;
import com.zhihuan.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户资料接口
 */
@Tag(name = "用户资料")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "查询当前用户详情")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getCurrentUser());
    }

    @Operation(summary = "修改当前用户资料")
    @PutMapping("/me")
    public Result<Void> update(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateProfile(userService.getCurrentUserId(), dto);
        return Result.success();
    }
}