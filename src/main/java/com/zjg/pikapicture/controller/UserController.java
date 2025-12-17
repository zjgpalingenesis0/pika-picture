package com.zjg.pikapicture.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zjg.pikapicture.annotation.AuthCheck;
import com.zjg.pikapicture.common.BaseResponse;
import com.zjg.pikapicture.common.DeleteRequest;
import com.zjg.pikapicture.common.ResultUtils;
import com.zjg.pikapicture.exception.ThrowUtils;
import com.zjg.pikapicture.model.dto.user.*;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.model.vo.LoginUserVO;
import com.zjg.pikapicture.model.vo.UserVO;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.zjg.pikapicture.constant.UserConstant.ADMIN_ROLE;
import static com.zjg.pikapicture.exception.ErrorCode.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        //校验输入是否为空
        ThrowUtils.throwIf(userRegisterRequest == null, PARAMS_ERROR, "注册必须信息未填完整");
        //获取前端输入内容
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        //调用接口，执行登录操作
        Long result = userService.userRegister(userAccount, userPassword, checkPassword);
        ThrowUtils.throwIf(result == null, SYSTEM_ERROR, "注册失败");
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        //校验
        ThrowUtils.throwIf(userLoginRequest == null, PARAMS_ERROR);
        //取到前端输入
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        //登录
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        ThrowUtils.throwIf(loginUserVO == null, SYSTEM_ERROR, "登录失败");
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/current")
    public BaseResponse<LoginUserVO> getCurrentUser(HttpServletRequest request) {
        //获取当前用户
        User currentUser = userService.getCurrentUser(request);
        //返回脱敏信息
        LoginUserVO result = userService.getLoginUserVO(currentUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result, "注销成功");
    }

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
//        1. 判断输入   PARAMS_ERROR
        ThrowUtils.throwIf(userAddRequest == null, PARAMS_ERROR);
//        2. 创建用户对象，把userAddRequest复制到用户对象中
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
//        3. 设置一个默认密码，加密，给用户对象设置上
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
//        4. 插入数据库，userService.save
        boolean result = userService.save(user);
//        5. 如果插入失败  OPERATION_ERROR
        ThrowUtils.throwIf(!result, OPERATION_ERROR, "创建用户失败");
//        6. 返回id
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据用户id查询用户
     *
     * @param id
     * @return
     */
    @GetMapping("/get/user")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<UserVO> getUserById(long id) {
        //校验
        ThrowUtils.throwIf(id <= 0, PARAMS_ERROR);
        //根据id获取用户
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, NOT_FOUND_ERROR);
        //脱敏
        UserVO userVO = userService.getUserVO(user);
        return ResultUtils.success(userVO);
    }

    /**
     * 删除用户
     *
     * @param DeleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest DeleteRequest) {
        //校验
        ThrowUtils.throwIf(DeleteRequest == null || DeleteRequest.getId() <= 0, PARAMS_ERROR);
        //删除用户
        boolean result = userService.removeById(DeleteRequest.getId());
        ThrowUtils.throwIf(!result, OPERATION_ERROR, "删除失败");
        return ResultUtils.success(result);
    }

    /**
     * 更新用户信息
     *
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        //校验
        ThrowUtils.throwIf(userUpdateRequest == null, PARAMS_ERROR);
        //更新
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, OPERATION_ERROR, "更新失败");
        return ResultUtils.success(result);
    }

    /**
     * 分页查询
     *
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
//        1. 判空
        ThrowUtils.throwIf(userQueryRequest == null, PARAMS_ERROR);
//        2. 取current，pagesize参数，long类型
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
//        3. userService.page   把对象属性转成queryWrapper条件
        QueryWrapper<User> queryWrapper = userService.getQueryWrapper(userQueryRequest);
        Page<User> userPage = userService.page(new Page<>(current, pageSize), queryWrapper);
//        4. 创建Page<UserVO>对象，获取脱敏用户信息列表
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
}
