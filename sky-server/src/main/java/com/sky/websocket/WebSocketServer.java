package com.sky.websocket;

import com.sky.config.WebSocketConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket服务
 */
@Component
@ServerEndpoint(value = "/ws/{sid}", configurator = WebSocketConfiguration.class)
@Slf4j
public class WebSocketServer {

    //存放会话对象
    private static Map<String, Session> sessionMap = new HashMap();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        log.info("[WebSocket]: 客户端：{}建立连接", sid);
        sessionMap.put(sid, session);

        // 获取握手时存下的 HTTP 请求头
        Map<String, Object> userProperties = session.getUserProperties();
        @SuppressWarnings("unchecked")
        Map<String, List<String>> headers = (Map<String, List<String>>) userProperties.get("headers");
        if (headers != null) {
            headers.forEach((k, v) -> log.info("{}: {}", k, v));
        }
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                sendToAllClient("{message: hello from springboot application}");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("[WebSocket]: 收到来自客户端：{}的信息:{}", sid, message);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        log.info("[WebSocket]: 客户端{}连接断开", sid);
        sessionMap.remove(sid);
    }

    /**
     * 群发
     *
     * @param message
     */
    public void sendToAllClient(String message) {
        log.info("服务端向客户端发送消息，message: {}", message);
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                //服务器向客户端发送消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
