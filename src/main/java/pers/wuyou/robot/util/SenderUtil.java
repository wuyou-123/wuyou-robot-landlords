package pers.wuyou.robot.util;

import catcode.Neko;
import love.forte.simbot.api.message.MessageContent;
import love.forte.simbot.api.message.events.GroupMsg;
import love.forte.simbot.api.sender.Sender;
import love.forte.simbot.bot.BotManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

/**
 * 发送消息的工具类
 *
 * @author admin
 */
@Component
@SuppressWarnings("unused")
public class SenderUtil {
    public static Sender SENDER;


    @Autowired
    public SenderUtil(BotManager manager) {
        SenderUtil.SENDER = manager.getDefaultBot().getSender().SENDER;
    }

    /**
     * 发送消息
     *
     * @param type    类型,group或private
     * @param code    群号或QQ号
     * @param message 消息内容
     */
    private static void sendMsg(SendType type, String code, String message) {
        sendMsg(type, code, "", message);
    }

    /**
     * 发送消息
     *
     * @param type    类型,group或private
     * @param code    群号或QQ号
     * @param message 消息内容
     */
    private static synchronized void sendMsg(SendType type, String code, String group, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        if (code == null || code.isEmpty()) {
            return;
        }
        try {
            switch (type) {
                case GROUP:
                    SENDER.sendGroupMsg(code, message);
                    break;
                case PRIVATE:
                    if (group != null && !group.isEmpty()) {
                        SENDER.sendPrivateMsg(code, group, message);
                    } else {
                        SENDER.sendPrivateMsg(code, message);
                    }
                    break;
                default:
            }
        } catch (NoSuchElementException e) {
            sendPrivateMsg(RobotUtil.ADMINISTRATOR.get(0), String.format("尝试给%s[%s]发送消息: %s 失败", type, code, message));
        }

    }

    /**
     * 发送群消息
     *
     * @param msg     groupMsg 对象
     * @param message 消息内容
     */
    public static void sendGroupMsg(GroupMsg msg, String message) {
        sendMsg(SendType.GROUP, msg.getGroupInfo().getGroupCode(), message);
    }

    /**
     * 发送群消息
     *
     * @param group 群号
     * @param msg   消息内容
     */
    public static void sendGroupMsg(String group, MessageContent msg) {
        sendGroupMsg(group, msg.getMsg());
    }

    /**
     * 发送群消息
     *
     * @param group 群号
     * @param neko  猫猫码
     */
    public static void sendGroupMsg(String group, Neko neko) {
        sendGroupMsg(group, neko.toString());
    }

    /**
     * 发送群消息
     *
     * @param group   群号
     * @param message 消息内容
     */
    public static void sendGroupMsg(String group, String message) {
        sendMsg(SendType.GROUP, group, message);
    }

    /**
     * 发送私聊消息
     *
     * @param qq      QQ号
     * @param message 消息内容
     */
    public static void sendPrivateMsg(String qq, MessageContent message) {
        sendPrivateMsg(qq, message.getMsg());
    }

    /**
     * 发送私聊消息
     *
     * @param qq   QQ号
     * @param neko 猫猫码
     */
    public static void sendPrivateMsg(String qq, Neko neko) {
        sendPrivateMsg(qq, neko.toString());
    }

    /**
     * 发送私聊消息
     *
     * @param qq      QQ号
     * @param message 消息内容
     */
    public static void sendPrivateMsg(String qq, String message) {
        sendMsg(SendType.PRIVATE, qq, message);
    }

    /**
     * 发送私聊消息
     *
     * @param qq      QQ号
     * @param message 消息内容
     */
    public static void sendPrivateMsg(String qq, String group, String message) {
        sendMsg(SendType.PRIVATE, qq, group, message);
    }

    public enum SendType {
        /**
         * 消息类型
         */
        GROUP, PRIVATE
    }

}