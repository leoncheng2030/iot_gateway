/**
 * IoT 物联网平台常量定义
 * 统一管理IoT模块中使用的常量、枚举等
 */

// ==================== 物模型相关 ====================

/**
 * 物模型类型
 */
export const ModelType = {
	PROPERTY: 'PROPERTY', // 属性
	EVENT: 'EVENT', // 事件
	SERVICE: 'SERVICE' // 服务
}

/**
 * 物模型类型标签映射
 */
export const ModelTypeLabels = {
	[ModelType.PROPERTY]: '属性',
	[ModelType.EVENT]: '事件',
	[ModelType.SERVICE]: '服务'
}

/**
 * 物模型类型颜色映射(用于Tag)
 */
export const ModelTypeColors = {
	[ModelType.PROPERTY]: 'blue',
	[ModelType.EVENT]: 'orange',
	[ModelType.SERVICE]: 'green'
}

/**
 * 值类型
 */
export const ValueType = {
	BOOL: 'bool', // 布尔
	INT: 'int', // 整数
	FLOAT: 'float', // 浮点数
	DOUBLE: 'double', // 双精度浮点数
	ENUM: 'enum', // 枚举
	TEXT: 'text', // 文本
	DATE: 'date', // 日期
	STRUCT: 'struct', // 结构体
	ARRAY: 'array' // 数组
}

/**
 * 值类型标签映射
 */
export const ValueTypeLabels = {
	[ValueType.BOOL]: '布尔',
	[ValueType.INT]: '整数',
	[ValueType.FLOAT]: '浮点数',
	[ValueType.DOUBLE]: '双精度',
	[ValueType.ENUM]: '枚举',
	[ValueType.TEXT]: '文本',
	[ValueType.DATE]: '日期',
	[ValueType.STRUCT]: '结构体',
	[ValueType.ARRAY]: '数组'
}

/**
 * 数值类型集合（用于判断）
 */
export const NumericValueTypes = [ValueType.INT, ValueType.FLOAT, ValueType.DOUBLE]

/**
 * 访问模式（读写类型）
 */
export const AccessMode = {
	READ: 'R', // 只读
	WRITE: 'W', // 只写
	READ_WRITE: 'RW' // 读写
}

/**
 * 访问模式标签映射
 */
export const AccessModeLabels = {
	[AccessMode.READ]: '只读',
	[AccessMode.WRITE]: '只写',
	[AccessMode.READ_WRITE]: '读写'
}

// ==================== 设备相关 ====================

/**
 * 设备状态
 */
export const DeviceStatus = {
	ONLINE: 'ONLINE', // 在线
	OFFLINE: 'OFFLINE', // 离线
	INACTIVE: 'INACTIVE' // 未激活
}

/**
 * 设备状态标签映射
 */
export const DeviceStatusLabels = {
	[DeviceStatus.ONLINE]: '在线',
	[DeviceStatus.OFFLINE]: '离线',
	[DeviceStatus.INACTIVE]: '未激活'
}

/**
 * 设备状态颜色映射
 */
export const DeviceStatusColors = {
	[DeviceStatus.ONLINE]: 'green',
	[DeviceStatus.OFFLINE]: 'red',
	[DeviceStatus.INACTIVE]: 'gray'
}

// ==================== Modbus相关 ====================

/**
 * Modbus功能码
 */
export const ModbusFunctionCode = {
	READ_COILS: 0x01, // 读线圈
	READ_DISCRETE_INPUTS: 0x02, // 读离散输入
	READ_HOLDING_REGISTERS: 0x03, // 读保持寄存器
	READ_INPUT_REGISTERS: 0x04, // 读输入寄存器
	WRITE_SINGLE_COIL: 0x05, // 写单个线圈
	WRITE_SINGLE_REGISTER: 0x06, // 写单个寄存器
	WRITE_MULTIPLE_COILS: 0x0f, // 写多个线圈
	WRITE_MULTIPLE_REGISTERS: 0x10 // 写多个寄存器
}

/**
 * Modbus功能码标签映射
 */
export const ModbusFunctionCodeLabels = {
	[ModbusFunctionCode.READ_COILS]: '读线圈(0x01)',
	[ModbusFunctionCode.READ_DISCRETE_INPUTS]: '读离散输入(0x02)',
	[ModbusFunctionCode.READ_HOLDING_REGISTERS]: '读保持寄存器(0x03)',
	[ModbusFunctionCode.READ_INPUT_REGISTERS]: '读输入寄存器(0x04)',
	[ModbusFunctionCode.WRITE_SINGLE_COIL]: '写单个线圈(0x05)',
	[ModbusFunctionCode.WRITE_SINGLE_REGISTER]: '写单个寄存器(0x06)',
	[ModbusFunctionCode.WRITE_MULTIPLE_COILS]: '写多个线圈(0x0F)',
	[ModbusFunctionCode.WRITE_MULTIPLE_REGISTERS]: '写多个寄存器(0x10)'
}

/**
 * Modbus数据类型
 */
export const ModbusDataType = {
	INT16: 'INT16', // 16位整数
	UINT16: 'UINT16', // 16位无符号整数
	INT32: 'INT32', // 32位整数
	UINT32: 'UINT32', // 32位无符号整数
	FLOAT: 'FLOAT', // 32位浮点数
	BIT: 'BIT', // 单个位
	STRING: 'STRING' // 字符串
}

/**
 * Modbus数据类型标签映射
 */
export const ModbusDataTypeLabels = {
	[ModbusDataType.INT16]: 'INT16(有符号16位整数)',
	[ModbusDataType.UINT16]: 'UINT16(无符号16位整数)',
	[ModbusDataType.INT32]: 'INT32(有符号32位整数)',
	[ModbusDataType.UINT32]: 'UINT32(无符号32位整数)',
	[ModbusDataType.FLOAT]: 'FLOAT(32位浮点数)',
	[ModbusDataType.BIT]: 'BIT(单个位)',
	[ModbusDataType.STRING]: 'STRING(字符串)'
}

/**
 * 字节序
 */
export const ByteOrder = {
	BIG_ENDIAN: 'BIG_ENDIAN', // 大端
	LITTLE_ENDIAN: 'LITTLE_ENDIAN', // 小端
	BIG_ENDIAN_SWAP: 'BIG_ENDIAN_SWAP', // 大端交换
	LITTLE_ENDIAN_SWAP: 'LITTLE_ENDIAN_SWAP' // 小端交换
}

/**
 * 字节序标签映射
 */
export const ByteOrderLabels = {
	[ByteOrder.BIG_ENDIAN]: '大端(ABCD)',
	[ByteOrder.LITTLE_ENDIAN]: '小端(DCBA)',
	[ByteOrder.BIG_ENDIAN_SWAP]: '大端交换(BADC)',
	[ByteOrder.LITTLE_ENDIAN_SWAP]: '小端交换(CDAB)'
}

// ==================== 服务相关 ====================

/**
 * 服务调用方式
 */
export const CallType = {
	ASYNC: 'ASYNC', // 异步
	SYNC: 'SYNC' // 同步
}

/**
 * 服务调用方式标签映射
 */
export const CallTypeLabels = {
	[CallType.ASYNC]: '异步',
	[CallType.SYNC]: '同步'
}

/**
 * 服务参数数据类型
 */
export const ServiceParamDataType = {
	INT: 'int',
	FLOAT: 'float',
	DOUBLE: 'double',
	BOOL: 'bool',
	STRING: 'string',
	ARRAY: 'array',
	OBJECT: 'object',
	ENUM: 'enum',
	DATE: 'date'
}

/**
 * 服务参数数据类型标签映射
 */
export const ServiceParamDataTypeLabels = {
	[ServiceParamDataType.INT]: '整数 (int)',
	[ServiceParamDataType.FLOAT]: '浮点数 (float)',
	[ServiceParamDataType.DOUBLE]: '双精度 (double)',
	[ServiceParamDataType.BOOL]: '布尔 (bool)',
	[ServiceParamDataType.STRING]: '字符串 (string)',
	[ServiceParamDataType.ARRAY]: '数组 (array)',
	[ServiceParamDataType.OBJECT]: '对象 (object)',
	[ServiceParamDataType.ENUM]: '枚举 (enum)',
	[ServiceParamDataType.DATE]: '日期 (date)'
}

// ==================== SSE消息类型 ====================

/**
 * SSE消息类型
 */
export const SSEMessageType = {
	DEVICE_STATUS: 'deviceStatus', // 设备状态变化
	DEVICE_DATA: 'deviceData', // 设备数据上报
	DEVICE_SHADOW: 'deviceShadow', // 设备影子变化
	DEVICE_EVENT: 'deviceEvent' // 设备事件上报
}

// ==================== 工具函数 ====================

/**
 * 判断是否为数值类型
 * @param {string} valueType 值类型
 * @returns {boolean}
 */
export function isNumericType(valueType) {
	return NumericValueTypes.includes(valueType)
}

/**
 * 获取模型类型标签
 * @param {string} modelType 模型类型
 * @returns {string}
 */
export function getModelTypeLabel(modelType) {
	return ModelTypeLabels[modelType] || modelType
}

/**
 * 获取模型类型颜色
 * @param {string} modelType 模型类型
 * @returns {string}
 */
export function getModelTypeColor(modelType) {
	return ModelTypeColors[modelType] || 'default'
}

/**
 * 获取设备状态标签
 * @param {string} status 设备状态
 * @returns {string}
 */
export function getDeviceStatusLabel(status) {
	return DeviceStatusLabels[status] || status
}

/**
 * 获取设备状态颜色
 * @param {string} status 设备状态
 * @returns {string}
 */
export function getDeviceStatusColor(status) {
	return DeviceStatusColors[status] || 'default'
}

/**
 * 获取值类型标签
 * @param {string} valueType 值类型
 * @returns {string}
 */
export function getValueTypeLabel(valueType) {
	return ValueTypeLabels[valueType] || valueType
}

/**
 * 获取访问模式标签
 * @param {string} accessMode 访问模式
 * @returns {string}
 */
export function getAccessModeLabel(accessMode) {
	return AccessModeLabels[accessMode] || accessMode
}
