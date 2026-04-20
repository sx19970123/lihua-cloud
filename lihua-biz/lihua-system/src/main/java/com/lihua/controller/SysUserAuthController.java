package com.lihua.controller;

import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.model.response.basecontroller.ApiResponseController;
import com.lihua.security.model.CurrentUser;
import com.lihua.service.SysUserAuthService;
import com.lihua.web.annotation.InternalOnly;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("system/user/auth")
public class SysUserAuthController extends ApiResponseController {

    @Resource
    private SysUserAuthService sysUserAuthService;

    @Operation(summary = "根据用户名查询用户信息（仅内部服务远程调用）")
    @GetMapping("queryUserByUsername/{username}")
    @InternalOnly
    public ApiResponseModel<CurrentUser> queryUserByUsername(@PathVariable("username") String username) {
        return success(sysUserAuthService.queryUserByUsername(username));
    }

}
