package com.zjg.pikapicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjg.pikapicture.model.dto.spaceuser.SpaceUserAddRequest;
import com.zjg.pikapicture.model.dto.spaceuser.SpaceUserEditRequest;
import com.zjg.pikapicture.model.dto.spaceuser.SpaceUserQueryRequest;
import com.zjg.pikapicture.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjg.pikapicture.model.vo.SpaceUserVO;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-12-18 14:36:28
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加空间成员
     * @param spaceUserAddRequest
     * @return
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员对象
     * @param spaceUser
     * @param add 判断是在创建，还是编辑
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 将查询请求对象转为查询封装对象
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 查询单个封装类
     * @param spaceUser
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 查询封装类列表
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 编辑空间对象角色
     * @param spaceUserEditRequest
     * @return
     */
    Boolean editSpaceUser(SpaceUserEditRequest spaceUserEditRequest);

}
