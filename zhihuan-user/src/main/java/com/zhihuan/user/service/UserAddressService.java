package com.zhihuan.user.service;

import com.zhihuan.user.dto.AddressDTO;
import com.zhihuan.user.vo.AddressVO;

import java.util.List;

/**
 * 收货地址服务
 */
public interface UserAddressService {

    List<AddressVO> list(Long userId);

    Long add(Long userId, AddressDTO dto);

    void update(Long userId, Long id, AddressDTO dto);

    void delete(Long userId, Long id);

    void setDefault(Long userId, Long id);
}