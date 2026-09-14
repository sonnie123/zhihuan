package com.zhihuan.user.controller;

import com.zhihuan.common.result.Result;
import com.zhihuan.user.service.UserFollowService;
import com.zhihuan.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注关系接口
 */
@Tag(name = "关注关系")
@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
public class UserFollowController {

    private final UserFollowService userFollowService;
    private final UserService userService;

    @Operation(summary = "关注用户")
    @PostMapping("/{targetUserId}")
    public Result<Void> follow(@PathVariable Long targetUserId) {
        userFollowService.follow(userService.getCurrentUserId(), targetUserId);
        return Result.success();
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/{targetUserId}")
    public Result<Void> unfollow(@PathVariable Long targetUserId) {
        userFollowService.unfollow(userService.getCurrentUserId(), targetUserId);
        return Result.success();
    }

    @Operation(summary = "是否已关注")
    @GetMapping("/{targetUserId}")
    public Result<Boolean> isFollowing(@PathVariable Long targetUserId) {
        return Result.success(userFollowService.isFollowing(userService.getCurrentUserId(), targetUserId));
    }
}