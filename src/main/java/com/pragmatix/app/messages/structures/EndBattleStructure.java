package com.pragmatix.app.messages.structures;

import com.pragmatix.app.common.BattleResultEnum;
import com.pragmatix.app.common.WhichLevelEnum;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 04.04.2014 14:41
 */
@Structure
public class EndBattleStructure {

    /**
     * время начала боя (в секундах)
     */
    public int startBattleTime;
    /**
     * время окончания боя (в секундах)
     */
    public int finishBattleTime;
    /**
     * результат боя
     */
    public BattleResultEnum result;
    /**
     * тип боя (дрались с уровнем выше/меньше)
     */
    public WhichLevelEnum type;
    /**
     * получено, бонусного опыта
     */
    public int expBonus;
    /**
     * id миссии
     * 0 - бой с ботами
     */
    public short missionId;
    /**
     * массив потраченого оружия в уровне
     */
    public BackpackItemStructure[] items;
    /**
     * во время боя была обнаружена попытка взлома
     */
    public short banType;
    /**
     * доп. информация по бану
     */
    public String banNote;

    @Override
    public String toString() {
        return "{" +
                "start=" + AppUtils.formatDateInSeconds(startBattleTime) +
                ", finish=" + AppUtils.formatDateInSeconds(finishBattleTime) +
                ", result=" + result +
                ", type=" + type +
                ", expBonus=" + expBonus +
                ", missionId=" + missionId +
                (items != null && items.length > 0 ? ", items=" + Arrays.toString(items) : "") +
                (banType > 0 ? ", banType=" + banType +
                        ", banNote='" + banNote + '\'' : "") +
                '}';
    }
}
