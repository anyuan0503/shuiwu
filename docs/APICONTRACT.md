# 水务监测分析系统 · API 契约文档

> 三端协作统一契约：Spring Boot 后端 / 前端 Vue3 / Python FastAPI AI 微服务。
> 命名空间已知，请严格按此契约实现，勿擅自变更路径或字段。

## 0. 通用约定

- 后端端口 `8080`，前端 `5173`(dev) / `80`(prod via nginx)，AI 服务端口 `8000`。
- 认证方式：`Authorization: Bearer <JWT>`。
- 统一响应体（后端所有接口）：
```json
{ "code": 200, "message": "success", "data": {} }
```
  `code==200` 成功；`401` 未认证；`403` 无权限；业务错误用 `500`/`400`。
- 分页请求参数：`page`(从1)、`size`，分页响应：
```json
{ "code":200, "message":"success",
  "data": { "list":[], "total":0, "page":1, "size":10 } }
```
- 前端对响应统一走 `axios` 拦截，直接读取 `data`。
- 所有时间字段格式 `yyyy-MM-dd HH:mm:ss`。

## 1. 认证模块 `/api/auth`

| 方法 | 路径 | 说明 | 入参 | 返回 data |
|---|---|---|---|---|
| POST | /api/auth/login | 登录 | `{username,password}` | `{token, user:{id,username,realName,roleCode,roleName}, menus:[{id,name,path,component,icon,children:[]}]}` |
| GET | /api/auth/me | 当前用户信息 | - | 同 user |
| POST | /api/auth/logout | 登出 | - | - |
| PUT | /api/auth/password | 修改密码 | `{oldPassword,newPassword}` | - |

## 2. 设备台账 `/api/device`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/device/page | 分页，参数：`page,size,keyword,deviceType,status,area` |
| GET | /api/device/list | 全量列表（无分页，用于下拉/大屏） |
| GET | /api/device/{id} | 详情 |
| POST | /api/device | 新增 `{deviceNo,deviceName,deviceType,model,location,area,manufacturer,unit,installDate,status,lon,lat,remark}` |
| PUT | /api/device | 更新 |
| DELETE | /api/device/{id} | 删除 |
| GET | /api/device/onlineCount | 在线统计 `[{deviceType, total, online, offline, fault}]` |

设备类型枚举：`pressure/flow/quality/level`；状态：`1在线 0离线 2故障 3停用`。

## 3. 实时监测 `/api/monitor`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/monitor/realtime | 最新值列表 `[{deviceId,deviceName,deviceType,pressure,flow,ph,turbidity,residualCl,temperature,level,qualityStatus,updateTime}]` |
| GET | /api/monitor/trend | 趋势曲线，参数 `deviceId,type,startTime,endTime,pointCount`；`type`∈`pressure/flow/turbidity/ph/residualCl/level`。返回 `[{time,value}]`（分表路由+抽稀） |
| POST | /api/monitor/data | （定时采集/模拟）单条写入 `{deviceId,dataTime,pressure,flow,ph,turbidity,residualCl,temperature,level}` |
| GET | /api/monitor/stat | 统计 `{pressureAvg,flowTotal,worstQuality}` 等 |

## 4. 分级告警 `/api/alarm`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/alarm/page | 分页，参数 `page,size,alarmLevel,alarmStatus,deviceId,startTime,endTime` |
| GET | /api/alarm/rule/list | 规则列表 |
| POST | /api/alarm/rule | 新增规则 `{ruleName,deviceId,monitorField,alarmType,alarmLevel,thresholdMin,thresholdMax,windowMinutes,enabled,remark}` |
| PUT | /api/alarm/rule | 更新规则 |
| DELETE | /api/alarm/rule/{id} | 删除规则 |
| PUT | /api/alarm/handle | 处理告警 `{id, handleResult}` |
| PUT | /api/alarm/ignore | 忽略告警 `{id}` |
| GET | /api/alarm/summary | 告警汇总 `{total, level1, level2, level3, unhandled}` |
| GET | /api/alarm/trend | 近N天告警趋势 `[{date, count, level3}]` |

`alarmLevel`：`1提示 2警告 3严重`；`alarmStatus`：`0未处理 1处理中 2已处理 3已忽略`。

## 5. AI 智能分析 `/api/ai`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/ai/nlsql | 自然语言查数 `{question}` → `{rawSql, answer, usedEngine, chartConfig, tableData, }`；`chartConfig` 供前端选图渲染 |
| GET | /api/ai/log/page | AI 日志分页，参数 `page,size,logType,keyword` |
| POST | /api/ai/clean | 触发数据清洗，参数 `{deviceId?}` → `{cleaned,repaired,removed}` |
| GET | /api/ai/anomaly | 管网异常分析，参数 `deviceId?,topN` → `[{deviceName, field, start, end, score, desc}]` |

## 6. 报表 `/api/report`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/report/page | 分页 |
| POST | /api/report/generate | 生成报表 `{reportType,deviceId,startTime,endTime}` → `{id, filePath}` |
| GET | /api/report/{id}/download | 下载（返回文件流） |
| GET | /api/report/summary | 统计概览 |

## 7. 系统管理 `/api/system`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/system/user/page | 用户分页 `page,size,keyword,roleId` |
| POST | /api/system/user | 新增 `{username,password,realName,phone,email,roleIds,status}` |
| PUT | /api/system/user | 更新 |
| PUT | /api/system/user/{id}/status | 启停 `{status}` |
| DELETE | /api/system/user/{id} | 删除 |
| GET | /api/system/role/list | 角色列表 |
| GET | /api/system/menu/tree | 菜单树 |
| GET | /api/system/stat | 平台概览 `{userCount,deviceCount,alarmUnhandled,onlineRatio, todayAlarm}` |

## 8. WebSocket 实时推送 `/ws`

- 连接地址：`ws://host:8080/ws?token=<JWT>`，握手后立即推送初始实时数据。
- 推送消息格式（JSON 文本帧）：
```json
{ "type":"realtime", "data":[{ ...最新监测值列表... }] }
{ "type":"alarm", "data":{ "id":1, "deviceName":"泵站1压力计", "alarmLevel":3, "alarmDesc":"压力超上限", "alarmTime":"..." } }
{ "type":"heartbeat", "data":"pong" }
```

## 9. AI 微服务对接（后端 → FastAPI `http://127.0.0.1:8000`，内部接口不对外）

| 方法 | 路径 | 说明 | 入参 | 返回 |
|---|---|---|---|---|
| POST | /ai/nlsql | NL2SQL | `{question, tables:[{name,desc,fields:[...]}], credentials:{host,port,user,password,database}}` | `{success, usedEngine, rawSql, answer, chartConfig:{chartType,title,x,y,series[]}, tableData:[{}]}` |
| POST | /ai/clean | 清洗 | `{rows:[{...字段为监测点数值}]}` | `{cleaned,repaired,removed, detail:{index,reason}[]}` |
| POST | /ai/anomaly | 异常分析 | `{series:[{deviceId,deviceName,field,data:[[ts,value]...]}]}` | `{anomalies:[{deviceId,field,start,end,score,desc}]}` |

> NL2SQL 采用"规则+规则引擎+可选LLM"混合策略：无 LLM key 时降级为内置 Prompt 规则解析（RuleCenter），保证离线可用。

## 10. 前端路由/页面与菜单 component 对应

```
/dashboard      → dashboard/index
/device/ledger  → device/ledger
/monitor        → monitor/index
/alarm          → alarm/index
/ai/analyze     → ai/analyze
/ai/log         → ai/log
/report         → report/index
/bigscreen      → bigscreen/index  (DataV 大屏, 独立无菜单布局/全屏)
/system/user    → system/user
/system/role    → system/role
/system/device  → system/device
```

## 11. 默认账号

- admin / 123456（管理员）
- operator / 123456（运维员）
- viewer / 123456（观察员）