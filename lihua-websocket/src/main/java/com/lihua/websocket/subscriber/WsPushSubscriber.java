package com.lihua.websocket.subscriber;

import com.lihua.cache.enums.RedisTopicEnum;
import com.lihua.cache.websocket.WsPushMessage;
import com.lihua.common.enums.WebSocketMsgTypeEnum;
import com.lihua.websocket.manager.WebSocketManager;
import com.lihua.websocket.model.WebSocketResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * WS 推送的 Redis pub/sub 订阅端：所有持有 WS 连接的实例都订阅 {@link RedisTopicEnum#WS_PUSH}，
 * 收到投递后各自查本地会话表、只推送本实例持有的连接——业务方一次投递即完成多实例扇出
 * <p>
 * mono 形态：WS 嵌在 admin 进程内，自发自收（publish 经 Redis 回本进程订阅器）；
 * cloud 形态：独立 lihua-ws 服务，可多实例水平扩展
 * <p>
 * 推送为 fire-and-forget：回调内异常仅记日志，不重试不确认（可靠性靠持久层 + 客户端拉取兜底）
 */
@Slf4j
@Component
public class WsPushSubscriber {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private WebSocketManager webSocketManager;

    @PostConstruct
    public void subscribe() {
        redissonClient.getTopic(RedisTopicEnum.WS_PUSH.getValue())
                .addListener(WsPushMessage.class, (channel, msg) -> {
                    try {
                        WebSocketResult<Object> result = new WebSocketResult<>(
                                WebSocketMsgTypeEnum.valueOf(msg.getType()), msg.getData());
                        if (msg.getUserIdList() == null) {
                            // 全员广播
                            webSocketManager.send(result);
                        } else {
                            // 定向推送本实例持有的连接（会话表无此用户时静默跳过，属扇出常态）
                            webSocketManager.send(msg.getUserIdList(), result);
                        }
                    } catch (Exception e) {
                        log.error("WS 推送订阅消息处理失败: {}", e.getMessage(), e);
                    }
                });
        log.info("WS 推送订阅完成: topic={}", RedisTopicEnum.WS_PUSH.getValue());
    }
}
