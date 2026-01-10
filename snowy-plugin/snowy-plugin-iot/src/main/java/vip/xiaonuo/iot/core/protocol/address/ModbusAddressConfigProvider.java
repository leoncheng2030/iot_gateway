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
package vip.xiaonuo.iot.core.protocol.address;

import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Modbus协议地址配置提供者
 * Modbus使用Driver机制，不是ProtocolServer，单独提供配置
 *
 * @author jetox
 * @date 2026/01/10 17:45
 **/
@Component
public class ModbusAddressConfigProvider {

    /**
     * 获取Modbus TCP地址配置模板
     */
    public AddressConfigTemplate getModbusTcpTemplate() {
        AddressConfigTemplate template = new AddressConfigTemplate();
        template.setProtocolType("MODBUS_TCP");
        template.setTemplateName("Modbus TCP 地址配置");
        template.setInputMode(AddressConfigTemplate.AddressInputMode.SIMPLE);
        template.setFormatDescription("Modbus TCP 使用功能码 + 数字地址的方式访问寄存器");
        template.setExamples(Arrays.asList(
            "功能码 0x03 + 地址 100 - 读取保持寄存器地址100开始的数据",
            "功能码 0x04 + 地址 200 - 读取输入寄存器地址200开始的数据",
            "功能码 0x01 + 地址 0 - 读取线圈地址0开始的数据"
        ));
        template.setValidationRegex("^\\d+$"); // 纯数字地址

        // 配置字段列表
        AddressConfigTemplate.ConfigField functionCodeField = new AddressConfigTemplate.ConfigField();
        functionCodeField.setName("functionCode");
        functionCodeField.setLabel("功能码");
        functionCodeField.setType(AddressConfigTemplate.FieldType.SELECT);
        functionCodeField.setDescription("选择Modbus功能码");
        functionCodeField.setRequired(true);
        functionCodeField.setDefaultValue("0x03");
        functionCodeField.setOptions(Arrays.asList(
            createOption("0x01 - 读线圈", "0x01", "读取线圈状态（DO）"),
            createOption("0x02 - 读离散输入", "0x02", "读取离散输入状态（DI）"),
            createOption("0x03 - 读保持寄存器", "0x03", "读取保持寄存器（最常用）"),
            createOption("0x04 - 读输入寄存器", "0x04", "读取输入寄存器（只读）"),
            createOption("0x05 - 写单个线圈", "0x05", "写入单个线圈"),
            createOption("0x06 - 写单个寄存器", "0x06", "写入单个保持寄存器"),
            createOption("0x0F - 写多个线圈", "0x0F", "写入多个线圈"),
            createOption("0x10 - 写多个寄存器", "0x10", "写入多个保持寄存器")
        ));

        AddressConfigTemplate.ConfigField addressField = new AddressConfigTemplate.ConfigField();
        addressField.setName("registerAddress");
        addressField.setLabel("寄存器地址");
        addressField.setType(AddressConfigTemplate.FieldType.NUMBER);
        addressField.setDescription("寄存器的起始地址（0-65535）");
        addressField.setRequired(true);
        addressField.setDefaultValue(0);
        addressField.setMin(0);
        addressField.setMax(65535);

        AddressConfigTemplate.ConfigField dataTypeField = new AddressConfigTemplate.ConfigField();
        dataTypeField.setName("dataType");
        dataTypeField.setLabel("数据类型");
        dataTypeField.setType(AddressConfigTemplate.FieldType.SELECT);
        dataTypeField.setDescription("选择数据类型");
        dataTypeField.setRequired(true);
        dataTypeField.setDefaultValue("int");
        dataTypeField.setOptions(Arrays.asList(
            createOption("开关量 (bool)", "bool", "布尔值，true/false"),
            createOption("整数 (int)", "int", "16位整数"),
            createOption("浮点数 (float)", "float", "32位浮点数")
        ));

        template.setFields(Arrays.asList(functionCodeField, addressField, dataTypeField));

        return template;
    }

    /**
     * 获取Modbus RTU地址配置模板
     */
    public AddressConfigTemplate getModbusRtuTemplate() {
        AddressConfigTemplate template = getModbusTcpTemplate();
        template.setProtocolType("MODBUS_RTU");
        template.setTemplateName("Modbus RTU 地址配置");
        template.setFormatDescription("Modbus RTU 使用功能码 + 数字地址的方式访问寄存器（通过串口通信）");
        return template;
    }

    /**
     * 创建选项对象
     */
    private AddressConfigTemplate.Option createOption(String label, Object value, String description) {
        AddressConfigTemplate.Option option = new AddressConfigTemplate.Option();
        option.setLabel(label);
        option.setValue(value);
        option.setDescription(description);
        return option;
    }
}
