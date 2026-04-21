package com.lihua.controller;

import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.model.response.basecontroller.ApiResponseController;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;
import com.lihua.service.SysUserAuthService;
import com.lihua.web.annotation.InternalOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证服务远程调用接口，仅远程调用使用")
@RestController
@RequestMapping("system/user/auth")
public class SysUserAuthController extends ApiResponseController {

    @Resource
    private SysUserAuthService sysUserAuthService;

    @Operation(summary = "查询登录用户信息")
    @GetMapping("loginSelect/{username}")
    @InternalOnly
    public ApiResponseModel<CurrentUser> loginSelect(@PathVariable("username") String username) {
        return success(sysUserAuthService.loginSelect(username));
    }

    @Operation(summary = "查询登录用户上下文信息")
    @GetMapping("queryLoginUserProfile")
    @InternalOnly
    public ApiResponseModel<LoginUserSession> queryLoginUserProfile(@RequestBody LoginUserSession loginUserSession) {
        return success(sysUserAuthService.queryLoginUserProfile(loginUserSession));
    }

}
