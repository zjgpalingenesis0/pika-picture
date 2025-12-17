package com.zjg.pikapicture.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zjg.pikapicture.config.CosClientConfig;
import com.zjg.pikapicture.exception.BusinessException;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.model.dto.file.UploadPictureResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSManager cosManager;

    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        //检验图片
        validPicture(multipartFile);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String filename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(filename);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        String uploadPath = String.format("%s%s", uploadPathPrefix, uploadFilename);
        //解析结果并返回
        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            //上传图片
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult()
                    .getOriginalInfo().getImageInfo();
            //封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

            uploadPictureResult.setUrl(cosClientConfig.getHost() + '/' + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(filename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("upload picture error", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        } finally {
            if (file != null) {
                deleteTempFile(file);
            }

        }

    }

    /**
     * 验证图片
     *
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
//        1. 判空  PARAMS_ERROR
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
//        2. 校验文件大小
        //        1. 先获取文件大小.getSize
        long fileSize = multipartFile.getSize();
        //        2. 定义一个final常量ONE_M，表示单位1MB的数值大小，1024*1024   long
        final long ONE_M = 1024 * 1024L;
        //        3. 判断如果文件大小大于2MB  PARAMS_ERROR
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件不能太大");
//        3. 校验文件后缀
        //        1. 获取 FileUtil.getSuffix
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //    2. 定义允许上传的文件后缀列表final  List<String>   ALLOW_FRMAT_LIST, 有jpeg，png，jpg，webp等等。
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        //        3. 判断如果列表不包含这个后缀，PARAMS_ERROR
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    /**
     * 删除临时文件
     *
     * @param file
     */
    private void deleteTempFile(File file) {
//        1. 判断为空，不清理，直接返回
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR);
//        2. file.delete， 如果删除失败，日志报错
        boolean delete = file.delete();
        if (!delete) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
