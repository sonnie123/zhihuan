package com.zhihuan.user.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收货地址出参
 */
@Data
public class AddressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String receiverName;

    private String receiverPhone;

    private String province;

    private String city;

    private String district;

    private String detail;

    private Integer isDefault;

    private LocalDateTime createTime;
}