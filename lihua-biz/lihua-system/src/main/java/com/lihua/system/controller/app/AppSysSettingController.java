package com.lihua.system.controller.app;

import com.lihua.system.controller.base.BaseSysSettingController;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "APP-系统设置")
@RestController
@RequestMapping("app/system/setting")
public class AppSysSettingController extends BaseSysSettingController {

}
