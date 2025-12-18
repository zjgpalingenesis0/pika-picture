package com.zjg.pikapicture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjg.pikapicture.exception.BusinessException;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.model.dto.space.SpaceAddRequest;
import com.zjg.pikapicture.model.dto.space.SpaceQueryRequest;
import com.zjg.pikapicture.model.entity.Space;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.model.enums.SpaceLevelEnum;
import com.zjg.pikapicture.model.enums.SpaceTypeEnum;
import com.zjg.pikapicture.model.vo.SpaceVO;
import com.zjg.pikapicture.model.vo.UserVO;
import com.zjg.pikapicture.service.SpaceService;
import com.zjg.pikapicture.mapper.SpaceMapper;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.zjg.pikapicture.exception.ErrorCode.*;

/**
* @author Lenovo
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-12-13 13:40:09
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

//    Map<Long, Object> lockMap = new ConcurrentHashMap<>();


    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        //填充参数默认值，转换实体类和DTO
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        String spaceName = spaceAddRequest.getSpaceName();
        if (StrUtil.isBlank(spaceName)) {
            space.setSpaceName("默认空间");
        }
        Integer spaceLevel = spaceAddRequest.getSpaceLevel();
        if (ObjUtil.isEmpty(spaceLevel)) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        //补充：空间类型设置,默认应是私人空间
        Integer spaceType = spaceAddRequest.getSpaceType();
        if (ObjUtil.isEmpty(spaceType)) {
            space.setSpaceType(SpaceTypeEnum.PERSONAL.getValue());
        }
        fillSpaceBySpaceLevel(space);
        //检验参数
        validSpace(space, true);
        //检验权限
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (!userService.isAdmin(loginUser) && SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel()) {
            throw new BusinessException(NO_AUTH_ERROR, "没有权限");
        }
        //同一用户每种类型的空间只能各自创建一个
        String lock = String.valueOf(userId).intern();
//        Object lock = lockMap.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                //判断是否已有空间
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        //添加空间类型的查询条件
                        .eq(Space::getSpaceType, spaceType)
                        .exists();
                ThrowUtils.throwIf(exists, OPERATION_ERROR, "空间已存在");
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, OPERATION_ERROR, "创建空间失败");
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }

    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {

        //判空
        ThrowUtils.throwIf(spaceQueryRequest == null, PARAMS_ERROR);
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        //获取全部查询属性
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        Long userId = spaceQueryRequest.getUserId();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        //添加所有条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "user_id", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "space_level", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "space_type", spaceType);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "space_name", spaceName);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);

        return queryWrapper;
    }

    @Override
    public SpaceVO getSpaceVO(Space space) {
    //  1. 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);

    // 2. 关联查询用户信息
        // 1. 拿到userid
        Long userId = space.getUserId();

        // 2. 不为空且大于0，就根据userid获取用户，然后脱敏。
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, NOT_FOUND_ERROR, "用户不存在");
            UserVO userVO = userService.getUserVO(user);
            //3. 设置给pictureVO中的userVO
            spaceVO.setUser(userVO);
        }

        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage) {
//        1. 初始化分页对象
        List<Space> spaceList = spacePage.getRecords();
        long current = spacePage.getCurrent();
        long size = spacePage.getSize();
        long total = spacePage.getTotal();
        Page<SpaceVO> spaceVOPage = new Page<>(current, size, total);
//        2. 空数据处理
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
//        3. 对象转换
        List<SpaceVO> spaceVOList = spaceList.stream()
                .map(SpaceVO::objToVo)
                .toList();
//        4. 用户信息查询：批量查询所有相关用户的详细信息
        Set<Long> userIdSet = spaceList.stream()
                .map(Space::getUserId)
                .collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet)
                .stream()
                .collect(Collectors.groupingBy(User::getId));
//        5. 数据填充：将用户信息填充到对应的对象中
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        });
//        6. 返回结果：设置好填充后的记录列表并返回
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, PARAMS_ERROR);
//        1. 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        //创建时校验
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwIf(spaceLevel == null, PARAMS_ERROR, "空间级别不能为空");
            ThrowUtils.throwIf(spaceType == null, PARAMS_ERROR, "空间类型不能为空");
        }
//        2. 修改数据时
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(PARAMS_ERROR, "空间名称过长");
        }
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(PARAMS_ERROR, "空间级别不存在");
        }
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(PARAMS_ERROR, "空间类型不存在");
        }

    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        Integer spaceLevel = space.getSpaceLevel();
        ThrowUtils.throwIf(spaceLevel == null, PARAMS_ERROR);
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);

        if (enumByValue != null) {
            Long maxSize = enumByValue.getMaxSize();
            Long maxCount = enumByValue.getMaxCount();
            Long spaceMaxSize = space.getMaxSize();
            Long spaceMaxCount = space.getMaxCount();
            if (spaceMaxSize == null) {
                space.setMaxSize(maxSize);
            }
            if (spaceMaxCount == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        ThrowUtils.throwIf(space == null, PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, PARAMS_ERROR);
        if (!userService.isAdmin(loginUser) && !space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(NO_AUTH_ERROR, "权限不够");
        }
    }


}




