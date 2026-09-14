package com.zhihuan.trade.controller;

import com.zhihuan.common.result.Result;
import com.zhihuan.trade.dto.OrderCreateDTO;
import com.zhihuan.trade.dto.ReviewDTO;
import com.zhihuan.trade.service.OrderService;
import com.zhihuan.trade.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口（担保交易核心链路）
 */
@Tag(name = "交易-订单")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "下单")
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.createOrder(dto));
    }

    @Operation(summary = "支付")
    @PostMapping("/pay")
    public Result<Void> pay(@RequestParam Long orderId, @RequestParam Long buyerId) {
        orderService.payOrder(orderId, buyerId);
        return Result.success();
    }

    @Operation(summary = "取消")
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestParam Long orderId, @RequestParam Long buyerId,
                               @RequestParam(required = false) String reason) {
        orderService.cancelOrder(orderId, buyerId, reason);
        return Result.success();
    }

    @Operation(summary = "发货")
    @PostMapping("/ship")
    public Result<Void> ship(@RequestParam Long orderId, @RequestParam Long sellerId,
                             @RequestParam(required = false) String company,
                             @RequestParam(required = false) String trackingNo) {
        orderService.shipOrder(orderId, sellerId, company, trackingNo);
        return Result.success();
    }

    @Operation(summary = "确认收货")
    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestParam Long orderId, @RequestParam Long buyerId) {
        orderService.confirmReceipt(orderId, buyerId);
        return Result.success();
    }

    @Operation(summary = "评价")
    @PostMapping("/review")
    public Result<Void> review(@Valid @RequestBody ReviewDTO dto) {
        orderService.review(dto);
        return Result.success();
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderVO> detail(@PathVariable Long orderId) {
        return Result.success(orderService.getById(orderId));
    }
}