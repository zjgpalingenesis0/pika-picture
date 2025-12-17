package com.zjg.pikapicture.controller;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjg.pikapicture.annotation.AuthCheck;
import com.zjg.pikapicture.common.BaseResponse;
import com.zjg.pikapicture.common.DeleteRequest;
import com.zjg.pikapicture.common.ResultUtils;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.manager.cache_.CacheTemplate;
import com.zjg.pikapicture.manager.cache_.CaffeineCache;
import com.zjg.pikapicture.manager.cache_.MultiLevelCache;
import com.zjg.pikapicture.manager.cache_.RedisCache;

import com.zjg.pikapicture.model.dto.space.*;
import com.zjg.pikapicture.model.entity.Space;
import com.zjg.pikapicture.model.entity.Space;
import com.zjg.pikapicture.model.entity.User;

import com.zjg.pikapicture.model.enums.SpaceLevelEnum;
import com.zjg.pikapicture.model.vo.SpaceVO;
import com.zjg.pikapicture.service.SpaceService;
import com.zjg.pikapicture.service.SpaceService;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.zjg.pikapicture.constant.UserConstant.ADMIN_ROLE;

@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService SpaceService;

    @Resource
    private SpaceService spaceService;

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        //校验
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        String spaceName = spaceAddRequest.getSpaceName();
        Integer spaceLevel = spaceAddRequest.getSpaceLevel();
        ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceLevel), ErrorCode.PARAMS_ERROR);
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);
        ThrowUtils.throwIf(enumByValue == null, ErrorCode.PARAMS_ERROR);
        //创建空间
        User loginUser = userService.getCurrentUser(request);
        Long spaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.OPERATION_ERROR, "创建空间失败");

        return ResultUtils.success(spaceId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(DeleteRequest deleteRequest, HttpServletRequest request) {
//        - 参数校验：检查删除请求对象和ID是否有效
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        //空间id
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
//        - 权限验证：获取当前登录用户，校验操作权限（只有本人或管理员可删除）
        User loginUser = userService.getCurrentUser(request);
//        - 存在性检查：确认要删除的空间是否存在
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ThrowUtils.throwIf(!oldSpace.getUserId().equals(loginUser.getId())
                && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "没有权限");
//        - 执行删除：调用服务层删除数据库记录
        boolean result = SpaceService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除失败");

//        - 返回结果：返回操作成功状态
        return ResultUtils.success(result);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
//        - 参数校验：检查更新请求对象和ID是否有效
        ThrowUtils.throwIf(spaceUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = spaceUpdateRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
//        - 数据转换：将DTO转换为实体对象，处理标签字段的JSON序列化
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        //自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
//        - 数据校验：调用服务层校验空间有效性
        spaceService.validSpace(space, false);
//        - 存在性检查：确认要更新的空间是否存在
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
//        - 执行更新：调用服务层更新数据库记录
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新空间失败");
//        - 返回结果：返回操作成功状态
        return ResultUtils.success(result);
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(Long id) {
//        - 参数校验：检查ID是否有效
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
//        - 查询数据：根据ID查询数据库获取图片信息
        Space space = SpaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
//        - 返回结果：返回图片实体对象
        return ResultUtils.success(space);
    }

    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(Long id, HttpServletRequest request) {
//        - 参数校验：检查ID是否有效
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
//        - 查询数据：根据ID查询数据库获取图片信息
        Space space = SpaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
//        - 封装处理：调用服务层将实体转换为封装对象（包含关联信息）
        SpaceVO spaceVO = SpaceService.getSpaceVO(space);
        ThrowUtils.throwIf(spaceVO == null, ErrorCode.OPERATION_ERROR);
//        - 返回结果：返回图片视图对象
        return ResultUtils.success(spaceVO);
    }

    @PostMapping("list/page")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Page<Space>> getSpaceList(SpaceQueryRequest spaceQueryRequest) {
//        - 参数提取：从查询请求中获取分页参数
        long current = spaceQueryRequest.getCurrent();
        long pageSize = spaceQueryRequest.getPageSize();
//        - 分页查询：调用服务层进行分页查询，返回原始实体数据
        QueryWrapper<Space> queryWrapper = SpaceService.getQueryWrapper(spaceQueryRequest);
        Page<Space> spacePage = SpaceService.page(new Page<>(current, pageSize), queryWrapper);
//        - 返回结果：返回分页后的图片实体列表
        return ResultUtils.success(spacePage);
    }

    @PostMapping("list/page/vo")
    public BaseResponse<Page<SpaceVO>> getSpaceVOList(SpaceQueryRequest spaceQueryRequest) {
//        - 参数提取：从查询请求中获取分页参数
        long current = spaceQueryRequest.getCurrent();
        long pageSize = spaceQueryRequest.getPageSize();

//        - 防爬虫限制：限制单次查询最大数量不超过20条
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);

//        - 分页查询：调用服务层进行分页查询，返回原始实体数据
        QueryWrapper<Space> queryWrapper = SpaceService.getQueryWrapper(spaceQueryRequest);
        Page<Space> spacePage = SpaceService.page(new Page<>(current, pageSize), queryWrapper);
//        - 数据封装：调用服务层将实体分页数据转换为视图对象分页数据（包含用户关联信息）
        Page<SpaceVO> spaceVOPage = SpaceService.getSpaceVOPage(spacePage);
//        - 返回结果：返回分页后的图片实体列表
        return ResultUtils.success(spaceVOPage);
    }


    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
//        - 参数校验：检查编辑请求对象和ID是否有效
        ThrowUtils.throwIf(spaceEditRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = spaceEditRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getCurrentUser(request);
//        - 数据转换：将DTO转换为实体对象，处理标签字段的JSON序列化
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        //填充空间级别信息
        spaceService.fillSpaceBySpaceLevel(space);
//        - 时间设置：设置编辑时间为当前时间
        space.setEditTime(new Date());
//        - 数据校验：调用服务层校验图片数据有效性
        SpaceService.validSpace(space, false);
//        - 存在性检查：确认要编辑的图片是否存在
        Space oldSpace = SpaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
//        - 权限验证：校验操作权限（只有本人或管理员可编辑）
        ThrowUtils.throwIf(!oldSpace.getUserId().equals(loginUser.getId())
                && userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

//        - 执行更新：调用服务层更新数据库记录
        boolean result = SpaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//        - 返回结果：返回操作成功状态
        return ResultUtils.success(result);
    }

    /**
     * 给前端展示所有的空间级别信息
     *
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                ))
                .toList();

        return ResultUtils.success(spaceLevelList);
    }

}
