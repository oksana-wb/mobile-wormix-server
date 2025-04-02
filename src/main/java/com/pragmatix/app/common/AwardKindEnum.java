package com.pragmatix.app.common;

import com.pragmatix.app.services.events.*;
import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * Что может быть выдано в игре в виде награды
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 01.10.12 10:28
 */
public enum AwardKindEnum implements TypeableEnum {

    NONE(0),
    /**
     * @see com.pragmatix.app.services.events.AddMoneyEvent
     */
    MONEY(1),
    /**
     * @see com.pragmatix.app.services.events.AddRealMoneyEvent
     */
    REAL_MONEY(2, true),
    /**
     * @see com.pragmatix.app.services.events.AddBattlesCountEvent
     * @see com.pragmatix.app.services.events.SetBattlesCountEvent
     */
    BATTLES_COUNT(3),
    /**
     * @see com.pragmatix.app.services.events.AddReactionRateEvent
     */
    REACTION_RATE(4),
    /**
     * @see AddWeaponShotEvent
     */
    WEAPON_SHOT(5),
    /**
     * @see AddTemporalStuffEvent
     */
    TEMPORARY_STUFF(6, true),
    /**
     * @see AddReagentEvent
     */
    REAGENT(7),
    /**
     * @see AddStuffEvent
     */
    STUFF(8, true),
    /**
     * @see AddRaceEvent
     */
    RACE(9, true),
    /**
     * @see AddWeaponEvent
     */
    WEAPON(10, true),
    /**
     * @see com.pragmatix.app.services.events.AddExperienceEvent
     */
    EXPERIENCE(11),
    /**
     * @see com.pragmatix.app.services.events.AddSkinEvent
     */
    SKIN(12),
    /**
     * @see com.pragmatix.app.services.events.AddWagerWinAwardTokenEvent
     */
    WAGER_AWARD_TOKEN(13),
    /**
     * @see com.pragmatix.app.services.events.AddBossWinAwardTokenEvent
     */
    BOSS_AWARD_TOKEN(14),

    BONUS_EXPERIENCE(101),
    EXTRA_MONEY(102),
    RENAME(103),
    ;

    private final int type;

    // фиксировать ли награду в БД
    private final boolean statable;

    AwardKindEnum(int type) {
        this.type = type;
        this.statable = false;
    }

    AwardKindEnum(int type, boolean statable) {
        this.type = type;
        this.statable = statable;
    }

    @Override
    public int getType() {
        return type;
    }

    public boolean isStatable() {
        return statable;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }

}
