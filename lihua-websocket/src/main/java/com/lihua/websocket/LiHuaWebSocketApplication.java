package com.lihua.websocket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * WS 连接服务（cloud 第六服务，与 mono 的 lihua-websocket 纯库形态分叉——cloud 侧自带服务启动引导，
 * 同 lihua-system 双仓关系）：无库基础设施服务，不落表。
 * 持有 /ws-connect 连接，订阅 Redis pub/sub 扇出推送本地会话；可多实例部署水平扩容
 */
@EnableAsync(proxyTargetClass = true)
@SpringBootApplication
@ComponentScan({"com.lihua.**"})
public class LiHuaWebSocketApplication {
    public static void main(String[] args) {
        SpringApplication.run(LiHuaWebSocketApplication.class, args);
    }
}
