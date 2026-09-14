package com.zhihuan.user.controller;

import com.zhihuan.common.result.Result;
import com.zhihuan.user.dto.AddressDTO;
import com.zhihuan.user.service.UserAddressService;
import com.zhihuan.user.service.UserService;
import com.zhihuan.user.vo.AddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址接口
 */
@Tag(name = "收货地址")
@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;
    private final UserService userService;

    @Operation(summary = "地址列表")
    @GetMapping("/list")
    public Result<List<AddressVO>> list() {
        return Result.success(userAddressService.list(userService.getCurrentUserId()));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody AddressDTO dto) {
        return Result.success(userAddressService.add(userService.getCurrentUserId(), dto));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        userAddressService.update(userService.getCurrentUserId(), id, dto);
        return Result.success();
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userAddressService.delete(userService.getCurrentUserId(), id);
        return Result.success();
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        userAddressService.setDefault(userService.getCurrentUserId(), id);
        return Result.success();
    }
}