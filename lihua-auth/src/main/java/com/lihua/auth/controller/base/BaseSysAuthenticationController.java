package com.lihua.auth.controller.base;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.spring.plugins.secondary.SecondaryVerificationApplication;
import com.lihua.auth.model.dto.SysLoginUserDTO;
import com.lihua.auth.model.dto.SysRegisterDTO;
import com.lihua.auth.service.SysAuthenticationService;
import com.lihua.client.facade.SysSettingClientFacade;
import com.lihua.common.enums.ResultCodeEnum;
import com.lihua.common.model.response.ApiResponseModel;
import com.lihua.common.model.response.basecontroller.ApiResponseController;
import com.lihua.log.annotation.Log;
import com.lihua.log.enums.LogTypeEnum;
import com.lihua.security.model.LoginUserSession;
import com.lihua.web.annotation.PreventDuplicateSubmit;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 身份验证控制器基类：承载管理版与 App 版共有端点（对外行为双版本一致，差异仅在路由前缀与文档分组）；
 * 登录数据重载存在端侧差异（Web 版刷新 token 有效期），由各子类自行声明
 */
public abstract class BaseSysAuthenticationController extends ApiResponseController {

    @Resource
    protected SysAuthenticationService sysAuthenticationService;

    @Resource
    protected SysSettingClientFacade sysSettingClientFacade;

    @Resource
    protected ImageCaptchaApplication imageCaptchaApplication;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录")
    @PostMapping("login")
    @Log(description = "用户登录", type = LogTypeEnum.LOGIN, excludeParams = {"password"}, recordResult = false)
    public ApiResponseModel<String> login(@RequestBody @Valid SysLoginUserDTO loginUserDTO) {
        // 校验验证码
        boolean checked = checkCaptcha(loginUserDTO.getCaptchaVerification());
        if (!checked) {
            return error(ResultCodeEnum.CAPTCHA_ERROR);
        }

        // 1.用户登录
        LoginUserSession loginUserSession = sysAuthenticationService.login(loginUserDTO);
        // 2.生成token
        String token = sysAuthenticationService.cacheAndCreateToken(loginUserSession);
        // 3.检查是否配置了同账号最大同时登录数，超出数量后首先登录的用户会被踢下线
        sysAuthenticationService.checkSameAccount(token);

        return success(ResultCodeEnum.SUCCESS, token);
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册")
    @PostMapping("register")
    @PreventDuplicateSubmit
    @Log(description = "用户注册", type = LogTypeEnum.REGISTER, excludeParams = {"password", "confirmPassword"}, recordResult = false)
    public ApiResponseModel<String> register(@RequestBody @Valid SysRegisterDTO sysRegisterDTO) {
        // 校验验证码
        boolean checked = checkCaptcha(sysRegisterDTO.getCaptchaVerification());
        if (!checked) {
            return error(ResultCodeEnum.CAPTCHA_ERROR);
        }

        String password = sysRegisterDTO.getPassword();
        String confirmPassword = sysRegisterDTO.getConfirmPassword();

        // 校验两次密码输入是否相同
        if (!password.equals(confirmPassword)) {
            return error(ResultCodeEnum.ERROR, "两次输入的密码不一致");
        }

        // 注册
        return sysAuthenticationService.register(sysRegisterDTO.getUsername(), password);
    }

    @Operation(summary = "获取一次性令牌")
    @GetMapping("onceToken")
    public ApiResponseModel<String> getOnceToken() {
        return success(sysAuthenticationService.getOnceToken());
    }

    // 校验验证码
    private boolean checkCaptcha(String captchaVerification) {
        ApiResponseModel<Boolean> responseModel = sysSettingClientFacade.enableCaptcha();

        // 验证码开关关闭时直接放行；开启、开关值缺失或远程配置读取失败（从严降级）时走二次校验
        // 枚举常量为 Integer，须 equals 值比较（== 为引用比较，200 超出 Integer 缓存必不等）
        if (ResultCodeEnum.SUCCESS.getCode().equals(responseModel.getCode()) && Boolean.FALSE.equals(responseModel.getData())) {
            return true;
        }

        // 远程配置读取降级（system 不可达，captchaFallback 已吞异常返回 503 响应体）：
        // 登录链后续 RPC 同样不可用（checkSameAccount 对 sysSetting 熔断无 fallback，
        // 强行继续只会以 501 收场），直接以 503 语义快速失败
        if (ResultCodeEnum.SERVER_BAD_ERROR.getCode().equals(responseModel.getCode())) {
            throw new InternalAuthenticationServiceException(ResultCodeEnum.SERVER_BAD_ERROR.getDefaultMsg());
        }

        // 未携带验证码令牌直接判定失败（二次校验对空参抛 NPE）
        if (captchaVerification == null || captchaVerification.isEmpty()) {
            return false;
        }

        if (imageCaptchaApplication instanceof SecondaryVerificationApplication) {
            return ((SecondaryVerificationApplication) imageCaptchaApplication).secondaryVerification(captchaVerification);
        }

        return false;
    }
}
