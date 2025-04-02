package com.pragmatix.pvp.messages.battle.client;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
* @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
*         Created: 08.12.12 23:45
*/
public enum DropReasonEnum implements TypeableEnum {
    /**
     * номальное завершение боя
     */
    NORMAL(0),
    /**
     * если игрок сдался
     */
    SURRENDER(1),
    /**
     * если игрок считерил,
     * типо техническое поражение за читерство
     */
    OPPONENT_CHEATER(2),
    /**
     * игра зависла(на ожидании данных)
     */
    STUCK(3),
    /**
     * приходит с сервера если сервер поймал меня на читерстве
     */
    I_AM_CHEATER(4),
    /**
     * игра(рассинхронизировалась
     */
    DECYNC(5),
    /**
     * игра завислана на ожидании команды от противника
     */
    OPPONENT_TIMEOUT_COMMAND(6),
    /**
     * игра завислана на ожидании команды от меня
     */
    MINE_TIMEOUT_COMMAND(7),
    /**
     * игра завислана на ожидании команды подтверждения передачи хода от противника
     */
    OPPONENT_TIMEOUT_RESPONCE(8),
    /**
     * игра завислана на ожидании команды подтверждения передачи хода от меня
     */
    MINE_TIMEOUT_RESPONCE(9),
    /**
     * не найден бой по battleId
     */
    BATTLE_NOT_EXIST(10),
    /**
     * в указонном бое данный участник не найден
     */
    ABSENT_IN_BATTLE(11),;

    private int type;

    @Override
    public int getType() {
        return type;
    }

    DropReasonEnum(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }
}
