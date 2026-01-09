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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vip.xiaonuo.dev.api.DevEmailApi;
import vip.xiaonuo.dev.api.DevPushApi;
import vip.xiaonuo.dev.api.DevSmsApi;

import java.util.List;
import java.util.Map;

/**
 * IoT通知服务实现
 *
 * @author yubaoshan
 * @date 2024/12/11 08:10
 **/
@Slf4j
@Service
public class IotNotificationServiceImpl implements IotNotificationService {

    @Resource
    private DevEmailApi devEmailApi;

    @Resource
    private DevSmsApi devSmsApi;

    @Resource
    private DevPushApi devPushApi;

    @Value("${snowy.iot.notification.sms.enabled:false}")
    private Boolean smsEnabled;

    @Value("${snowy.iot.notification.sms.template:}")
    private String smsTemplate;

    @Value("${snowy.iot.notification.email.enabled:false}")
    private Boolean emailEnabled;

    @Value("${snowy.iot.notification.dingtalk.enabled:false}")
    private Boolean dingTalkEnabled;

    @Value("${snowy.iot.notification.alert.phones:}")
    private String alertPhones;

    @Value("${snowy.iot.notification.alert.emails:}")
    private String alertEmails;

    @Override
    public boolean sendSms(List<String> phoneNumbers, String templateCode, Map<String, String> params) {
        if (!smsEnabled) {
            log.warn("短信通知未启用");
            return false;
        }

        if (CollectionUtil.isEmpty(phoneNumbers)) {
            log.warn("手机号列表为空");
            return false;
        }

        try {
            // 使用Snowy框架的DevSmsApi发送短信
            String phoneStr = String.join(",", phoneNumbers);
            JSONObject paramMap = new JSONObject(params);
            devSmsApi.sendDynamicSms(phoneStr, templateCode, paramMap);
            
            log.info("短信发送成功 - PhoneCount: {}, TemplateCode: {}", phoneNumbers.size(), templateCode);
            return true;
        } catch (Exception e) {
            log.error("短信发送异常", e);
            return false;
        }
    }

    @Override
    public boolean sendEmail(List<String> emails, String subject, String content) {
        if (!emailEnabled) {
            log.warn("邮件通知未启用");
            return false;
        }

        if (CollectionUtil.isEmpty(emails)) {
            log.warn("邮箱列表为空");
            return false;
        }

        try {
            // 使用Snowy框架的DevEmailApi发送邮件
            String emailStr = String.join(",", emails);
            devEmailApi.sendDynamicHtmlEmail(emailStr, subject, content);
            log.info("邮件发送成功 - EmailCount: {}, Subject: {}", emails.size(), subject);
            return true;
        } catch (Exception e) {
            log.error("邮件发送异常", e);
            return false;
        }
    }

    @Override
    public boolean sendDingTalk(String webhook, String title, String content, List<String> atMobiles, boolean isAtAll) {
        if (!dingTalkEnabled) {
            log.warn("钉钉通知未启用");
            return false;
        }

        try {
            // 使用Snowy框架的DevPushApi发送钉钉消息
            if (isAtAll) {
                // @所有人
                devPushApi.pushDingTalkMarkdown(title, content, true);
            } else if (CollectionUtil.isNotEmpty(atMobiles)) {
                // @指定人员
                String phones = String.join(",", atMobiles);
                devPushApi.pushDingTalkText(content, false, phones);
            } else {
                // 普通消息
                devPushApi.pushDingTalkMarkdown(title, content, false);
            }
            
            log.info("钉钉消息发送成功 - Title: {}", title);
            return true;
        } catch (Exception e) {
            log.error("钉钉消息发送异常", e);
            return false;
        }
    }

    @Override
    public boolean sendAlert(String title, String content, String level) {
        boolean success = false;
        
        // 根据告警级别添加前缀
        String levelPrefix = getLevelPrefix(level);
        String alertTitle = levelPrefix + title;
        String alertContent = String.format("**告警级别**: %s\n\n%s", level, content);
        
        // 发送钉钉通知
        if (dingTalkEnabled) {
            boolean dingTalkResult = sendDingTalk(null, alertTitle, alertContent, null, false);
            success = success || dingTalkResult;
        }
        
        // 发送邮件通知
        if (emailEnabled && StrUtil.isNotBlank(alertEmails)) {
            List<String> emails = StrUtil.split(alertEmails, ',');
            boolean emailResult = sendEmail(emails, alertTitle, alertContent);
            success = success || emailResult;
        }
        
        // 发送短信通知（仅ERROR级别）
        if ("ERROR".equals(level) && smsEnabled && StrUtil.isNotBlank(alertPhones) && StrUtil.isNotBlank(smsTemplate)) {
            List<String> phones = StrUtil.split(alertPhones, ',');
            Map<String, String> params = new java.util.HashMap<>();
            params.put("title", title);
            params.put("content", content);
            boolean smsResult = sendSms(phones, smsTemplate, params);
            success = success || smsResult;
        }
        
        if (success) {
            log.info("告警通知发送成功 - Title: {}, Level: {}", title, level);
        } else {
            log.warn("告警通知发送失败，所有通知渠道均未启用或发送失败 - Title: {}", title);
        }
        
        return success;
    }

    /**
     * 获取告警级别前缀
     */
    private String getLevelPrefix(String level) {
        switch (level) {
            case "ERROR":
                return "🔴 [严重] ";
            case "WARNING":
                return "🟡 [警告] ";
            case "INFO":
            default:
                return "🟢 [提示] ";
        }
    }
}
