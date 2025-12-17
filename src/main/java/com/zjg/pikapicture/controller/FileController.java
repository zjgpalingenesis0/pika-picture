package com.zjg.pikapicture.controller;


import jakarta.servlet.http.HttpServletResponse;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.zjg.pikapicture.annotation.AuthCheck;
import com.zjg.pikapicture.common.BaseResponse;
import com.zjg.pikapicture.common.ResultUtils;
import com.zjg.pikapicture.exception.BusinessException;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.manager.COSManager;
import com.zjg.pikapicture.service.PictureService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.net.BindException;

import static com.zjg.pikapicture.constant.UserConstant.ADMIN_ROLE;

/**
 * 测试文件上传下载功能的
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private COSManager cosManager;

    @PostMapping("/test/upload")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        //指定上传文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);

        File file = null;
        try {
            //上传文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            //返回可访问地址
            return ResultUtils.success(filepath);
        } catch (IOException e) {
            log.error("file upload error: " + e.getMessage()
                    + ", filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            if (file != null) {
                //删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("delete file error: " + filepath);
                }
            }
        }
    }

    @GetMapping("/test/download")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            //        1. 直接调用getObject
            COSObject cosObject = cosManager.getObject(filepath);
//        2. 获取输入流.getObjectContent()
            cosObjectInput = cosObject.getObjectContent();
            //处置下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
//        3. 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
//        4. 输出流写入响应，刷新
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file download error: " + e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }


}
