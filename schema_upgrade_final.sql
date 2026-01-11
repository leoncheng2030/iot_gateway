-- =============================================
-- IoT Gateway 数据库架构升级脚本（完整版）
-- 基于 iot_gateway.sql 实际状态生成
-- 功能：将旧的 register_mapping 架构迁移到新的 property_mapping + address_config 架构
-- 特性：幂等性设计，可重复执行
-- 日期：2026-01-11
-- =============================================

-- =============================================
-- 第一部分：创建产品级映射表（如果不存在）
-- =============================================

-- 1.1 创建产品属性映射表（物模型关联层）
-- 说明：该表已在 iot_gateway.sql 中定义，此处使用 IF NOT EXISTS 确保幂等性
CREATE TABLE IF NOT EXISTS `iot_product_property_mapping` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `product_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '产品ID',
  `thing_model_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物模型属性ID',
  `identifier` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '属性标识符（冗余字段，便于查询）',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `sort_code` int NULL DEFAULT 100 COMMENT '排序',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_product_property`(`product_id` ASC, `identifier` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_thing_model_id`(`thing_model_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '产品属性映射表（物模型关联）' ROW_FORMAT = Dynamic;

-- 1.2 创建产品地址配置表（协议配置层）
-- 说明：该表已在 iot_gateway.sql 中定义，此处使用 IF NOT EXISTS 确保幂等性
CREATE TABLE IF NOT EXISTS `iot_product_address_config` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `mapping_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联产品属性映射表ID',
  `protocol_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '协议类型：S7/MODBUS_TCP/OPC_UA/BACNET/FINS/MC',
  `device_address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备地址（协议特定格式，如：DB1.DBW0、40001、ns=2;s=Temp）',
  `data_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '数据类型：int/float/bool/string/word/dword/real',
  `value_multiplier` decimal(10, 4) NULL DEFAULT 1.0000 COMMENT '数值倍率（缩放系数）',
  `value_offset` decimal(10, 4) NULL DEFAULT 0.0000 COMMENT '数值偏移',
  `byte_order` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'BIG_ENDIAN' COMMENT '字节序：BIG_ENDIAN/LITTLE_ENDIAN',
  `ext_config` json NULL COMMENT '协议特有参数配置（JSON格式）',
  `polling_interval` int NULL DEFAULT 0 COMMENT '采集间隔(ms)，0表示使用设备默认',
  `timeout` int NULL DEFAULT 3000 COMMENT '超时时间(ms)',
  `retry_count` int NULL DEFAULT 3 COMMENT '重试次数',
  `enabled` tinyint(1) NULL DEFAULT 1 COMMENT '是否启用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_mapping_protocol`(`mapping_id` ASC, `protocol_type` ASC) USING BTREE,
  INDEX `idx_protocol_type`(`protocol_type` ASC) USING BTREE,
  INDEX `idx_device_address`(`device_address`(100) ASC) USING BTREE,
  INDEX `idx_mapping_id`(`mapping_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '产品级地址配置表（协议特定配置）' ROW_FORMAT = Dynamic;

-- =============================================
-- 第二部分：从旧表迁移数据到产品级新表
-- =============================================

-- 2.1 从产品寄存器映射表迁移到产品属性映射表
-- 说明：iot_product_register_mapping 存在数据，需要迁移到新架构
INSERT IGNORE INTO `iot_product_property_mapping` 
  (`id`, `product_id`, `thing_model_id`, `identifier`, `enabled`, `sort_code`, `create_time`, `update_time`)
SELECT 
  CONCAT('pmap_', ID) AS id,
  PRODUCT_ID,
  THING_MODEL_ID,
  IDENTIFIER,
  ENABLED,
  COALESCE(SORT_CODE, 100) AS sort_code,
  COALESCE(CREATE_TIME, NOW()) AS create_time,
  NOW() AS update_time
FROM `iot_product_register_mapping`
WHERE DELETE_FLAG = 'NOT_DELETE';

-- 2.2 从产品寄存器映射表迁移到产品地址配置表（Modbus协议）
-- 说明：将旧的 register_address, function_code 等字段迁移到 ext_config JSON 中
INSERT IGNORE INTO `iot_product_address_config`
  (`id`, `mapping_id`, `protocol_type`, `device_address`, `data_type`, 
   `value_multiplier`, `value_offset`, `byte_order`, `ext_config`, 
   `polling_interval`, `timeout`, `retry_count`, `enabled`, `remark`, `create_time`, `update_time`)
SELECT 
  CONCAT('paddr_modbus_', r.ID) AS id,
  CONCAT('pmap_', r.ID) AS mapping_id,
  'MODBUS_TCP' AS protocol_type,
  CONCAT('4', LPAD(r.register_address, 4, '0')) AS device_address,
  r.DATA_TYPE,
  r.SCALE_FACTOR,
  r.OFFSET_VALUE,
  r.BYTE_ORDER,
  JSON_OBJECT(
    'registerAddress', CAST(r.register_address AS UNSIGNED),
    'functionCode', r.FUNCTION_CODE,
    'slaveAddress', 1,
    'bitIndex', r.BIT_INDEX
  ) AS ext_config,
  0 AS polling_interval,
  3000 AS timeout,
  3 AS retry_count,
  r.ENABLED,
  CONCAT('[从产品寄存器映射表迁移] ', COALESCE(r.REMARK, '')) AS remark,
  COALESCE(r.CREATE_TIME, NOW()) AS create_time,
  NOW() AS update_time
FROM `iot_product_register_mapping` r
WHERE r.DELETE_FLAG = 'NOT_DELETE';

-- =============================================
-- 第三部分：迁移设备级 Modbus 专用字段到 ext_config
-- =============================================

-- 3.1 将 iot_device_address_config 表中 Modbus 专用字段迁移到 ext_config
-- 说明：保留原字段，将数据同步到 ext_config JSON 字段中
UPDATE `iot_device_address_config`
SET 
  `ext_config` = JSON_SET(
    COALESCE(`ext_config`, JSON_OBJECT()),
    '$.registerAddress', COALESCE(`register_address`, 0),
    '$.functionCode', COALESCE(`function_code`, '0x03'),
    '$.slaveAddress', COALESCE(`slave_address`, 1),
    '$.bitIndex', NULL
  ),
  `remark` = CONCAT(COALESCE(`remark`, ''), ' [Modbus字段已迁移到ext_config]'),
  `update_time` = NOW()
WHERE 
  `protocol_type` = 'MODBUS_TCP'
  AND (`register_address` IS NOT NULL OR `function_code` IS NOT NULL OR `slave_address` IS NOT NULL)
  AND (
    `ext_config` IS NULL 
    OR JSON_EXTRACT(`ext_config`, '$.registerAddress') IS NULL
  );

-- =============================================
-- 第四部分：数据验证查询
-- =============================================

-- 4.1 验证产品属性映射表数据
SELECT 
  '产品属性映射表' AS 表名,
  COUNT(*) AS 记录数,
  COUNT(DISTINCT product_id) AS 产品数,
  MIN(create_time) AS 最早创建时间,
  MAX(update_time) AS 最近更新时间
FROM `iot_product_property_mapping`;

-- 4.2 验证产品地址配置表数据
SELECT 
  '产品地址配置表' AS 表名,
  COUNT(*) AS 记录数,
  protocol_type AS 协议类型,
  COUNT(*) AS 协议配置数
FROM `iot_product_address_config`
GROUP BY protocol_type;

-- 4.3 验证设备地址配置 Modbus 字段迁移状态
SELECT 
  '设备地址配置表Modbus迁移' AS 检查项,
  COUNT(*) AS Modbus配置总数,
  SUM(CASE WHEN JSON_EXTRACT(ext_config, '$.registerAddress') IS NOT NULL THEN 1 ELSE 0 END) AS 已迁移数,
  SUM(CASE WHEN JSON_EXTRACT(ext_config, '$.registerAddress') IS NULL THEN 1 ELSE 0 END) AS 未迁移数
FROM `iot_device_address_config`
WHERE protocol_type = 'MODBUS_TCP';

-- 4.4 对比旧表和新表的记录数
SELECT 
  '数据对比' AS 检查项,
  (SELECT COUNT(*) FROM iot_product_register_mapping WHERE DELETE_FLAG = 'NOT_DELETE') AS 旧表记录数,
  (SELECT COUNT(*) FROM iot_product_property_mapping) AS 新表属性映射数,
  (SELECT COUNT(*) FROM iot_product_address_config) AS 新表地址配置数;

-- =============================================
-- 第五部分：清理操作（默认注释，需手动确认后执行）
-- =============================================

-- 5.1 删除 iot_device_address_config 表中的 Modbus 专用字段
-- 警告：执行前请先验证 ext_config 中的数据完整性！
-- 验证查询：SELECT id, register_address, function_code, slave_address, ext_config FROM iot_device_address_config WHERE protocol_type = 'MODBUS_TCP' LIMIT 10;

/*
-- 删除 register_address 字段
ALTER TABLE `iot_device_address_config` 
DROP COLUMN IF EXISTS `register_address`;

-- 删除 function_code 字段
ALTER TABLE `iot_device_address_config` 
DROP COLUMN IF EXISTS `function_code`;

-- 删除 slave_address 字段
ALTER TABLE `iot_device_address_config` 
DROP COLUMN IF EXISTS `slave_address`;
*/

-- 5.2 删除或重命名旧的寄存器映射表
-- 警告：执行前请确认所有数据已成功迁移！

/*
-- 方案1：重命名旧表（推荐，便于回滚）
RENAME TABLE `iot_device_register_mapping` TO `iot_device_register_mapping_backup`;
RENAME TABLE `iot_product_register_mapping` TO `iot_product_register_mapping_backup`;

-- 方案2：直接删除旧表（不可恢复，慎用！）
-- DROP TABLE IF EXISTS `iot_device_register_mapping`;
-- DROP TABLE IF EXISTS `iot_product_register_mapping`;
*/

-- =============================================
-- 升级脚本执行完成
-- =============================================

SELECT 
  '===============================================' AS '',
  '数据库架构升级完成' AS 状态,
  NOW() AS 完成时间,
  '请执行上方的验证查询，确认数据迁移正确' AS 下一步操作,
  '===============================================' AS '';
