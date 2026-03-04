package com.zjg.pikapicture.service.impl;
import java.util.*;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjg.pikapicture.api.aliyunai.AliYunAiApi;
import com.zjg.pikapicture.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.zjg.pikapicture.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zjg.pikapicture.exception.BusinessException;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.manager.COSManager;
import com.zjg.pikapicture.manager.FileManager;
import com.zjg.pikapicture.manager.upload.FilePictureUpload;
import com.zjg.pikapicture.manager.upload.PictureUploadTemplate;
import com.zjg.pikapicture.manager.upload.UrlPictureUpload;
import com.zjg.pikapicture.model.dto.file.UploadPictureResult;
import com.zjg.pikapicture.model.dto.picture.*;
import com.zjg.pikapicture.model.entity.Picture;
import com.zjg.pikapicture.model.entity.Space;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.model.enums.PictureReviewStatusEnum;
import com.zjg.pikapicture.model.enums.UserRoleEnum;
import com.zjg.pikapicture.model.vo.PictureVO;
import com.zjg.pikapicture.model.vo.UserVO;
import com.zjg.pikapicture.service.PictureService;
import com.zjg.pikapicture.mapper.PictureMapper;
import com.zjg.pikapicture.service.SpaceService;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Collectors;

import static com.zjg.pikapicture.exception.ErrorCode.*;
import static com.zjg.pikapicture.model.enums.UserRoleEnum.ADMIN;

/**
* @author Lenovo
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-12-05 21:12:40
*/
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

//    @Resource
//    private FileManager fileManager;
    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private UserService userService;

    @Resource
    private COSManager cosManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Override
    public PictureVO uploadPicture(Object inputSource, User loginUser, PictureUploadRequest pictureUploadRequest) {
//        1. 校验参数
        ThrowUtils.throwIf(inputSource == null, PARAMS_ERROR,"图片为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 空间校验
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, NOT_FOUND_ERROR, "空间不存在");
            // 改为使用统一的权限校验
            //仅空间的管理员才能上传
//            if (!Objects.equals(loginUser.getId(), space.getUserId())) {
//                throw new BusinessException(NO_AUTH_ERROR, "权限不够");
//            }
            //校验空间额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(OPERATION_ERROR, "上传图片总数量过多");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(OPERATION_ERROR, "上传图片总大小太大");
            }
        }
//2. 判断是新增还是更新图片
        Long pictureId = null;
    //        1.如果PictureUploadRequest不为空，获取到上传图片id
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
    //        2.如果id不为空，this.lambdaQuery判断是否存在，不存在  NOT_FOUND_ERROR
//        if (pictureId != null) {
////            boolean exists = this.lambdaQuery()
////                    .eq(Picture::getId, pictureId)
////                    .exists();
////            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "数据库中图片不存在");
//        }
        //如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, NOT_FOUND_ERROR);
            // 改为使用统一的权限校验
            //仅本人或者管理员可以编辑
//            if (!userService.isAdmin(loginUser) && !oldPicture.getUserId().equals(loginUser.getId())) {
//                throw new BusinessException(NO_AUTH_ERROR);
//            }
            //校验空间是否一致
            //没传spaceId,复用原图片的
            Long oldSpaceId = oldPicture.getSpaceId();
            if (spaceId == null) {
                if (oldSpaceId != null) {
                    spaceId = oldSpaceId;
                }
            }
            //否则，要保证前后空间一致
            else {
                if (ObjUtil.notEqual(oldSpaceId, spaceId)) {
                    throw new BusinessException(PARAMS_ERROR, "空间id不一致");
                }
            }
        }
//        3. 上传图片，得到图片信息
    //        1. 调用上传图片方法（这里先写一个上传文件目录前缀，这里用字符串格式化，/public/用户id），获取到了上传文件结果
//        String prefix = String.format("public/%s", loginUser.getId());
        //按照用户id划分目录 -> 按照空间划分目录
        String prefix;
        if (spaceId == null) {
            //公共图库
            prefix = String.format("public/%s", loginUser.getId());
        }
        else {
            //私有空间
            prefix = String.format("space/%s", spaceId);
        }

        //根据输入源选择上传图片方法
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, prefix);
    //        2. 构造要入库的图片信息。（创建picture实体类，把上传文件结果的信息，放入对象）
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        //补充设置空间id
        picture.setSpaceId(spaceId);
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);

        BeanUtils.copyProperties(uploadPictureResult, picture);
        picture.setUserId(loginUser.getId());
        // 手动设置name字段，因为BeanUtils无法自动映射picName到name
        picture.setName(uploadPictureResult.getPicName());
        //补充审核参数
        fillReviewParams(picture, loginUser);
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
//        4. 操作数据库
//        1. 判断如果图片id不为空，表示更新，否则是新增（更新的话要设置id和edittime）
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        //开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            //保存更新图片
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "上传图片失败");
            //更新空间额度
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("total_size = total_size + " + picture.getPicSize())
                        .setSql("total_count = total_count + 1")
                        .update();
                ThrowUtils.throwIf(!update, OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });

        //如果是更新，清理旧图片
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            if (oldPicture != null && !oldPicture.getUrl().equals(picture.getUrl())) {
                clearPictureFile(oldPicture);
            }
        }
        return PictureVO.objToVo(picture);
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {

        //判空
        ThrowUtils.throwIf(pictureQueryRequest == null, PARAMS_ERROR);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //获取全部查询属性
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        Long userId = pictureQueryRequest.getUserId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Boolean nullSpaceId = pictureQueryRequest.getNullSpaceId();
        String searchText = pictureQueryRequest.getSearchText();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        //从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            //需要拼接查询条件
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                    .like("introduction", searchText)
            );
        }
        //添加所有条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        //补充空间相关字段
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "space_id", spaceId);
        // 处理 nullSpaceId 参数：true-查询公共图库，false-查询私有空间，null-查询所有
        if (Boolean.TRUE.equals(nullSpaceId)) {
            queryWrapper.isNull(true, "space_id");
        } else if (Boolean.FALSE.equals(nullSpaceId)) {
            queryWrapper.isNotNull(true, "space_id");
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "pic_size", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "pic_width", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "pic_height", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "pic_scale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "review_status", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewer_id", reviewerId);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "review_message", reviewMessage);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "pic_format", picFormat);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        //json数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        //补充按编辑时间筛选的逻辑
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "edit_time", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "edit_time", endEditTime);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
//  1. 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
// 2. 关联查询用户信息
        // 1. 拿到userid
        Long userId = picture.getUserId();

        // 2. 不为空且大于0，就根据userid获取用户，然后脱敏。
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, NOT_FOUND_ERROR, "用户不存在");
            UserVO userVO = userService.getUserVO(user);
            //3. 设置给pictureVO中的userVO
            pictureVO.setUser(userVO);
        }

        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
//        1. 初始化分页对象：创建返回用的PictureVO分页对象
        List<Picture> pictureList = picturePage.getRecords();
        long current = picturePage.getCurrent();
        long size = picturePage.getSize();
        long total = picturePage.getTotal();
        Page<PictureVO> pictureVOPage = new Page<>(current, size, total);
//        2. 空数据处理：如果图片列表为空，直接返回空的分页结果
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
//        3. 对象转换：将Picture实体列表转换为PictureVO视图对象列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .toList();
//        4. 用户信息查询：批量查询所有相关用户的详细信息
        Set<Long> userIdSet = pictureList.stream()
                .map(Picture::getUserId)
                .collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
//        5. 数据填充：将用户信息填充到对应的PictureVO对象中
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        });
//        6. 返回结果：设置好填充后的记录列表并返回
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, PARAMS_ERROR);
//        1. 从对象中取值（id，url，introduction）
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
//        2. 修改数据时，id不能为空，有参数则校验  PARAMS_ERROR
        // 注意：创建图片后第一次编辑信息时，id可能为空是允许的
        // 只有在没有url的情况下才要求id不能为空（纯编辑场景）
        if (StrUtil.isBlank(url)) {
            ThrowUtils.throwIf(ObjUtil.isNull(id), PARAMS_ERROR, "id为空");
        }
//        3. 如果传递了url，才校验  PARAMS_ERROR
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, PARAMS_ERROR, "url过长");
        }

//        4. introduction不为空，才校验  PARAMS_ERROR
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, PARAMS_ERROR, "简介过长");
        }

    }

    @Override
    public boolean doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
//        1. 校验
        ThrowUtils.throwIf(pictureReviewRequest == null, PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatus = PictureReviewStatusEnum.getEnumByVaule(reviewStatus);
        ThrowUtils.throwIf(id <= 0 || pictureReviewStatus == null, PARAMS_ERROR);
//        2. 判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, NOT_FOUND_ERROR);
//        3. 校验审核状态是否重复
        if(oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(PARAMS_ERROR, "不要重复审核");
        }
//        4. 数据库操作
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, picture);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, OPERATION_ERROR);
        return result;
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        //管理员操作自动过审
        if (userService.isAdmin(loginUser)) {
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员操作无需审核");
        }
        //非管理员创建和编辑后都改为待审核
        else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //校验
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, PARAMS_ERROR);
        Integer count = pictureUploadByBatchRequest.getCount();
        String searchText = pictureUploadByBatchRequest.getSearchText();
        ThrowUtils.throwIf(count > 30,  PARAMS_ERROR, "抓取图片过多");
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        //抓取内容
        //字符串标准化, 要抓取的地址
        String fetchUrl = String.format("http://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            //抓取
            document = Jsoup.connect(fetchUrl).get();
        } catch (Exception e) {
            throw new BusinessException(OPERATION_ERROR, "获取页面失败" + e.getMessage());
        }
        //解析内容
        //拿最外层
        Element div = document.getElementsByClass("dgControl").first();
        ThrowUtils.throwIf(ObjUtil.isNull(div), OPERATION_ERROR, "获取元素失败");

        // 获取包含图片信息的链接元素
        Elements linkList = div.select("a.iusc");
        int uploadCount = 0;
        for (Element link : linkList) {
            String fileUrl = null;

            try {
                // 从m属性中解析JSON数据获取真实图片URL
                String mAttribute = link.attr("m");
                if (StrUtil.isNotBlank(mAttribute)) {
                    JSONObject jsonData = JSONUtil.parseObj(mAttribute);
                    fileUrl = jsonData.getStr("murl");
                }

                // 如果JSON解析失败，尝试从href中解析mediaurl
                if (StrUtil.isBlank(fileUrl)) {
                    String href = link.attr("href");
                    if (StrUtil.isNotBlank(href) && href.contains("mediaurl=")) {
                        String[] parts = href.split("mediaurl=");
                        if (parts.length > 1) {
                            String mediaUrl = parts[1].split("&")[0];
                            fileUrl = java.net.URLDecoder.decode(mediaUrl, "UTF-8");
                        }
                    }
                }

                if (StrUtil.isBlank(fileUrl)) {
                    log.info("未能获取到图片URL，已跳过");
                    continue;
                }
            } catch (Exception e) {
                log.error("解析图片URL失败", e);
                continue;
            }

            //上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if (StrUtil.isNotBlank(namePrefix)) {
                //设置图片名称，序号连续递增
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, loginUser, pictureUploadRequest);
                log.info("图片上传成功，id={}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        //判断图片是否对多条记录使用
        String pictureUrl = oldPicture.getUrl();
        Long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        //如果不止一条记录用到该图片
        if (count > 1) {
            return;
        }
        cosManager.deleteObject(pictureUrl);
        //清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long userId = picture.getUserId();
        if (spaceId == null) {
            //公共图库  管理员+本人
            if (!userService.isAdmin(loginUser) && !userId.equals(loginUser.getId())) {
                throw new BusinessException(NO_AUTH_ERROR, "权限不够");
            }
        }
        else {
            //私有空间  本人
            if (!userId.equals(loginUser.getId())) {
                throw new BusinessException(NO_AUTH_ERROR, "权限不够");
            }
        }
    }

    @Override
    public void deletePicture(Long id, User loginUser) {
        // 存在性检查：确认要删除的图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ThrowUtils.throwIf(!oldPicture.getUserId().equals(loginUser.getId())
                && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "没有权限");
        // 校验权限，已经改为使用注解鉴权
//        this.checkPictureAuth(loginUser, oldPicture);

        //执行删除：调用服务层删除数据库记录
        //开启事务
        transactionTemplate.execute(status -> {
            //删除图片
            boolean result = this.removeById(id);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "上传图片失败");
            //释放空间额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("total_size = total_size - " + oldPicture.getPicSize())
                        .setSql("total_count = total_count - 1")
                        .update();
                ThrowUtils.throwIf(!update, OPERATION_ERROR, "额度删除失败");
            }
            return true;
        });

        // 异步清理图片
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(User loginUser, PictureEditRequest pictureEditRequest) {
        Long id = pictureEditRequest.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        //数据转换：将DTO转换为实体对象，处理标签字段的JSON序列化
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //时间设置：设置编辑时间为当前时间
        picture.setEditTime(new Date());
        //数据校验：调用服务层校验图片数据有效性
        this.validPicture(picture);
        //存在性检查：确认要编辑的图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //权限验证：校验操作权限（只有本人或管理员可编辑）
        ThrowUtils.throwIf(!oldPicture.getUserId().equals(loginUser.getId())
                && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        //补充审核信息
        this.fillReviewParams(picture, loginUser);

        // 校验权限，已经改为使用注解鉴权
//        this.checkPictureAuth(loginUser, oldPicture);

//        - 执行更新：调用服务层更新数据库记录
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
//        1. 获取和校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();

//        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceId), PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, PARAMS_ERROR);
//        2. 校验空间权限
        if (spaceId == null) {
            //公共图库，只能让管理员批量修改
            if (!userService.isAdmin(loginUser)) {
                throw new BusinessException(NO_AUTH_ERROR, "权限不够");
            }

        }
        else {
            //私有图库
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, NOT_FOUND_ERROR, "空间不存在");

            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(NO_AUTH_ERROR, "权限不够");
            }
        }

//        3. 查询指定图片，仅选择需要的字段
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(spaceId != null, Picture::getSpaceId, spaceId)
                .isNull(spaceId == null, Picture::getSpaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (pictureList.isEmpty()) {
            return;
        }
//        4. 更新和分类标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        //批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        this.fillPictureWithNameRule(pictureList, nameRule);
//        5. 批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "批量更新失败");
    }

    @Override
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {

        //获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = this.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 校验权限，已经改为使用注解鉴权
//        checkPictureAuth(loginUser, picture);

        //构造请求参数
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        String url = picture.getUrl();
        ThrowUtils.throwIf(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR, "图片没有url");
        input.setImageUrl(url);
        createOutPaintingTaskRequest.setInput(input);
        BeanUtils.copyProperties(createPictureOutPaintingTaskRequest, createOutPaintingTaskRequest);

        //创建任务
        return aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);

    }

    /**
     * 填充图片名称
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        //校验
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(StrUtil.isBlank(nameRule), ErrorCode.PARAMS_ERROR);
        //遍历图片列表，重命名
        int count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replace("{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
        throw new BusinessException(OPERATION_ERROR, e.getMessage());
        }
    }
}




