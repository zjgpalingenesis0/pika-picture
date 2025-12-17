package com.zjg.pikapicture.aop;

import com.zjg.pikapicture.annotation.AuthCheck;
import com.zjg.pikapicture.exception.BusinessException;
import com.zjg.pikapicture.exception.ErrorCode;
import com.zjg.pikapicture.model.entity.User;
import com.zjg.pikapicture.model.enums.UserRoleEnum;
import com.zjg.pikapicture.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static com.zjg.pikapicture.exception.ErrorCode.NO_AUTH_ERROR;

@Component
@Aspect
@Slf4j
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        //获取必须角色
        String mustRole = authCheck.mustRole();
        //获取当前请求对象
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        //获取登录用户
        User loginUser = userService.getCurrentUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByVaule(mustRole);
        //不需要权限放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        //必须有权限才能通过
        //获取当前用户的权限
        String userRole = loginUser.getUserRole();
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByVaule(userRole);
        //没有权限拒绝
        if (userRoleEnum == null) {
            log.info("没有权限");
            throw new BusinessException(NO_AUTH_ERROR);
        }
        //要求必须有管理员权限，但用户没有管理员权限，拒绝
        if (!UserRoleEnum.ADMIN.equals(userRoleEnum) && UserRoleEnum.ADMIN.equals(mustRoleEnum)) {
            log.info("必须有管理员权限");
            throw new BusinessException(NO_AUTH_ERROR);
        }
        //通过权限校验，放行
        return joinPoint.proceed();

    }
}
