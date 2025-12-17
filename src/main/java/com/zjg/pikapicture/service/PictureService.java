package com.zjg.pikapicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjg.pikapicture.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.zjg.pikapicture.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zjg.pikapicture.model.dto.picture.*;
import com.zjg.pikapicture.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.model.vo.PictureVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

/**
* @author Lenovo
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-12-05 21:12:40
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     * @param inputSource
     * @param loginUser
     * @param pictureUploadRequest
     * @return
     */
    PictureVO uploadPicture(Object inputSource, User loginUser, PictureUploadRequest  pictureUploadRequest);



    /**
     * 获取查询对象
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest  pictureQueryRequest);

    /**
     * 获取单张图片包装类
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片封装类（分页）
     * @param page
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> page, HttpServletRequest request);

    /**
     * 校验图片
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 管理员图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    boolean doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 异步图片清理
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 校验空间图片权限
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 封装删除图片
     * @param id
     * @param loginUser
     */
    void deletePicture(Long id, User loginUser);

    /**
     * 修改图片
     * @param loginUser
     */
    void editPicture(User loginUser, PictureEditRequest pictureEditRequest);

    /**
     * 批量编辑图片（包括分类，标签，重命名）
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建AI扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);


}
