package com.zjg.pikapicture.controller;

import com.zjg.pikapicture.annotation.AuthCheck;
import com.zjg.pikapicture.common.BaseResponse;
import com.zjg.pikapicture.common.ResultUtils;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.model.dto.spaceanalyze.*;
import com.zjg.pikapicture.model.entity.Space;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.service.SpaceAnalyzeService;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.zjg.pikapicture.constant.UserConstant.ADMIN_ROLE;

@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getCurrentUser(request);

        SpaceUsageAnalyzeResponse response = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);

        return ResultUtils.success(response);
    }

    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getCurrentUser(request);

        List<SpaceCategoryAnalyzeResponse> response = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);

        return ResultUtils.success(response);
    }

    @PostMapping("/tags")
    public BaseResponse<List<SpaceTagsAnalyzeResponse>> getSpaceTagsAnalyze(SpaceTagsAnalyzeRequest spaceTagsAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagsAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getCurrentUser(request);
        List<SpaceTagsAnalyzeResponse> response = spaceAnalyzeService.getSpaceTagsAnalyze(spaceTagsAnalyzeRequest, loginUser);
        return ResultUtils.success(response);
    }

    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getCurrentUser(request);
        List<SpaceSizeAnalyzeResponse> response = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(response);
    }

    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getCurrentUser(request);
        List<SpaceUserAnalyzeResponse> response = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser);
        return ResultUtils.success(response);
    }

    @PostMapping("/rank")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<List<Space>> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getCurrentUser(request);
        List<Space> result = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(result);
    }


}
