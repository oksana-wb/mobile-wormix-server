package com.pragmatix.app.messages.structures;

import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 22.01.2016 9:57
 *         <p>
 *         Структура - лог хода в миссии
 */
@Structure
public class TurnStructure {
    /**
     * номер хода в бою
     */
    public short turnNum;
    /**
     * кто ходил
     */
    public boolean isPlayerTurn;
    /**
     * время окончания хода: время реального прихода команды (UNIX Timestamp)
     */
    public int endTime;
    /**
     * урон, полученный _игроком_ (независимо от того, чей ход)
     */
    public int damageToPlayer;
    /**
     * урон, полученный _боссом_ (независимо от того, чей ход)
     */
    public int damageToBoss;
    /**
     * массив потраченого оружия за ход тем, чей был ход
     */
    public BackpackItemStructure[] items;
    /**
     * массив убитых червей
     */
    public BossWormStructure[] deaths;
    /**
     * массив порождённых червей
     */
    public BossWormStructure[] births;

    public TurnStructure() {
    }

    public TurnStructure(int turnNum, boolean isPlayerTurn, BackpackItemStructure[] items, BossWormStructure[] deaths, BossWormStructure[] births) {
        this.turnNum = (short) turnNum;
        this.isPlayerTurn = isPlayerTurn;
        this.items = items;
        this.deaths = deaths;
        this.births = births;
    }

    @Override
    public String toString() {
        return "TurnStructure{" +
                "turnNum=" + turnNum +
                ", isPlayerTurn=" + isPlayerTurn +
                ", damageToPlayer=" + damageToPlayer +
                ", damageToBoss=" + damageToBoss +
                ", items=" + Arrays.toString(items) +
                '}';
    }
}
