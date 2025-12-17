package com.zjg.pikapicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjg.pikapicture.model.dto.picture.PictureQueryRequest;
import com.zjg.pikapicture.model.dto.space.SpaceAddRequest;
import com.zjg.pikapicture.model.dto.space.SpaceQueryRequest;
import com.zjg.pikapicture.model.entity.Picture;
import com.zjg.pikapicture.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.model.vo.PictureVO;
import com.zjg.pikapicture.model.vo.SpaceVO;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author Lenovo
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-12-13 13:40:09
*/
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    SpaceVO getSpaceVO(Space space);


    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage);

    void validSpace(Space space, boolean add);

    /**
     * 根据空间级别填充空间对象
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 检查私有空间权限
     * @param space
     * @param loginUser
     */
    void checkSpaceAuth(Space space, User loginUser);
}
