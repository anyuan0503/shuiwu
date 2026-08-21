package com.water.common.constants;

/**
 * 通用常量
 */
public interface CommonConstants {

    String TOKEN_HEADER = "Authorization";
    String TOKEN_PREFIX = "Bearer ";

    /** 角色编码 */
    String ROLE_ADMIN = "ADMIN";
    String ROLE_OPERATOR = "OPERATOR";
    String ROLE_VIEWER = "VIEWER";

    /** 设备状态 */
    int DEVICE_ONLINE = 1;
    int DEVICE_OFFLINE = 0;
    int DEVICE_FAULT = 2;
    int DEVICE_STOP = 3;

    /** 告警状态 */
    int ALARM_UNHANDLED = 0;
    int ALARM_HANDLING = 1;
    int ALARM_HANDLED = 2;
    int ALARM_IGNORED = 3;

    /** WebSocket 消息类型 */
    String WS_TYPE_REALTIME = "realtime";
    String WS_TYPE_ALARM = "alarm";
    String WS_TYPE_HEARTBEAT = "heartbeat";

    String SYSTEM_ADMIN = "admin";
}