package pers.wuyou.robot.game.landlords.game;

import pers.wuyou.robot.game.landlords.GameManager;
import pers.wuyou.robot.game.landlords.common.Constant;
import pers.wuyou.robot.game.landlords.common.GameEvent;
import pers.wuyou.robot.game.landlords.entity.Player;
import pers.wuyou.robot.game.landlords.entity.PokerSell;
import pers.wuyou.robot.game.landlords.entity.Room;
import pers.wuyou.robot.game.landlords.enums.PlayerGameStatus;
import pers.wuyou.robot.game.landlords.enums.SellType;
import pers.wuyou.robot.game.landlords.helper.PokerHelper;
import pers.wuyou.robot.game.landlords.util.NotifyUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wuyou
 */
@SuppressWarnings("unused")
public class GamePlayerMessageOnRound implements GameEvent {

    private final static List<String> CMD_LIST = Arrays.asList("上一页", "下一页");
    private final static int PAGE_SIZE = 10;
    private final static String SELL_TYPE = "sellType";
    final Map<String, SellType[]> sellTypeMap = new HashMap<>();

    public GamePlayerMessageOnRound() {
        sellTypeMap.put("提示", new SellType[0]);
        sellTypeMap.put("顺子", new SellType[]{SellType.SINGLE_STRAIGHT, SellType.DOUBLE_STRAIGHT, SellType.THREE_STRAIGHT, SellType.FOUR_STRAIGHT});
        sellTypeMap.put("单顺子", new SellType[]{SellType.SINGLE_STRAIGHT});
        sellTypeMap.put("连对", new SellType[]{SellType.DOUBLE_STRAIGHT});
        sellTypeMap.put("双顺子", new SellType[]{SellType.DOUBLE_STRAIGHT});
        sellTypeMap.put("三顺子", new SellType[]{SellType.THREE_STRAIGHT});
        sellTypeMap.put("四顺子", new SellType[]{SellType.FOUR_STRAIGHT});
        sellTypeMap.put("三带", new SellType[]{SellType.THREE_ZONES_SINGLE, SellType.THREE_ZONES_DOUBLE});
        sellTypeMap.put("三带一", new SellType[]{SellType.THREE_ZONES_SINGLE});
        sellTypeMap.put("三带二", new SellType[]{SellType.THREE_ZONES_DOUBLE});
        sellTypeMap.put("四带", new SellType[]{SellType.FOUR_ZONES_SINGLE, SellType.FOUR_ZONES_DOUBLE});
        sellTypeMap.put("四带一", new SellType[]{SellType.FOUR_ZONES_SINGLE});
        sellTypeMap.put("四带二", new SellType[]{SellType.FOUR_ZONES_DOUBLE});
        sellTypeMap.put("飞机", new SellType[]{SellType.THREE_STRAIGHT_WITH_SINGLE, SellType.THREE_STRAIGHT_WITH_DOUBLE});
        sellTypeMap.put("炸弹", new SellType[]{SellType.BOMB});
    }

    private List<PokerSell> getNextPagePokerSellList(Player player) {
        final Map<String, Object> playerDataMap = GameManager.getPlayerDataMap(player);
        List<PokerSell> list = getList(player);
        int page = (int) playerDataMap.getOrDefault("page", 0);
        if (page * PAGE_SIZE > list.size()) {
            return new ArrayList<>();
        }
        playerDataMap.put("page", page + 1);
        return list.subList(page * PAGE_SIZE, Math.min(list.size(), (page + 1) * PAGE_SIZE));
    }

    private List<PokerSell> getPrePagePokerSellList(Player player) {
        final Map<String, Object> playerDataMap = GameManager.getPlayerDataMap(player);
        List<PokerSell> list = getList(player);
        int page = (int) playerDataMap.getOrDefault("page", 0);
        if (page <= 1) {
            return new ArrayList<>();
        }
        playerDataMap.put("page", page - 1);
        return list.subList((page - 2) * PAGE_SIZE, Math.min(list.size(), (page - 1) * PAGE_SIZE));
    }

    private List<PokerSell> getList(Player player) {
        final Map<String, Object> playerDataMap = GameManager.getPlayerDataMap(player);
        final List<SellType> sellTypes = Arrays.asList((SellType[]) playerDataMap.get(SELL_TYPE));
        List<PokerSell> sells;
        if (player.getRoom().getLastPlayer().equals(player)) {
            sells = PokerHelper.validSells(null, player.getPokers());
        } else {
            sells = PokerHelper.validSells(player.getRoom().getLastPlayPoker(), player.getPokers());
        }
        // 将每个类型的牌控制在三种以内
        return sells.stream()
                .filter(item -> sellTypes.size() == 0 || sellTypes.contains(item.getSellType()))
                .collect(Collectors.groupingBy(PokerSell::getSellType))
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(a -> a.getValue().get(0).getSellType().index()))
                .flatMap(i -> i.getValue().stream().limit(5))
                .collect(Collectors.toList());
    }

    @Override
    public void call(Room room, Map<String, Object> data) {
        Player player = room.getCurrentPlayer();
        final String message = data.get(Constant.MESSAGE).toString();
        final SellType[] types = sellTypeMap.get(message);
        final Map<String, Object> playerDataMap = GameManager.getPlayerDataMap(player);
        if (types == null) {
            if (CMD_LIST.contains(message)) {
                final List<PokerSell> list;
                if (playerDataMap.get(SELL_TYPE) == null) {
                    return;
                }
                switch (message) {
                    case "上一页":
                        list = getPrePagePokerSellList(player);
                        player.setStatus(PlayerGameStatus.CHOOSE_TIP);
                        if (list.isEmpty()) {
                            NotifyUtil.notifyPlayer(player, "没有上一页了");
                            NotifyUtil.notifyPlayerChoosePokers(player);
                            return;
                        }
                        playerDataMap.put("list", list);
                        NotifyUtil.notifyPlayerChoosePokers(player, list);
                        break;
                    case "下一页":
                        list = getNextPagePokerSellList(player);
                        player.setStatus(PlayerGameStatus.CHOOSE_TIP);
                        if (list.isEmpty()) {
                            NotifyUtil.notifyPlayer(player, "没有下一页了");
                            NotifyUtil.notifyPlayerChoosePokers(player);
                            return;
                        }
                        playerDataMap.put("list", list);
                        NotifyUtil.notifyPlayerChoosePokers(player, list);
                        break;
                    default:
                }
                return;
            }
            if (player.getStatus() == PlayerGameStatus.CHOOSE_TIP) {
                NotifyUtil.notifyPlayer(player, "请输入一个数字.");
            }
            return;
        }
        final List<SellType> sellTypes = Arrays.asList(types);
        if (!sellTypes.isEmpty() && room.getLastPlayPoker() != null && !room.getLastPlayer().equals(player) && !sellTypes.contains(room.getLastPlayPoker().getSellType())) {
            NotifyUtil.notifyPlayerTypePokerInvalid(player);
            return;
        }
        playerDataMap.put(SELL_TYPE, types);
        playerDataMap.put("page", 0);
        final List<PokerSell> list = getNextPagePokerSellList(player);
        player.setStatus(PlayerGameStatus.CHOOSE_TIP);
        playerDataMap.put("list", list);
        NotifyUtil.notifyPlayerChoosePokers(player, list);
    }
}
