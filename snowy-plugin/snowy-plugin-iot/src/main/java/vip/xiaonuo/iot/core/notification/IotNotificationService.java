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
package vip.xiaonuo.iot.core.notification;

import java.util.List;
import java.util.Map;

/**
 * IoT通知服务接口
 *
 * @author yubaoshan
 * @date 2024/12/11 08:10
 **/
public interface IotNotificationService {

    /**
     * 发送短信通知
     *
     * @param phoneNumbers 手机号列表
     * @param templateCode 模板编码
     * @param params 模板参数
     * @return 发送结果
     */
    boolean sendSms(List<String> phoneNumbers, String templateCode, Map<String, String> params);

    /**
     * 发送邮件通知
     *
     * @param emails 邮箱列表
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 发送结果
     */
    boolean sendEmail(List<String> emails, String subject, String content);

    /**
     * 发送钉钉通知
     *
     * @param webhook 钉钉机器人webhook地址
     * @param title 消息标题
     * @param content 消息内容
     * @param atMobiles @的手机号列表
     * @param isAtAll 是否@所有人
     * @return 发送结果
     */
    boolean sendDingTalk(String webhook, String title, String content, List<String> atMobiles, boolean isAtAll);

    /**
     * 发送告警通知（自动根据配置选择通知方式）
     *
     * @param title 告警标题
     * @param content 告警内容
     * @param level 告警级别(INFO/WARNING/ERROR)
     * @return 发送结果
     */
    boolean sendAlert(String title, String content, String level);
}
