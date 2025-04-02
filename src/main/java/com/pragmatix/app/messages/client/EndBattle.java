package com.pragmatix.app.messages.client;

import com.pragmatix.Commands;
import com.pragmatix.app.common.BattleResultEnum;
import com.pragmatix.app.common.BossBattleResultType;
import com.pragmatix.app.common.WhichLevelEnum;
import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Serialize;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * команда отсылаеться с клиента на сервер и говорит о конце битвы
 *
 * @see com.pragmatix.app.controllers.BattleController#onEndBattle(EndBattle, com.pragmatix.app.model.UserProfile)
 * <p>
 * User: denis
 * Date: 05.12.2009
 * Time: 21:11:16
 */
@Command(Commands.EndBattle)
public class EndBattle extends SecuredCommand {

    private static final BackpackItemStructure[] EMPTY_ARR = new BackpackItemStructure[0];

    /**
     * результат боя (в исходном "зашифрованном" виде как result + battleId)
     */
    @Resize(TypeSize.UINT32)
    public long resultRaw;
    /**
     * тип боя (дрались с уровнем выше/меньше)
     */
    public WhichLevelEnum type = WhichLevelEnum.MY_LEVEL;
    /**
     * получено, бонусного опыта
     */
    public int expBonus;
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;
    /**
     * id миссии
     * 0 - бой с ботами
     */
    public short missionId;
    /**
     * массив потраченого оружия в уровне
     */
    public BackpackItemStructure[] items = EMPTY_ARR;
    /**
     * во время боя была обнаружена попытка взлома
     */
    public short banType;
    /**
     * доп. информация по бану
     */
    public String banNote = "";
    /**
     * массив id реагентов, которые игрок собрал бою. длинной от 0-3
     * id могут повторяться
     */
    public byte[] collectedReagents = ArrayUtils.EMPTY_BYTE_ARRAY;

    public short fakeByteArraySize; // путаем следы: делаем вид, что дальше идёт byte[]

    // >>> итог боя с боссом:
    /**
     * полное количество ходов в течение боя
     */
    public short totalTurnsCount;
    /**
     * полный урон, полученный _игроком_
     */
    public int totalDamageToPlayer;
    /**
     * полный урон, полученный _боссом_
     */
    public int totalDamageToBoss;
    /**
     * массив оружия, использованного _игроком_ за бой
     */
    public BackpackItemStructure[] totalUsedItems = EMPTY_ARR;
    // <<< итог боя с боссом:

    /**
     * ключ текущей сессии
     */
    public String sessionKey;
    /**
     * результат боя: победа/ничья/поражение/сдался/... после "расшифровки" на входе
     */
    @Ignore
    public BattleResultEnum result;

    @Ignore
    public BattleAward battleAward = new BattleAward();

    public static class BattleAward {

        public int money;
        public int experience;
        public int realMoney;
        public int boostFactor;
        public List<Byte> collectedReagents = new ArrayList<>(CraftService.MAX_REAGENTS_FOR_BATTLE);
        public int bossWinAwardToken;
        public BossBattleResultType bossBattleResultType;
        public short rareItem;

        public String bossBattleResultTypeName(){
            return bossBattleResultType != null ? bossBattleResultType.name() : "";
        }
    }

    public EndBattle() {
    }

    public EndBattle(WhichLevelEnum type, BattleResultEnum result, int battleId, short missionId) {
        this.type = type;
        this.result = result;
        this.battleId = battleId;
        this.missionId = missionId;
        this.banNote = "";
        this.collectedReagents = new byte[0];
        this.items = new BackpackItemStructure[0];
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "EndBattle{" +
                "result=" + BattleResultEnum.valueOf((int) (resultRaw - battleId)) +
                ", type=" + type +
                ", expBonus=" + expBonus +
                ", battleId=" + battleId +
                ", missionId=" + missionId +
                ", banType=" + banType +
                ", banNote=" + banNote +
                ", items=[" + (items == null ? 0 : items.length) +
                ", collectedReagents=" + Arrays.toString(collectedReagents) +
                ", totalTurnsCount=" + totalTurnsCount +
                ", totalDamageToPlayer=" + totalDamageToPlayer +
                ", totalDamageToBoss=" + totalDamageToBoss +
                ", totalUsedItems=[" + (totalUsedItems == null ? 0 : totalUsedItems.length) +
                ", secureResult=" + secureResult +
                '}';
    }

}
