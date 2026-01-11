-- 添加规则完整性字段
ALTER TABLE iot_rule ADD COLUMN integrity_status VARCHAR(20) DEFAULT 'empty' COMMENT '完整性状态：valid-完整，invalid-不完整，empty-未配置';
ALTER TABLE iot_rule ADD COLUMN integrity_issues INT DEFAULT 0 COMMENT '完整性问题数量';
