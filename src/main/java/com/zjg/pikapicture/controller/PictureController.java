package com.zjg.pikapicture.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zjg.pikapicture.annotation.AuthCheck;
import com.zjg.pikapicture.api.aliyunai.AliYunAiApi;
import com.zjg.pikapicture.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zjg.pikapicture.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.zjg.pikapicture.common.BaseResponse;
import com.zjg.pikapicture.common.DeleteRequest;
import com.zjg.pikapicture.common.PageRequest;
import com.zjg.pikapicture.common.ResultUtils;
import com.zjg.pikapicture.exception.BusinessException;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.manager.cache_.CacheTemplate;
import com.zjg.pikapicture.manager.cache_.CaffeineCache;
import com.zjg.pikapicture.manager.cache_.MultiLevelCache;
import com.zjg.pikapicture.manager.cache_.RedisCache;
import com.zjg.pikapicture.model.dto.picture.*;
import com.zjg.pikapicture.model.entity.Picture;
import com.zjg.pikapicture.model.entity.Space;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.model.enums.PictureReviewStatusEnum;
import com.zjg.pikapicture.model.vo.PictureVO;
import com.zjg.pikapicture.service.PictureService;
import com.zjg.pikapicture.service.SpaceService;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.zjg.pikapicture.constant.UserConstant.ADMIN_ROLE;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    //    @Resource
//    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisCache redisCache;

    @Resource
    private CaffeineCache caffeineCache;

    @Resource
    private MultiLevelCache multiLevelCache;

    @Resource
    private SpaceService spaceService;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @PostMapping("/upload")
//    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
//        1. 判空
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR);
//        2. 获取登录用户
        User loginUser = userService.getCurrentUser(request);
//        3. 调用uploadPicture方法
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, loginUser, pictureUploadRequest);
        return ResultUtils.success(pictureVO);

    }

    @PostMapping("/upload/url")
//    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPictureByUrl(PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
//        1. 判空
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR);
//        2. 获取登录用户
        User loginUser = userService.getCurrentUser(request);
//        3. 调用uploadPicture方法
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, loginUser, pictureUploadRequest);
        return ResultUtils.success(pictureVO);

    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(DeleteRequest deleteRequest, HttpServletRequest request) {
        //参数校验：检查删除请求对象和ID是否有效
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        //图片id
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        //权限验证：获取当前登录用户，校验操作权限（只有本人或管理员可删除）
        User loginUser = userService.getCurrentUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);
        //删除图片
        pictureService.deletePicture(id, loginUser);
        // 返回结果：返回操作成功状态
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
//        - 参数校验：检查更新请求对象和ID是否有效
        ThrowUtils.throwIf(pictureUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureUpdateRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
//        - 数据转换：将DTO转换为实体对象，处理标签字段的JSON序列化
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
//        - 数据校验：调用服务层校验图片数据有效性
        pictureService.validPicture(picture);
//        - 存在性检查：确认要更新的图片是否存在
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        //补充审核参数
        User loginUser = userService.getCurrentUser(request);
        pictureService.fillReviewParams(picture, loginUser);
//        - 执行更新：调用服务层更新数据库记录
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新图片失败");
//        - 返回结果：返回操作成功状态
        return ResultUtils.success(result);
    }

    @GetMapping("/get")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(Long id) {
//        - 参数校验：检查ID是否有效
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
//        - 查询数据：根据ID查询数据库获取图片信息
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
//        - 返回结果：返回图片实体对象
        return ResultUtils.success(picture);
    }

    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id, HttpServletRequest request) {
//        - 参数校验：检查ID是否有效
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
//        - 查询数据：根据ID查询数据库获取图片信息
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
//        - 封装处理：调用服务层将实体转换为封装对象（包含关联信息）
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        ThrowUtils.throwIf(pictureVO == null, ErrorCode.OPERATION_ERROR);

        //空间权限校验
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            //私有空间
            User loginUser = userService.getCurrentUser(request);
            pictureService.checkPictureAuth(loginUser, picture);
        }


//        - 返回结果：返回图片视图对象
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("list/page")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Page<Picture>> getPictureList(PictureQueryRequest pictureQueryRequest) {
//        - 参数提取：从查询请求中获取分页参数
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();
//        - 分页查询：调用服务层进行分页查询，返回原始实体数据
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize), queryWrapper);
//        - 返回结果：返回分页后的图片实体列表
        return ResultUtils.success(picturePage);
    }

    @PostMapping("list/page/vo")
    public BaseResponse<Page<PictureVO>> getPictureVOList(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
//        - 参数提取：从查询请求中获取分页参数
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();

//        - 防爬虫限制：限制单次查询最大数量不超过20条
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getCurrentUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_FOUND_ERROR);
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            //公共图库
            //普通用户只能默认查看已过审的图片
            if (!userService.isAdmin(loginUser)) {
                pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            }
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            //私有空间,仅空间创建人可以访问
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "空间权限不够");
            }

        }
//        - 分页查询：调用服务层进行分页查询，返回原始实体数据
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize), queryWrapper);
//        - 数据封装：调用服务层将实体分页数据转换为视图对象分页数据（包含用户关联信息）
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
//        - 返回结果：返回分页后的图片实体列表
        return ResultUtils.success(pictureVOPage);
    }

    @PostMapping("list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> getPictureVOListWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
//        - 参数提取：从查询请求中获取分页参数
        long current = pictureQueryRequest.getCurrent();
        long pageSize = pictureQueryRequest.getPageSize();

//        - 防爬虫限制：限制单次查询最大数量不超过20条
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR);
        //普通用户只能默认查看已过审的图片
        User loginUser = userService.getCurrentUser(request);
        if (!userService.isAdmin(loginUser)) {
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        }
        //使用缓存
        //选择缓存
        CacheTemplate cacheTemplate = multiLevelCache;
        CacheTemplate cacheTemplate1 = pictureQueryRequest.getCacheTemplate();
        if (cacheTemplate1 != null) {
            cacheTemplate = cacheTemplate1;
        }
        //从缓存中读
        Page<PictureVO> cacheResult = cacheTemplate.readCache(pictureQueryRequest);
        if (cacheResult != null) {
            return ResultUtils.success(cacheResult);
        }
        //缓存未命中，查询数据库
//        - 分页查询：调用服务层进行分页查询，返回原始实体数据
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        Page<Picture> picturePage = pictureService.page(new Page<>(current, pageSize), queryWrapper);
//        - 数据封装：调用服务层将实体分页数据转换为视图对象分页数据（包含用户关联信息）
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

        //存入redis
        cacheTemplate.writeCache(pictureVOPage, pictureQueryRequest);

//        - 返回结果：返回分页后的图片实体列表
        return ResultUtils.success(pictureVOPage);
    }

    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        //参数校验：检查编辑请求对象和ID是否有效
        ThrowUtils.throwIf(pictureEditRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取当前登录用户
        User loginUser = userService.getCurrentUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_FOUND_ERROR);
        //编辑图片
        pictureService.editPicture(loginUser, pictureEditRequest);
        //返回结果：返回操作成功状态
        return ResultUtils.success(true);
    }

    @GetMapping("tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门",
                "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");

        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);

        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
//        1. 判空
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
//        2. 获取登录用户
        User loginUser = userService.getCurrentUser(request);
//        3. 执行审核
        boolean result = pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        //校验
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        //获取登录用户
        User loginUser = userService.getCurrentUser(request);
        //调用服务
        Integer result = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        ThrowUtils.throwIf(result < 0, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        //校验
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);

        //获取登录用户
        User loginUser = userService.getCurrentUser(request);

        //执行批量编辑操作
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);

        return ResultUtils.success(true);
    }

    @PostMapping("/out_painting/create_task")
    public BaseResponse<CreateOutPaintingTaskResponse> createOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getCurrentUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");

        CreateOutPaintingTaskResponse response = pictureService.createOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);

    }

    @GetMapping("out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse response = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(response);
    }

}
