package com.zjg.pikapicture.api.aliyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.zjg.pikapicture.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.zjg.pikapicture.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.zjg.pikapicture.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.zjg.pikapicture.exception.BusinessException;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {

    @Value("${aliYunAi.apiKey}")
    private String apiKey;
    /**
     * 创建任务地址
     */
    private static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
    /**
     * 查询任务地址
     */
    private static final String get_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建AI扩图任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        //校验
        ThrowUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR, "扩图参数为空");
        ThrowUtils.throwIf(StrUtil.isBlank(apiKey), ErrorCode.PARAMS_ERROR, "阿里云API Key未配置");

        //发送请求
        String requestBody = JSONUtil.toJsonStr(createOutPaintingTaskRequest);
        log.info("调用阿里云AI扩图API，请求参数: {}", requestBody);

        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("Authorization", "Bearer " + apiKey)
                //必须开启异步处理
                .header("X-DashScope-Async", "enable")
                .header("Content-Type", "application/json")
                .body(requestBody);

        //处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {

            String responseBody = httpResponse.body();
            log.info("阿里云AI扩图API响应，状态码: {}, 响应体: {}", httpResponse.getStatus(), responseBody);

            if (!httpResponse.isOk()) {
                log.error("请求异常: {}", responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用阿里云AI扩图API失败，状态码: "
                        + httpResponse.getStatus() + ", 响应: " + responseBody);
            }
            CreateOutPaintingTaskResponse response = JSONUtil.toBean(responseBody, CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();

            if (StrUtil.isNotBlank(errorCode)) {
                String errorMessage = response.getMessage();
                log.error("AI扩图失败，errorCode: {}, errorMessage: {}", errorCode, errorMessage);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "扩图失败");
            }
            return response;
        }

    }

    /**
     * 查询AI扩图任务状态
     *
     * @param taskId 任务ID
     * @return 任务查询响应
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(apiKey), ErrorCode.SYSTEM_ERROR, "阿里云API Key未配置");
        //发送请求
        String url = String.format(get_OUT_PAINTING_TASK_URL, taskId);
        log.info("查询阿里云AI扩图任务状态，任务ID: {}", taskId);

        HttpRequest httpRequest = HttpRequest.get(url)
                .header("Authorization", "Bearer " + apiKey);

        //处理响应
        try (HttpResponse httpResponse = httpRequest.execute()) {

            String responseBody = httpResponse.body();
            log.info("阿里云AI扩图任务状态查询响应，状态码: {}, 响应体: {}", httpResponse.getStatus(), responseBody);

            if (!httpResponse.isOk()) {
                log.error("请求异常: {}", responseBody);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用阿里云AI扩图API失败，状态码: "
                        + httpResponse.getStatus() + ", 响应: " + responseBody);
            }

            return JSONUtil.toBean(responseBody, GetOutPaintingTaskResponse.class);
        }
    }

}
