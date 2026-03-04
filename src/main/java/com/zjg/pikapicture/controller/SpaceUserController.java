package com.zjg.pikapicture.controller;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjg.pikapicture.common.BaseResponse;
import com.zjg.pikapicture.common.DeleteRequest;
import com.zjg.pikapicture.common.ResultUtils;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.manager.auth.annotation.SaSpaceCheckPermission;
import com.zjg.pikapicture.manager.auth.model.SpaceUserPermissionConstant;
import com.zjg.pikapicture.model.dto.spaceuser.SpaceUserAddRequest;
import com.zjg.pikapicture.model.dto.spaceuser.SpaceUserEditRequest;
import com.zjg.pikapicture.model.dto.spaceuser.SpaceUserQueryRequest;
import com.zjg.pikapicture.model.entity.Space;
import com.zjg.pikapicture.model.entity.SpaceUser;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.model.enums.SpaceRoleEnum;
import com.zjg.pikapicture.model.vo.SpaceUserVO;
import com.zjg.pikapicture.service.SpaceService;
import com.zjg.pikapicture.service.SpaceUserService;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 添加成员到空间
     *
     * @param spaceUserAddRequest
     * @return
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long result = spaceUserService.addSpaceUser(spaceUserAddRequest);
        ThrowUtils.throwIf(result == null, ErrorCode.OPERATION_ERROR, "创建失败");
        return ResultUtils.success(result);
    }

    /**
     * 从空间移除成员
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        //判断是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR, "空间成员不存在");
        //删除
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除空间成员失败");
        return ResultUtils.success(result);

    }

    /**
     * 查询某个成员在某个空间中的信息
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = spaceUserQueryRequest.getUserId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(userId, spaceId), ErrorCode.PARAMS_ERROR);
        //设置查询条件
        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);
        SpaceUser spaceUser = spaceUserService.getOne(queryWrapper);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR, "空间成员不存在");
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询空间成员列表
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> getSpaceUserList(SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);

        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);
        List<SpaceUser> spaceUserList = spaceUserService.list(queryWrapper);
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(spaceUserList);

        return ResultUtils.success(spaceUserVOList);

    }

    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(SpaceUserEditRequest spaceUserEditRequest) {
        ThrowUtils.throwIf(spaceUserEditRequest == null, ErrorCode.PARAMS_ERROR);

        Boolean result = spaceUserService.editSpaceUser(spaceUserEditRequest);
        return ResultUtils.success(result);

    }

    /**
     * 查询我加入的团队空间列表
     *
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getCurrentUser(request);
        long userId = loginUser.getId();
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(userId);
        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);
        List<SpaceUser> spaceUserList = spaceUserService.list(queryWrapper);

        // 过滤出团队空间（spaceType = 1）
        List<SpaceUser> teamSpaceUsers = spaceUserList.stream()
                .filter(spaceUser -> {
                    Space space = spaceService.getById(spaceUser.getSpaceId());
                    return space != null && space.getSpaceType() == 1; // 只要团队空间
                })
                .collect(java.util.stream.Collectors.toList());

        // 如果没有团队空间，直接返回空列表
        if (teamSpaceUsers.isEmpty()) {
            return ResultUtils.success(new java.util.ArrayList<>());
        }

        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(teamSpaceUsers);

        return ResultUtils.success(spaceUserVOList);
    }
}
