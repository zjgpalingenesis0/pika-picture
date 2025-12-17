package com.zjg.pikapicture.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.thoughtworks.xstream.core.BaseException;
import com.zjg.pikapicture.exception.BusinessException;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.mapper.SpaceMapper;
import com.zjg.pikapicture.model.dto.spaceanalyze.*;
import com.zjg.pikapicture.model.entity.Picture;
import com.zjg.pikapicture.model.entity.Space;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.service.PictureService;
import com.zjg.pikapicture.service.SpaceAnalyzeService;
import com.zjg.pikapicture.service.SpaceService;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Override
    public void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        //全空间分析或者公共图库分析，仅管理员
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "没有权限");
        }

        //私有空间校验
        else {
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(ObjUtil.isEmpty(spaceId) || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(space, loginUser);
        }

    }

    @Override
    public void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        //管理员查询全空间
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        //管理员查询公共图库
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("space_id");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("space_id", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        //校验
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //查询全部或公共图库逻辑
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
            //统计公共图库的资源使用
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("pic_size");
            if (!spaceUsageAnalyzeRequest.isQueryAll()) {
                queryWrapper.isNull("space_id");
            }
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedCount = pictureObjList.size();
            long usedSize = pictureObjList.stream()
                    .mapToLong(result -> result instanceof Long ? (Long) result : 0)
                    .sum();
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            //公共图库无上限，无比例
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setMaxSize(null);

            return spaceUsageAnalyzeResponse;
        }

        //查询指定空间
        else {
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);

            //获取空间信息
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            //校验空间权限
            spaceService.checkSpaceAuth(space, loginUser);
            //构造返回结果
            SpaceUsageAnalyzeResponse response = new SpaceUsageAnalyzeResponse();
            response.setUsedSize(space.getTotalSize());
            response.setMaxSize(space.getMaxSize());
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
            response.setSizeUsageRatio(sizeUsageRatio);
            response.setUsedCount(space.getTotalCount());
            response.setMaxCount(space.getMaxCount());
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
            response.setCountUsageRatio(countUsageRatio);

            return response;
        }

    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        //校验空间分析权限
        this.checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);

        //创造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //根据分析范围补充查询条件
        this.fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);

        //分组查询
        queryWrapper.select("category AS category",
                "COUNT(*) AS count", "SUM(pic_size) AS total_size")
                .groupBy("category");

        //查询并转换结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    long count = ((Number)result.get("count")).longValue();
                    long totalSize = ((Number)result.get("total_size")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .toList();

    }

    @Override
    public List<SpaceTagsAnalyzeResponse> getSpaceTagsAnalyze(SpaceTagsAnalyzeRequest spaceTagsAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagsAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        //校验空间分析权限
        this.checkSpaceAnalyzeAuth(spaceTagsAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        //根据分析范围补充查询条件
        this.fillAnalyzeQueryWrapper(spaceTagsAnalyzeRequest, queryWrapper);
        //查询所有符合条件的标签
        queryWrapper.select("tags");
        List<String> tagJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
//                .filter(ObjUtil::isNotNull)  //如果不要对空标签计数，过滤掉
//                .map(Object::toString)
                .map(obj -> obj != null ? obj.toString() : null)
                .toList();
        //合并所有标签，统计使用次数
        Map<String, Long> tagCountMap = tagJsonList.stream()
//                .flatMap(tagJson -> JSONUtil.toList(tagJson, String.class).stream())
                .flatMap(tagJson -> {
                    if (tagJson == null || tagJson.trim().isEmpty()) {
                        //为空标签添加计数
                        return Stream.of("空标签");
                    }
                    List<String> tags = JSONUtil.toList(tagJson, String.class);
                    if (tags.isEmpty()) {
                        //JSON数组为空也算空标签
                        return Stream.of("空标签");
                    }
                    return tags.stream();
                })
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        //转成响应对象，按次数降序排序
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))  //降序排序
                .map(entry -> new SpaceTagsAnalyzeResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
//        检验参数
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
//        检查空间分析权限
        this.checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
//        构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
//        根据分析范围补充查询条件
        this.fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
//        查询所有符合条件的图片大小
        queryWrapper.select("pic_size");
        List<Long> sizeList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .toList();

        // 定义分段范围, 用有序map
        Map<String, Long> sizeCountMap = new LinkedHashMap<>();
        sizeCountMap.put("<100KB", sizeList.stream().filter(size -> size < 100 * 1024).count());
        sizeCountMap.put("100KB-500KB", sizeList.stream().filter(size -> size < 500 * 1024 && size >= 100 * 1024).count());
        sizeCountMap.put("500KB-1MB", sizeList.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        sizeCountMap.put(">=1MB", sizeList.stream().filter(size -> size >= 1024 * 1024).count());

        // 转换为响应对象
        return sizeCountMap.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .toList();

    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //检查空间分析权限
        this.checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        //根据范围填充查询条件
        this.fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        //分析维度：每日，每周，每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch(timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(create_time, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(create_time) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(create_time, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }
        //分组和排序
        queryWrapper.groupBy("period").orderByAsc("period");
        //查询结果，转化
        List<Map<String, Object>> pictureMap = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return pictureMap.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number)result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .toList();
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //只有管理员才能运行
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "没有管理员权限");
        //设置查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "space_name", "total_size", "user_id")
                .orderByDesc("total_size")
                .last("limit " + spaceRankAnalyzeRequest.getTopN());

        return spaceService.list(queryWrapper);
    }


}
