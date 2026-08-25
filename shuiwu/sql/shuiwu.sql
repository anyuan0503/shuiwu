-- =====================================================================
--  水务监测分析系统 数据库初始化脚本 (MySQL 5.7 / utf8mb4)
--  说明：
--  1) 用户 / 角色 / 菜单实现 RBAC 三级角色权限
--  2) 管网时序监测数据(压力/流量/水质/液位)采用"按月分表"策略，
--     物理表命名规则 monitor_data_YYYYMM，由后端分表路由动态读写
--  3) 基础业务表与月分表模板见下文
-- =====================================================================
DROP DATABASE IF EXISTS shuiwu;
CREATE DATABASE shuiwu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE shuiwu;
-- 令 mysql 客户端按 UTF-8 解释初始化脚本，避免中文以 latin1 误存为乱码
SET NAMES utf8mb4;

-- ---------------------------
-- 1. 用户表
-- ---------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL COMMENT '登录名',
    password     VARCHAR(128) NOT NULL COMMENT 'BCrypt加密密码',
    real_name    VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    phone        VARCHAR(20)  DEFAULT NULL,
    email        VARCHAR(100) DEFAULT NULL,
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='系统用户';

-- ---------------------------
-- 2. 角色表
-- ---------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名',
    role_code   VARCHAR(50) NOT NULL COMMENT '角色编码 ADMIN/OPERATOR/VIEWER',
    role_desc   VARCHAR(200) DEFAULT NULL,
    status      TINYINT     NOT NULL DEFAULT 1,
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='角色表';

-- ---------------------------
-- 3. 用户角色表
-- ---------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE=InnoDB COMMENT='用户角色关联';

-- ---------------------------
-- 4. 菜单/权限表 (RBAC资源)
-- ---------------------------
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    menu_name   VARCHAR(50)  NOT NULL,
    menu_type   TINYINT      NOT NULL DEFAULT 1 COMMENT '1目录 2菜单 3按钮',
    path        VARCHAR(200) DEFAULT NULL,
    component   VARCHAR(200) DEFAULT NULL,
    perm        VARCHAR(100) DEFAULT NULL COMMENT '权限标识 sys:user:list',
    icon        VARCHAR(100) DEFAULT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    visible     TINYINT      NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='菜单权限表';

-- ---------------------------
-- 5. 角色菜单表
-- ---------------------------
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_role (role_id)
) ENGINE=InnoDB COMMENT='角色菜单关联';

-- ---------------------------
-- 6. 设备台账表
-- ---------------------------
DROP TABLE IF EXISTS device;
CREATE TABLE device (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    device_no    VARCHAR(50)  NOT NULL COMMENT '设备编号',
    device_name  VARCHAR(100) NOT NULL COMMENT '设备名称',
    device_type  VARCHAR(30)  NOT NULL COMMENT 'pressure/flow/quality/level',
    model        VARCHAR(100) DEFAULT NULL COMMENT '型号',
    location     VARCHAR(200) DEFAULT NULL COMMENT '安装位置',
    area         VARCHAR(100) DEFAULT NULL COMMENT '所属片区/水厂',
    manufacturer VARCHAR(100) DEFAULT NULL,
    unit         VARCHAR(20)  DEFAULT NULL COMMENT '计量单位 MPa/m3/h/NTU',
    install_date DATE         DEFAULT NULL,
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1在线 0离线 2故障 3停用',
    lon          DECIMAL(10,6) DEFAULT NULL,
    lat          DECIMAL(10,6) DEFAULT NULL,
    remark       VARCHAR(500) DEFAULT NULL,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_type (device_type)
) ENGINE=InnoDB COMMENT='设备台账';

-- ---------------------------
-- 7. 告警规则表
-- ---------------------------
DROP TABLE IF EXISTS alarm_rule;
CREATE TABLE alarm_rule (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    rule_name     VARCHAR(100) NOT NULL,
    device_id     BIGINT       DEFAULT NULL COMMENT '空=全局规则',
    monitor_field VARCHAR(50)  NOT NULL COMMENT 'field: pressure/flow/turbidity/cl_ph...',
    alarm_type    VARCHAR(30)  NOT NULL COMMENT 'threshold/trend/anomaly',
    alarm_level   TINYINT      NOT NULL DEFAULT 1 COMMENT '1提示 2警告 3严重',
    threshold_min DECIMAL(12,4) DEFAULT NULL,
    threshold_max DECIMAL(12,4) DEFAULT NULL,
    window_minutes INT         NOT NULL DEFAULT 5 COMMENT '持续窗口判断',
    enabled       TINYINT      NOT NULL DEFAULT 1,
    remark        VARCHAR(500) DEFAULT NULL,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='告警规则';

-- ---------------------------
-- 8. 告警记录表
-- ---------------------------
DROP TABLE IF EXISTS alarm;
CREATE TABLE alarm (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    alarm_no      VARCHAR(50)  NOT NULL COMMENT '告警单号',
    device_id     BIGINT       NOT NULL,
    rule_id       BIGINT       DEFAULT NULL,
    alarm_type    VARCHAR(30)  NOT NULL,
    alarm_level   TINYINT      NOT NULL DEFAULT 1,
    alarm_desc    VARCHAR(500) NOT NULL,
    current_value DECIMAL(12,4) DEFAULT NULL,
    alarm_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '0未处理 1处理中 2已处理 3已忽略',
    alarm_time    DATETIME     NOT NULL,
    handle_user   VARCHAR(50)  DEFAULT NULL,
    handle_time   DATETIME     DEFAULT NULL,
    handle_result VARCHAR(500) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_device (device_id),
    KEY idx_status (alarm_status),
    KEY idx_time (alarm_time)
) ENGINE=InnoDB COMMENT='告警记录';

-- ---------------------------
-- 9. AI日志表
-- ---------------------------
DROP TABLE IF EXISTS ai_log;
CREATE TABLE ai_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       DEFAULT NULL,
    user_name   VARCHAR(50)  DEFAULT NULL,
    log_type    VARCHAR(30)  NOT NULL COMMENT 'nlsql/clean/anomaly',
    question    VARCHAR(1000) DEFAULT NULL COMMENT '自然语言问题',
    sql_text    MEDIUMTEXT   DEFAULT NULL COMMENT '生成的SQL',
    answer      MEDIUMTEXT   DEFAULT NULL COMMENT '结果描述',
    ai_engine   VARCHAR(50)  DEFAULT NULL COMMENT '使用的引擎 prompt/rule/llm',
    cost_ms     INT          DEFAULT NULL,
    success     TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE=InnoDB COMMENT='AI分析日志';

-- ---------------------------
-- 10. 报表表
-- ---------------------------
DROP TABLE IF EXISTS report;
CREATE TABLE report (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    report_name VARCHAR(100) NOT NULL,
    report_type VARCHAR(30)  NOT NULL COMMENT 'daily/weekly/monthly/custom',
    device_id   BIGINT       DEFAULT NULL,
    start_time  DATETIME     DEFAULT NULL,
    end_time    DATETIME     DEFAULT NULL,
    file_path   VARCHAR(300) DEFAULT NULL COMMENT '导出文件相对路径',
    summary     TEXT         DEFAULT NULL,
    create_user VARCHAR(50)  DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='报表';

-- ---------------------------
-- 11. 实时监测最新值缓存表 (用于大屏快速读取)
-- ---------------------------
DROP TABLE IF EXISTS monitor_latest;
CREATE TABLE monitor_latest (
    device_id     BIGINT       NOT NULL,
    device_name   VARCHAR(100) DEFAULT NULL,
    device_type   VARCHAR(30)  DEFAULT NULL,
    pressure      DECIMAL(12,4) DEFAULT NULL,
    flow          DECIMAL(12,4) DEFAULT NULL,
    ph            DECIMAL(12,4) DEFAULT NULL,
    turbidity     DECIMAL(12,4) DEFAULT NULL,
    residual_cl   DECIMAL(12,4) DEFAULT NULL,
    temperature   DECIMAL(12,4) DEFAULT NULL,
    level         DECIMAL(12,4) DEFAULT NULL,
    quality_status VARCHAR(30) DEFAULT NULL COMMENT 'normal/warn/error',
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (device_id)
) ENGINE=InnoDB COMMENT='设备最新监测数据';

-- -------------------------------------------------------------------
-- 时序分表模板：monitor_data_YYYYMM
-- 采用动态分表，后端按 data_time 路由到对应物理表
-- 按月建表脚本(示例)：CALL create_monitor_table('2026','08');
-- -------------------------------------------------------------------
DROP TABLE IF EXISTS monitor_data_202608;
CREATE TABLE monitor_data_202608 (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    device_id   BIGINT       NOT NULL,
    data_time   DATETIME     NOT NULL,
    pressure    DECIMAL(12,4) DEFAULT NULL COMMENT '压力 MPa',
    flow        DECIMAL(12,4) DEFAULT NULL COMMENT '流量 m3/h',
    ph          DECIMAL(12,4) DEFAULT NULL COMMENT 'pH值',
    turbidity   DECIMAL(12,4) DEFAULT NULL COMMENT '浊度 NTU',
    residual_cl DECIMAL(12,4) DEFAULT NULL COMMENT '余氯 mg/L',
    temperature DECIMAL(12,4) DEFAULT NULL COMMENT '温度 °C',
    level       DECIMAL(12,4) DEFAULT NULL COMMENT '液位 m',
    is_clean    TINYINT      NOT NULL DEFAULT 1 COMMENT '清洗标记 0脏数据 1正常 2已修复',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_device_time (device_id, data_time)
) ENGINE=InnoDB COMMENT='监测数据月分表(模板)';

-- =====================================================================
-- 按月自动建表存储过程
-- =====================================================================
DROP PROCEDURE IF EXISTS create_monitor_table;
DELIMITER $$
CREATE PROCEDURE create_monitor_table(IN y CHAR(4), IN m CHAR(2))
BEGIN
    SET @tbl = CONCAT('monitor_data_', y, m);
    SET @sql = CONCAT(
        'CREATE TABLE IF NOT EXISTS ', @tbl, ' (',
        ' id BIGINT NOT NULL AUTO_INCREMENT,',
        ' device_id BIGINT NOT NULL,',
        ' data_time DATETIME NOT NULL,',
        ' pressure DECIMAL(12,4) DEFAULT NULL,',
        ' flow DECIMAL(12,4) DEFAULT NULL,',
        ' ph DECIMAL(12,4) DEFAULT NULL,',
        ' turbidity DECIMAL(12,4) DEFAULT NULL,',
        ' residual_cl DECIMAL(12,4) DEFAULT NULL,',
        ' temperature DECIMAL(12,4) DEFAULT NULL,',
        ' level DECIMAL(12,4) DEFAULT NULL,',
        ' is_clean TINYINT NOT NULL DEFAULT 1,',
        ' create_time DATETIME DEFAULT CURRENT_TIMESTAMP,',
        ' PRIMARY KEY (id),',
        ' KEY idx_device_time (device_id, data_time)',
        ') ENGINE=InnoDB COMMENT=''监测数据月分表'''
    );
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
END$$
DELIMITER ;

-- 为最近几个月建表
CALL create_monitor_table('2026','06');
CALL create_monitor_table('2026','07');
CALL create_monitor_table('2026','08');

-- =====================================================================
-- 初始数据：角色
-- =====================================================================
INSERT INTO sys_role (role_name, role_code, role_desc) VALUES
('水务管理员','ADMIN','拥有系统全部权限，负责用户、设备、规则、告警、报表管理'),
('运维员','OPERATOR','负责设备台账维护、告警处理、数据查看与导出'),
('观察员','VIEWER','仅可查看监测数据、大屏与基础报表');

-- 初始用户密码统一为 123456 (BCrypt)
INSERT INTO sys_user (username, password, real_name, phone) VALUES
('admin',    '$2a$10$QdDtE.QUGN4Q0CzwQVdsCe5Ixk4XRlKEjTbzqAFDZv.WVNcpehvj2', '系统管理员', '13800000000'),
('operator', '$2a$10$QdDtE.QUGN4Q0CzwQVdsCe5Ixk4XRlKEjTbzqAFDZv.WVNcpehvj2', '运维人员',   '13800000001'),
('viewer',   '$2a$10$QdDtE.QUGN4Q0CzwQVdsCe5Ixk4XRlKEjTbzqAFDZv.WVNcpehvj2', '观察员',     '13800000002');

INSERT INTO sys_user_role (user_id, role_id) VALUES (1,1),(2,2),(3,3);

-- =====================================================================
-- 初始数据：菜单
-- 权限标识用于接口级 @PreAuthorize 校验
-- =====================================================================
INSERT INTO sys_menu (parent_id, menu_name, menu_type, path, component, perm, icon, sort_order) VALUES
(0,'平台总览',2,'/dashboard','dashboard/index','dashboard:view','Odometer',1),
(0,'设备台账',2,'/device/ledger','device/ledger','device:list','Monitor',2),
(0,'实时监测',2,'/monitor','monitor/index','monitor:view','DataLine',3),
(0,'分级告警',2,'/alarm','alarm/index','alarm:list','Warning',4),
(0,'AI智能分析',1,'/ai',NULL,NULL,'Cpu',5),
(0,'AI分析',2,'/ai/analyze','ai/analyze','ai:analyze','MagicStick',51),
(0,'AI日志',2,'/ai/log','ai/log','ai:log','Document',52),
(0,'报表管理',2,'/report','report/index','report:view','Tickets',6),
(0,'可视化大屏',2,'/bigscreen','bigscreen/index','bigscreen:view','TrendCharts',7),
(0,'系统管理',1,'/system',NULL,NULL,'Setting',90),
(0,'用户管理',2,'/system/user','system/user','sys:user:list','User',91),
(0,'角色管理',2,'/system/role','system/role','sys:role:list','Avatar',92),
(0,'设备管理',2,'/system/device','system/device','device:manage','Cpu',93);

-- 角色-菜单授权：ADMIN全量，OPERATOR功能性，VIEWER只读
INSERT INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu;
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2,1),(2,2),(2,3),(2,4),(2,5),(2,6),(2,7),(2,8),(2,9);
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3,1),(3,3),(3,8),(3,9);

-- =====================================================================
-- 初始数据：示例监测设备（便于演示大屏/趋势/告警，运行时监控任务会持续模拟采集）
-- =====================================================================
INSERT INTO device (device_no, device_name, device_type, model, location, area, manufacturer, unit, install_date, status, lon, lat, remark) VALUES
('PT-001', '一号泵站出水压力计', 'pressure', 'E+H PMC51', '一号泵站出水总管', '东城区水厂', 'Endress+Hauser', 'MPa',  '2023-05-12', 1, 120.2058, 30.2888, '泵站出口主管压力'),
('PT-002', '二号泵站出水压力计', 'pressure', 'E+H PMC51', '二号泵站出水总管', '西城区水厂', 'Endress+Hauser', 'MPa',  '2023-05-12', 1, 120.1721, 30.2732, '泵站出口主管压力'),
('FT-001', '东区主干管流量计',   'flow',     '西门子 MAG5100', '东区主干管(PE800)', '东城区水厂', 'Siemens',        'm3/h','2023-06-01', 1, 120.2310, 30.2910, '分区计量'),
('FT-002', '西区供水流量计',     'flow',     '西门子 MAG5100', '西区配水管网',     '西城区水厂', 'Siemens',        'm3/h','2023-06-01', 1, 120.1488, 30.2611, '分区计量'),
('QT-001', '净水厂出厂水质分析仪','quality', '哈希 UV-254',   '净水厂出厂口',     '中心水厂',   'HACH',           'NTU', '2023-07-20', 1, 120.1876, 30.2564, '浊度/pH/余氯在线监测'),
('LT-001', '高位水池液位计',     'level',    '雷达液位 VEGA',  '高位水池(标高56m)', '东城区水厂', 'VEGA',           'm',   '2023-08-15', 1, 120.2193, 30.3053, '蓄水池液位');