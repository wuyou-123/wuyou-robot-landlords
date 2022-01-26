package pers.wuyou.robot.entertainment.listener;

import catcode.Neko;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import love.forte.simbot.annotation.ContextValue;
import love.forte.simbot.annotation.Filter;
import love.forte.simbot.annotation.FilterValue;
import love.forte.simbot.annotation.Listener;
import love.forte.simbot.api.message.events.PrivateMsg;
import love.forte.simbot.component.mirai.message.event.MiraiNudgedEvent;
import love.forte.simbot.filter.MatchType;
import pers.wuyou.robot.core.RobotCore;
import pers.wuyou.robot.core.annotation.ContextType;
import pers.wuyou.robot.core.annotation.RobotListen;
import pers.wuyou.robot.core.util.CatUtil;
import pers.wuyou.robot.core.util.SenderUtil;
import pers.wuyou.robot.entertainment.entity.Sentence;
import pers.wuyou.robot.entertainment.service.SentenceService;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author wuyou
 */
@Listener
public class NudgedListener {
    private final SentenceService service;
    private final Random random = new Random();
    private List<String> messageList;
    private boolean isInit;

    public NudgedListener(SentenceService service) {
        this.service = service;
    }

    @RobotListen(value = MiraiNudgedEvent.ByMember.class, isBoot = true)
    public void nudge(MiraiNudgedEvent.ByMember msg) {
        final Neko neko = msg.getMsgContent().getCats(CatUtil.CatType.NUDGE).get(0);
        final String nekoParam = "target";
        if (!Objects.equals(neko.get(nekoParam), RobotCore.getDefaultBotCode())) {
            return;
        }
        if (!isInit) {
            messageList = this.service.list().stream().map(Sentence::getText).collect(Collectors.toList());
            isInit = true;
        }
        if (messageList.isEmpty()) {
            return;
        }
        final String group = msg.getGroupInfo().getGroupCode();
        final String text = messageList.get(random.nextInt(messageList.size()));
        RobotCore.sender().sendGroupMsg(group, text);
    }

    @RobotListen(PrivateMsg.class)
    @Filter(value = "^add{{text}}", matchType = MatchType.REGEX_FIND)
    public void addNudgeMessage(
            @ContextValue(ContextType.QQ) String qq,
            @FilterValue("text") String text
    ) {
        if (RobotCore.getADMINISTRATOR().contains(qq)) {
            if (service.save(new Sentence(text))) {
                SenderUtil.sendPrivateMsg(qq, "success");
                messageList.add(text);
            } else {
                SenderUtil.sendPrivateMsg(qq, "fail");
            }
        }
    }

    @RobotListen(PrivateMsg.class)
    @Filter(value = "^remove{{text}}", matchType = MatchType.REGEX_FIND)
    public void removeNudgeMessage(
            @ContextValue(ContextType.QQ) String qq,
            @FilterValue("text") String text
    ) {
        if (RobotCore.getADMINISTRATOR().contains(qq)) {
            if (service.remove(new LambdaQueryWrapper<Sentence>().eq(Sentence::getText, text))) {
                SenderUtil.sendPrivateMsg(qq, "success");
                messageList.remove(text);
            } else {
                SenderUtil.sendPrivateMsg(qq, "fail");
            }
        }
    }
}
