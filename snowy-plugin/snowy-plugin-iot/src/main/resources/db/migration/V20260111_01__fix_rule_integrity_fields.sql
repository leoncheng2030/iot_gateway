-- 修复规则完整性字段类型
ALTER TABLE iot_rule MODIFY COLUMN integrity_status VARCHAR(20) DEFAULT 'empty' COMMENT '完整性状态：valid-完整，invalid-不完整，empty-未配置';
