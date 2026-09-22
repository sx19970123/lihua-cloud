package com.lihua.system.controller;

import com.lihua.client.model.RegisterUserModel;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.model.response.basecontroller.ApiResponseController;
import com.lihua.security.model.CurrentUser;
import com.lihua.security.model.LoginUserSession;
import com.lihua.system.service.SysUserAuthService;
import com.lihua.web.annotation.InternalOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证服务远程调用接口，仅远程调用使用")
@RestController
@RequestMapping("system/user/auth")
@Slf4j
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
    // 携带请求体必须 POST（GET+body 靠栈容忍不可依赖，CR-90）；客户端 @PostExchange 同步，签名材料含 HTTP method 双端一致
    @PostMapping("queryLoginUserProfile")
    @InternalOnly
    public ApiResponseModel<LoginUserSession> queryLoginUserProfile(@RequestBody LoginUserSession loginUserSession) {
        return success(sysUserAuthService.queryLoginUserProfile(loginUserSession));
    }

    @Operation(summary = "用户注册")
    @PostMapping("register")
    @InternalOnly
    ApiResponseModel<String> register(@RequestBody RegisterUserModel registerUserModel) {
        return success(sysUserAuthService.register(registerUserModel));
    }

}
