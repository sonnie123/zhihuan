package com.zhihuan.user.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhihuan.common.exception.BizException;
import com.zhihuan.common.result.ResultCode;
import com.zhihuan.user.dto.AddressDTO;
import com.zhihuan.user.entity.UserAddress;
import com.zhihuan.user.mapper.UserAddressMapper;
import com.zhihuan.user.service.UserAddressService;
import com.zhihuan.user.vo.AddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 收货地址服务实现
 */
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<AddressVO> list(Long userId) {
        List<UserAddress> list = userAddressMapper.selectList(
            new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getCreateTime));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(Long userId, AddressDTO dto) {
        // 首条地址默认设为默认
        Long count = userAddressMapper.selectCount(
            new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId));
        int isDefault = dto.getIsDefault() == null ? 0 : dto.getIsDefault();
        if (count == 0) {
            isDefault = 1;
        }
        if (isDefault == 1) {
            clearDefault(userId);
        }
        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(dto, address);
        address.setId(IdUtil.getSnowflakeNextId());
        address.setUserId(userId);
        address.setIsDefault(isDefault);
        userAddressMapper.insert(address);
        return address.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long id, AddressDTO dto) {
        UserAddress address = getUserAddress(userId, id);
        BeanUtils.copyProperties(dto, address);
        address.setUserId(userId);
        address.setId(id);
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefault(userId);
        }
        userAddressMapper.updateById(address);
    }

    @Override
    public void delete(Long userId, Long id) {
        UserAddress address = getUserAddress(userId, id);
        userAddressMapper.deleteById(address.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long id) {
        getUserAddress(userId, id);
        clearDefault(userId);
        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
            .eq(UserAddress::getId, id)
            .set(UserAddress::getIsDefault, 1));
    }

    private void clearDefault(Long userId) {
        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
            .eq(UserAddress::getUserId, userId)
            .set(UserAddress::getIsDefault, 0));
    }

    private UserAddress getUserAddress(Long userId, Long id) {
        UserAddress address = userAddressMapper.selectOne(
            new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, id)
                .eq(UserAddress::getUserId, userId));
        if (address == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "地址不存在");
        }
        return address;
    }

    private AddressVO toVO(UserAddress address) {
        AddressVO vo = new AddressVO();
        BeanUtils.copyProperties(address, vo);
        return vo;
    }
}