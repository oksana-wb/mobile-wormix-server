package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * За что в игре может быть выдана награда
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 01.10.12 10:34
 */
public enum AwardTypeEnum implements TypeableEnum {

    // логин в бонусный период
    BONUS_DAYS(1, true, true),
    // ежедневный бонус
    DAILY_BONUS(2, true, true),
    // возврат друга в игру
    COMEBACKED_FRIEND(3, true, false),
    // возврат в игру после долгого перерыва
    COMEBACK(4, true, true),
    // обыск домика
    SEARCH_THE_HOUSE(5, false, true),
    // выдано админом
    ADMIN(6, false, true),
    // награда за бой
    BATTLE(8, false, true),
    // награда за попадание в дневной топ
    DAILY_TOP(9, false, true),
    // клан занял призовое место по итогам сезона
    TOP_CLAN(10, true, true),
    // зашел по реферальной ссылке
    REFERRAL_LINK(11, true, false),
    // временная акция
    ACTION(12, false, true),
    // компенсация за исключение из клана
    EXPELLED_FROM_CLAN(15, false, true),
    // обналичил медали
    CASH_MEDALS(16, false, true),
    // закрыл клан с не пустой казной
    DELETE_CLAN(17, false, true),
    // завершил серию гладиаторских боёв
    GLADIATOR_BATTLE(18, false, true),
    // завершил квест
    QUEST_FINISH(19, false, true),
    // завершил битву наёмников
    MERCENARIES_BATTLE(20, false, true),
    // открыл сундук и выпали деньги
    OPEN_CHEST(21, false, true),
    // увеличился уровень
    LEVEL_UP(22, false, true),
    // награда за достижение (логгируется в базу отдельно)
    ACHIEVE(23, false, false),
    // компенсация за изъятое сезонное оружие
    SEASON_COMPENSATION(24, false, false),
    // награда по итогам сезона
    TOP_SEASON(25, true, true),
    // дополнительная награда за супербоссов в зависимости от числа успешных попыток
    HEROIC_BOSS_EXTRA_AWARD(26, false, false),
    // продажа шапки/артефакта
    SELL_STUFF(27, false, true),
    // выдача оружия после перебалансировки
    LEVEL_UP_WEAPONS(28, true, false),
    // забираем апгрейды ставшие недоступными игроку по уровню. Компенсируем рубинами
    DOWNGRADE_WEAPONS_COMPENSATION(29, true, true),
    // выдача наград после перебалансировки наград за уровни
    LEVEL_UP_AWARDS(30, true, false),

    // удвоение награды за увеличение уровня за просмотр рекламы
    REPEAT_LEVEL_UP_AWARD(31, false, true),
    // удвоение ежедневного бонуса  за просмотр рекламы
    REPEAT_DAILY_BONUS(32, false, true),

    BOSS_BATTLE_EXTRA(36, false, true),
    ;
    private final int type;

    // выдаётмся исключительно в момент логина в игру
    public final boolean byLoginOnly;

    // необходимо фиксировать в базе
    public boolean statable;

    AwardTypeEnum(int type, boolean byLoginOnly, boolean statable) {
        this.type = type;
        this.byLoginOnly = byLoginOnly;
        this.statable = statable;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), type);
    }

}
