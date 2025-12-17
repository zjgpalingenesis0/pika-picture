package com.zjg.pikapicture.manager.upload;


import cn.hutool.core.io.FileUtil;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends PictureUploadTemplate {


    /**
     * 验证图片
     *
     * @param inputSource
     */
    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;

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

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;

        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
