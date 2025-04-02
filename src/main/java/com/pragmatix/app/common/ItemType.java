package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * Типы покупок (предмет, группа)
 *
 * @author denis
 *         Date: 13.01.2010
 *         Time: 22:42:59
 */
public enum ItemType implements TypeableEnum {

    NONE(-1),

    WEAPON(0),
    ADD_IN_GROUP(1),
    BATTLE(2),
    PARAMETERS(3),
    REMOVE_FROM_GROUP(4),
    STUFF(7),
    RACE(8),
    WIPE(9),
    REACTION_RATE(10),
    UNLOCK_MISSION(11),
    UPGRADE_WEAPON(12),
    DOWNGRADE_WEAPON(14),
    RESET_BONUS_ITEMS(15),
    ASSEMBLE_STUFF(16),
    REASSEMBLE_STUFF(17),
    OPEN_CHEST(18),
    REACTION_LEVEL(27),
    SELECT_RACE(28),
    RENAME(29),
    EXTRA_GROUP_SLOT(30),
    MERCENARIES_TICKET(31),
    QUEST_REWARD(32),
    SKIN(33),

    SPECIAL_DEAL_ITEM(19),
    COLISEUM_TICKET(20),

    CREATE_CLAN(21),
    EXPAND_CLAN(22),
    RENAME_CLAN(23),
    CHANGE_CLAN_DESCRIPTION(24),
    CHANGE_CLAN_EMBLEM(25),
    DONATION(26);


    private int type;

    ItemType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
