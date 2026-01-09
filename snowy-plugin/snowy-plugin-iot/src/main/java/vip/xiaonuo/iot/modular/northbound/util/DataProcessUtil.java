/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.iot.modular.northbound.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 数据处理工具类
 * 用于北向推送的数据过滤和转换
 *
 * @author yubaoshan
 * @date 2026/01/08
 */
@Slf4j
public class DataProcessUtil {

    /**
     * 过滤数据
     * 
     * 过滤规则格式（JSON）：
     * {
     *   "include": ["temperature", "humidity"],  // 仅包含这些字段
     *   "exclude": ["deviceStatus"],             // 排除这些字段
     *   "conditions": [                          // 条件过滤
     *     {"field": "temperature", "operator": ">", "value": 25},
     *     {"field": "humidity", "operator": "<=", "value": 80}
     *   ]
     * }
     * 
     * @param data 原始数据
     * @param filterRule 过滤规则（JSON字符串）
     * @return 过滤后的数据，null表示不符合条件
     */
    public static JSONObject filterData(JSONObject data, String filterRule) {
        if (StrUtil.isBlank(filterRule)) {
            return data;
        }

        try {
            JSONObject rule = JSONUtil.parseObj(filterRule);
            JSONObject filteredData = JSONUtil.createObj();
            filteredData.putAll(data);

            // 1. 字段包含过滤
            if (rule.containsKey("include")) {
                JSONObject temp = JSONUtil.createObj();
                for (String field : rule.getJSONArray("include").toList(String.class)) {
                    if (data.containsKey(field)) {
                        temp.set(field, data.get(field));
                    }
                }
                filteredData = temp;
            }

            // 2. 字段排除过滤
            if (rule.containsKey("exclude")) {
                for (String field : rule.getJSONArray("exclude").toList(String.class)) {
                    filteredData.remove(field);
                }
            }

            // 3. 条件过滤
            if (rule.containsKey("conditions")) {
                for (Object condObj : rule.getJSONArray("conditions")) {
                    JSONObject condition = (JSONObject) condObj;
                    String field = condition.getStr("field");
                    String operator = condition.getStr("operator");
                    Object value = condition.get("value");

                    if (!matchCondition(data, field, operator, value)) {
                        return null; // 不符合条件，返回null
                    }
                }
            }

            return filteredData;

        } catch (Exception e) {
            log.error("数据过滤异常", e);
            return data; // 过滤失败时返回原始数据
        }
    }

    /**
     * 转换数据
     * 
     * 转换规则格式（JSON）：
     * {
     *   "fieldMapping": {                        // 字段映射
     *     "temperature": "temp",
     *     "humidity": "hum"
     *   },
     *   "valueMapping": {                        // 值映射
     *     "deviceStatus": {
     *       "ONLINE": "在线",
     *       "OFFLINE": "离线"
     *     }
     *   },
     *   "calculated": [                          // 计算字段
     *     {
     *       "field": "fahrenheit",
     *       "expression": "temperature * 1.8 + 32"
     *     }
     *   ]
     * }
     * 
     * @param data 原始数据
     * @param transformRule 转换规则（JSON字符串）
     * @return 转换后的数据
     */
    public static JSONObject transformData(JSONObject data, String transformRule) {
        if (StrUtil.isBlank(transformRule)) {
            return data;
        }

        try {
            JSONObject rule = JSONUtil.parseObj(transformRule);
            JSONObject transformedData = JSONUtil.createObj();

            // 1. 字段映射
            JSONObject fieldMapping = rule.getJSONObject("fieldMapping");
            if (fieldMapping != null) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String originalField = entry.getKey();
                    String mappedField = fieldMapping.getStr(originalField);
                    String targetField = mappedField != null ? mappedField : originalField;
                    transformedData.set(targetField, entry.getValue());
                }
            } else {
                transformedData.putAll(data);
            }

            // 2. 值映射
            JSONObject valueMapping = rule.getJSONObject("valueMapping");
            if (valueMapping != null) {
                for (String field : valueMapping.keySet()) {
                    if (transformedData.containsKey(field)) {
                        Object originalValue = transformedData.get(field);
                        JSONObject mapping = valueMapping.getJSONObject(field);
                        if (mapping.containsKey(originalValue.toString())) {
                            transformedData.set(field, mapping.get(originalValue.toString()));
                        }
                    }
                }
            }

            // 3. 计算字段（简单实现）
            if (rule.containsKey("calculated")) {
                for (Object calcObj : rule.getJSONArray("calculated")) {
                    JSONObject calc = (JSONObject) calcObj;
                    String field = calc.getStr("field");
                    String expression = calc.getStr("expression");
                    
                    Object calculatedValue = evaluateExpression(transformedData, expression);
                    if (calculatedValue != null) {
                        transformedData.set(field, calculatedValue);
                    }
                }
            }

            return transformedData;

        } catch (Exception e) {
            log.error("数据转换异常", e);
            return data; // 转换失败时返回原始数据
        }
    }

    /**
     * 匹配条件
     */
    private static boolean matchCondition(JSONObject data, String field, String operator, Object value) {
        if (!data.containsKey(field)) {
            return false;
        }

        Object fieldValue = data.get(field);
        
        try {
            switch (operator) {
                case "=":
                case "==":
                    return fieldValue.toString().equals(value.toString());
                case "!=":
                    return !fieldValue.toString().equals(value.toString());
                case ">":
                    return Double.parseDouble(fieldValue.toString()) > Double.parseDouble(value.toString());
                case ">=":
                    return Double.parseDouble(fieldValue.toString()) >= Double.parseDouble(value.toString());
                case "<":
                    return Double.parseDouble(fieldValue.toString()) < Double.parseDouble(value.toString());
                case "<=":
                    return Double.parseDouble(fieldValue.toString()) <= Double.parseDouble(value.toString());
                case "contains":
                    return fieldValue.toString().contains(value.toString());
                default:
                    return true;
            }
        } catch (Exception e) {
            log.warn("条件匹配失败 - field: {}, operator: {}, value: {}", field, operator, value);
            return true;
        }
    }

    /**
     * 计算表达式（简单实现）
     * 支持基本的算术运算
     */
    private static Object evaluateExpression(JSONObject data, String expression) {
        try {
            String expr = expression;
            
            // 替换字段名为实际值
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String field = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Number) {
                    expr = expr.replace(field, value.toString());
                }
            }
            
            // 简单的算术表达式计算（这里可以使用更强大的表达式引擎如MVEL、Groovy等）
            return evaluateSimpleExpression(expr);
            
        } catch (Exception e) {
            log.warn("表达式计算失败 - expression: {}", expression);
            return null;
        }
    }

    /**
     * 计算简单算术表达式
     * 仅支持 +, -, *, /
     */
    private static Double evaluateSimpleExpression(String expression) {
        try {
            // 移除空格
            expression = expression.replace(" ", "");
            
            // 处理乘除
            while (expression.contains("*") || expression.contains("/")) {
                if (expression.contains("*")) {
                    int idx = expression.indexOf("*");
                    double left = getLeftNumber(expression, idx);
                    double right = getRightNumber(expression, idx);
                    double result = left * right;
                    expression = replaceOperation(expression, idx, left, right, result);
                } else {
                    int idx = expression.indexOf("/");
                    double left = getLeftNumber(expression, idx);
                    double right = getRightNumber(expression, idx);
                    double result = left / right;
                    expression = replaceOperation(expression, idx, left, right, result);
                }
            }
            
            // 处理加减
            double result = 0;
            String[] parts = expression.split("(?=[+-])");
            for (String part : parts) {
                if (StrUtil.isNotBlank(part)) {
                    result += Double.parseDouble(part);
                }
            }
            
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static double getLeftNumber(String expr, int opIdx) {
        int start = opIdx - 1;
        while (start >= 0 && (Character.isDigit(expr.charAt(start)) || expr.charAt(start) == '.')) {
            start--;
        }
        return Double.parseDouble(expr.substring(start + 1, opIdx));
    }

    private static double getRightNumber(String expr, int opIdx) {
        int end = opIdx + 1;
        while (end < expr.length() && (Character.isDigit(expr.charAt(end)) || expr.charAt(end) == '.')) {
            end++;
        }
        return Double.parseDouble(expr.substring(opIdx + 1, end));
    }

    private static String replaceOperation(String expr, int opIdx, double left, double right, double result) {
        String leftStr = String.valueOf(left);
        String rightStr = String.valueOf(right);
        int startIdx = expr.indexOf(leftStr);
        int endIdx = startIdx + leftStr.length() + 1 + rightStr.length();
        return expr.substring(0, startIdx) + result + expr.substring(endIdx);
    }
}
