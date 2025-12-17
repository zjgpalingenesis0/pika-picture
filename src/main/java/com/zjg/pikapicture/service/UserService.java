package com.zjg.pikapicture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zjg.pikapicture.model.dto.user.UserQueryRequest;
import com.zjg.pikapicture.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjg.pikapicture.model.vo.LoginUserVO;
import com.zjg.pikapicture.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-12-01 22:12:38
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userAccount  账户
     * @param userPassword  密码
     * @param checkPassword  验证码
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount  账号
     * @param userPassword   密码
     * @param request 请求
     * @return  用户脱敏信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 加密方法
     * @param userPassword  用户密码
     * @return  加密后密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取登录用户安全信息
     * @param loginUser  登录用户
     * @return  脱敏信息
     */
    LoginUserVO getLoginUserVO(User loginUser);

    /**
     * 获取当前登录用户信息
     * @param request  请求
     * @return  用户信息
     */
    User getCurrentUser(HttpServletRequest request);

    /**
     * 判断是否登录
     * @param request 请求
     * @return 是否成功
     */
    User isLogin(HttpServletRequest request);

    /**
     * 用户注销
     * @param request  请求
     * @return 是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后用户信息
     * @param user 前端输入的要查询用户
     * @return 脱敏后查询用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后用户信息列表
     * @param userList  用户列表
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     * @param userQueryRequest  查询条件
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 判断用户是否为管理员
     * @param loginUser
     * @return
     */
    boolean isAdmin(User loginUser);
}
