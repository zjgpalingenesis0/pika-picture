package com.zjg.pikapicture.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjg.pikapicture.exception.BusinessException;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.model.dto.spaceuser.SpaceUserAddRequest;
import com.zjg.pikapicture.model.dto.spaceuser.SpaceUserEditRequest;
import com.zjg.pikapicture.model.dto.spaceuser.SpaceUserQueryRequest;
import com.zjg.pikapicture.model.entity.Space;
import com.zjg.pikapicture.model.entity.SpaceUser;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.model.enums.SpaceRoleEnum;
import com.zjg.pikapicture.model.vo.SpaceUserVO;
import com.zjg.pikapicture.model.vo.SpaceVO;
import com.zjg.pikapicture.model.vo.UserVO;
import com.zjg.pikapicture.service.SpaceService;
import com.zjg.pikapicture.service.SpaceUserService;
import com.zjg.pikapicture.mapper.SpaceUserMapper;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author Lenovo
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-12-18 14:36:28
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        //校验
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);

        //操作数据库，创建对象
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建空间成员失败");

        return spaceUser.getId();
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        //创建时，用户id和空间id必填
        Long userId = spaceUser.getUserId();
        Long spaceId = spaceUser.getSpaceId();
        if (add) {
            ThrowUtils.throwIf(ObjUtil.hasEmpty(userId, spaceId), ErrorCode.PARAMS_ERROR, "用户id或者空间id为空");
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.OPERATION_ERROR, "用户获取失败");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.OPERATION_ERROR, "空间获取失败");
        }
        //校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        ThrowUtils.throwIf(StrUtil.isBlank(spaceRole), ErrorCode.PARAMS_ERROR);
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (StrUtil.isNotBlank(spaceRole) && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不存在空间角色类型");
        }
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();

        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "space_id", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        queryWrapper.eq(StrUtil.isNotBlank(spaceRole), "space_role", spaceRole);

        return queryWrapper;
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        //对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        //关联用户查询对象
        Long userId = spaceUser.getUserId();
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.NOT_FOUND_ERROR, "用户id不存在");
        User user = userService.getById(userId);
        UserVO userVO = userService.getUserVO(user);
        spaceUserVO.setUser(userVO);
        //关联空间查询对象
        Long spaceId = spaceUser.getSpaceId();
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.NOT_FOUND_ERROR, "空间id不存在");
        Space space = spaceService.getById(spaceId);
        SpaceVO spaceVO = spaceService.getSpaceVO(space);
        spaceUserVO.setSpace(spaceVO);
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        if (spaceUserList == null) {
            return new ArrayList<>();
        }
        //对象转封装
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(SpaceUserVO::objToVo)
                .toList();
        //用set可以去重
        //获取用户id
        Set<Long> userIdSet = spaceUserList.stream()
                .map(SpaceUser::getUserId)
                .collect(Collectors.toSet());
        //获取空间id
        Set<Long> spaceIdSet = spaceUserList.stream()
                .map(SpaceUser::getSpaceId)
                .collect(Collectors.toSet());
        //批量查询用户和空间
        Map<Long, List<User>> userMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        //填充用户，空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            User user = null;
            if (userMap.containsKey(userId)) {
                user = userMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userService.getUserVO(user));
            Space space = null;
            if (spaceMap.containsKey(spaceId)) {
                space = spaceMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }

    @Override
    public Boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest) {
        //实体类和DTO转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        //数据校验
        this.validSpaceUser(spaceUser, false);
        //判断是否存在
        Long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = this.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);

        //编辑
        boolean result = this.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "编辑失败");
        return result;
    }
}




