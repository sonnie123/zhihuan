package com.zhihuan.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 收货地址请求
 */
@Data
public class AddressDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 收货人姓名 */
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 32, message = "姓名不能超过 32 字符")
    private String receiverName;

    /** 收货人手机号 */
    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "收货人手机号格式不正确")
    private String receiverPhone;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址 */
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址不能超过 255 字符")
    private String detail;

    /** 是否默认地址 1是 0否 */
    private Integer isDefault = 0;
}