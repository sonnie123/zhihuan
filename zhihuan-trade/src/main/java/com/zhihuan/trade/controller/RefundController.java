package com.zhihuan.trade.controller;

import com.zhihuan.common.result.Result;
import com.zhihuan.trade.dto.RefundApplyDTO;
import com.zhihuan.trade.service.RefundService;
import com.zhihuan.trade.vo.RefundOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 退款接口
 */
@Tag(name = "交易-退款")
@RestController
@RequestMapping("/refund")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "申请退款")
    @PostMapping("/apply")
    public Result<Long> apply(@Valid @RequestBody RefundApplyDTO dto) {
        return Result.success(refundService.applyRefund(dto));
    }

    @Operation(summary = "卖家审核退款")
    @PostMapping("/audit")
    public Result<Void> audit(@RequestParam Long refundId, @RequestParam Long sellerId,
                              @RequestParam boolean agree) {
        refundService.sellerAudit(refundId, sellerId, agree);
        return Result.success();
    }

    @Operation(summary = "查询订单退款单")
    @GetMapping("/list")
    public Result<List<RefundOrderVO>> list(@RequestParam Long orderId) {
        return Result.success(refundService.listByOrder(orderId));
    }
}