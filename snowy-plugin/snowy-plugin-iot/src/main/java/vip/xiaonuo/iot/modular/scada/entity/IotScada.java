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
package vip.xiaonuo.iot.modular.scada.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 组态实体
 *
 * @author jetox
 * @date 2025/12/14
 **/
@Getter
@Setter
@TableName("IOT_SCADA")
public class IotScada {

    /** 主键ID */
    @TableId
    @Schema(description = "主键ID")
    private String id;

    /** 组态名称 */
    @Schema(description = "组态名称")
    private String name;

    /** 组态配置JSON */
    @Schema(description = "组态配置JSON")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String config;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private Date createTime;

    /** 创建用户 */
    @Schema(description = "创建用户")
    private String createUser;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private Date updateTime;

    /** 更新用户 */
    @Schema(description = "更新用户")
    private String updateUser;
}
