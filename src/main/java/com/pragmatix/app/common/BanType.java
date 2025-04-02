package com.pragmatix.app.common;

import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * Author: Oksana Shevchenko
 * Date: 19.12.2010
 * Time: 7:08:27
 */
public enum BanType implements TypeableEnum {

    // Баны выдаваемые сервером
    BAN_FOR_INVALID_FRIEND_LOGIN(28, 1, false, "прислал id друга который ему не друг, прислал id при логине"),
    BAN_FOR_INVALID_FRIEND(29, 1, false, "прислал id друга который ему не друг (была запрошена платформа)"),
    BAN_FOR_SEARCH_NOT_EXISTENT_USER(30, 30, false, "читерил при обыске, а именно обыскивал не зарегистрированных в игре игроков"),
    BAN_FOR_BATTLE_ID(31, 1, false, "отправил несуществующий id боя в команде конца боя"),
    BAN_FOR_BATTLE_RESULT(32, 0, false, "подмена результата боя"),
    BAN_FOR_INSTANT_WIN(33, 7, false, "8 боев подряд менее чем за 10сек каждый"),
    BAN_FOR_HOUSE_SEARCH(35, 7, false, "слишком быстро обыскивал, или обладает нечеловеческим везением"),
    BAN_FOR_PARAM_HACK(36, 0, false, "взлом параметров червяка"),
    BAN_FOR_FRIENDS_LIST_SUBSTITUTION(37, 30, false, "за подмену списка друзей"),
    BAN_FOR_FRIENDS_LIMIT_EXCEED(38, 30, false, "подменил список и послал более 10т друзей"),
    BAN_FOR_WEAR_WRONG_HAT(39, 1, false, "пытался одеть шапку которой у него нет"),
    DEPRECATED_BAN(41, 0, false, "бан за подмену списка друзей(устаревший"),
    BAN_FOR_SHOP_HACK(42, 0, false, "взлом магазина"),
    BAN_FOR_BOT(43, 1, false, "обнаружен бот"),
    BAN_FOR_BOT_RECIDIVIST(44, 0, false, "бот рецидивист"),
    BAN_FOR_LOGIN_HACK(46, 1, false, "команда Login не прошла проверку"),
    BAN_FOR_FAKE_COMMAND(47, 7, false, "бот прислал фейковую комманду"),
    BAN_FOR_CANCEL_PURCHASE(53, 0, false, "отмена платежа"),
    BAN_FOR_BOSS_HACK(54, 14, false, "чит в бое с боссом: бой не прошёл валидацию"),
    BAN_FOR_PVP_HACK(55, 14, false, "чит в PVP: ход игрока не прошёл валидацию"),

    PROFILE_MOVED_TO(61, 0, false, "профиль был перенесен в другую соц. сеть"),
    PROFILE_MOVED_FROM(62, -1, false, "профиль был перенесен из другой соц. сети"),

    // Баны выдаваемые админами
    BAN_BY_ADMIN(34, 7, true, "использование и распространение программ для взлома игры (разбанивать запрещено)"),
    BAN_FOR_USE_HACKING_PROGRAM(40, 0, true, "Использование и распространение программ для взлома игры"),
    BAN_FOR_CUSSING(45, 1, true, "матерился в чате"),
    BAN_RESERVED(48, 3, true, "за редактирование памяти игры (мягкий)"),
    BAN_FOR_USE_CHEAT_ENGINE(49, 21, true, "за редактирование памяти игры (жесткий)"),
    BAN_FOR_CHAT_CHEATING(51, 1, true, "ввод в заблуждение"),
    BAN_FOR_ADMIN_OFFENSE(52, 3, true, "оскорбление администрации"),
    BAN_FOR_FOUL(63, 3, true, "нарушение правил игры"),
    ;

    public final int type;
    public final int durationInDays;
    public final boolean manual;
    public final String caption;

    /**
     * @param type           тип бана
     * @param durationInDays на сколько дней бан
     */
    BanType(int type, int durationInDays, boolean manual, String caption) {
        this.type = type;
        this.durationInDays = durationInDays;
        this.manual = manual;
        this.caption = caption;
    }

    public int getType() {
        return type;
    }

    public int getDurationInDays() {
        return durationInDays;
    }

    public boolean isManual() {
        return manual;
    }

    public String getCaption() {
        return caption;
    }

    public static BanType valueOf(int type) {
        for(BanType banType : BanType.values()) {
            if(banType.getType() == type) {
                return banType;
            }
        }
        return null;
    }
}
