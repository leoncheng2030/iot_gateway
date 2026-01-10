-- 修复S7设备寄存器映射的identifier字段
-- 将identifier从地址改为物模型属性标识符，地址保存到extJson中

UPDATE iot_device_register_mapping 
SET identifier = 'test',
    ext_json = JSON_SET(COALESCE(ext_json, '{}'), '$.address', 'DB1.DBW0')
WHERE device_id = '2009900535810494466' 
  AND identifier = 'DB1.DBW0';

-- 验证修改结果
SELECT id, thing_model_id, identifier, register_address, data_type, ext_json
FROM iot_device_register_mapping 
WHERE device_id = '2009900535810494466';
