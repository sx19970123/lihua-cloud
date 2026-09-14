package com.lihua.system.controller.app;

import com.lihua.system.controller.base.BaseSysNoticeController;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "APP-通知公告")
@RestController
@RequestMapping("app/system/notice")
public class AppSysNoticeController extends BaseSysNoticeController {

}
