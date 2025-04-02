package com.pragmatix.app.common;

import com.pragmatix.app.services.CheatersCheckerService;
import com.pragmatix.gameapp.common.TypeableEnum;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 01.02.2016 11:08
 *         <p>
 *         Причина признания боя читерским
 */
public enum CheatTypeEnum implements TypeableEnum {
    /**
     * неизвестно - бой даже и не проходил проверку
     */
    UNCHECKED(-1, Severity.UNDEFINED),
    /**
     * Всё хорошо, чита не было
     */
    OK(0, Severity.ZERO),
    /**
     * Число ходов на клиенте и на сервере не сошлось или = 0
     */
    TURN_COUNT_MISMATCH(1, Severity.REPORT),
    /**
     * номера ходов дублируются или приходят в неверном порядке
     */
    TURN_NUMBER_MISMATCH(2, Severity.DISCARD),
    /**
     * клиент прислал нереально низкий HP для босса
     */
    BOSS_HP_LOW(3, Severity.REPORT),
    /**
     * урон боссу < его HP или не сходится между клиентом и сервером (выше порога {@link CheatersCheckerService#hpTolerance}
     */
    DAMAGE_TO_BOSS_MISMATCH(4, Severity.REPORT), // TODO: сделать Severity.DISCARD когда все ошибки с расчётом дамага на клиенте будут исправлены
    /**
     * урон игроку >= его HP (т.е. он на самом деле убит) или не сходится между клиентом и сервером (выше порога {@link CheatersCheckerService#hpTolerance}
     */
    DAMAGE_TO_PLAYER_MISMATCH(5, Severity.REPORT), // TODO: сделать Severity.DISCARD когда все ошибки с расчётом дамага на клиенте будут исправлены
    /**
     * количество использованного оружия на каждом ходе и всего не совпадают
     */
    ITEMS_USED_MISMATCH(6, Severity.DISCARD),
    /**
     * клиент прислал нереально большой урон от оружия за один выстрел/ход (выше порога {@link CheatersCheckerService#maxPossibleDamagePerTurn})
     */
    WEAPON_DAMAGE_HIGH(7, /*!!!*/ Severity.IMMEDIATE_BAN),
    /**
     * оружие было использовано за ход или за бой слишком много раз
     */
    WEAPON_USAGE_HIGH(8, Severity.DISCARD),
    /**
     * инициализирующий {@link com.pragmatix.app.messages.client.EndTurn} не пришёл или содержит некорректную информацию
     */
    INIT_WRONG(9, Severity.DISCARD),
    /**
     * изначальный HP, пришедший от клиента как сумма червей, не совпадает с его HP по версии сервера (выше порога {@link CheatersCheckerService#hpTolerance}
     */
    PLAYER_HP_MISMATCH(10, Severity.REPORT), // TODO: сделать Severity.DISCARD когда все ошибки с расчётом HP будут исправлены
    /**
     * босс не получил свой ход после хода игрока (два хода игрока подряд)
     */
    BOSS_TURN_SKIPPED(11, Severity.DISCARD),
    /**
     * более тяжёлый вариант BOSS_TURN_SKIPPED: босс вообще ни разу не сходил
     * NB: немедленный бан
     */
    BOSS_TURN_NEVER(12, /*!!!*/ Severity.REPORT),
    /**
     * подозрение на {@link CheatTypeEnum#PLAYER_HP_MISMATCH}
     * изначальный HP, пришедший от клиента как сумма червей, не совпадает с его HP по версии сервера (ниже порога {@link CheatersCheckerService#hpTolerance}
     */
    PLAYER_HP_MISMATCH_MAYBE(13, Severity.REPORT),
    /**
     * подозрение на {@link CheatTypeEnum#DAMAGE_TO_BOSS_MISMATCH}
     * урон боссу < его HP или не сходится между клиентом и сервером (ниже порога {@link CheatersCheckerService#hpTolerance}
     */
    DAMAGE_TO_BOSS_MISMATCH_MAYBE(14, Severity.REPORT),
    /**
     * подозрение на {@link CheatTypeEnum#DAMAGE_TO_PLAYER_MISMATCH}
     * урон игроку >= его HP (т.е. он на самом деле убит) или не сходится между клиентом и сервером (ниже порога {@link CheatersCheckerService#hpTolerance}
     */
    DAMAGE_TO_PLAYER_MISMATCH_MAYBE(15, Severity.REPORT),
    /**
     * игрок утопил босса, не используя при этом никакого оружия ("чит на матрицу")
     */
    SINK_WITHOUT_WEAPON(16, Severity.REPORT), // TODO: сделать discard/ban, когда убедимся, что не дает false positive
    /**
     * подозрение на {@link CheatTypeEnum#WEAPON_DAMAGE_HIGH}
     * клиент прислал подозрительно большой урон от оружия за один выстрел/ход (выше порога {@link CheatersCheckerService#maxNormalDamagePerTurn}, но ниже {@link CheatersCheckerService#maxPossibleDamagePerTurn})
     */
    WEAPON_DAMAGE_HIGH_MAYBE(17, Severity.REPORT),
    /**
     * сообщение {@link com.pragmatix.app.messages.client.EndBattle} от клиента не прошло проверку
     * @see CheatersCheckerService#validateEndBattleMsg(com.pragmatix.app.model.UserProfile, com.pragmatix.app.messages.client.EndBattle) - reason устанавливается здесь
     */
    PROTOCOL_HACK(18, Severity.DISCARD),
    /**
     * бой закончился подозрительно быстро
     * @see CheatersCheckerService#checkInstantWin(com.pragmatix.app.model.UserProfile, long, BattleResultEnum) - reason устанавливается здесь
     */
    INSTANT_WIN_MAYBE(19, Severity.REPORT),
    /**
     * бой закончился слишком быстро более чем {@link CheatersCheckerService#MAX_CHEATS_INSTANT_WIN} раз подряд
     * либо игрок победил, не сделав ни одного хода (однозначно бан)
     * @see CheatersCheckerService#checkInstantWin(com.pragmatix.app.model.UserProfile, long, BattleResultEnum) - reason устанавливается здесь
     */
    INSTANT_WIN(20, /*!!!*/ Severity.IMMEDIATE_BAN),
    /**
     * лог боя есть, но не содержит ни одного хода (ни игрока, ни босса). возможно, игрок полностью заблокировал отправку команды {@link com.pragmatix.app.messages.client.EndTurn}
     * (в этом случае все остальные валидации пропускаются, т.к. становятся бессмысленны)
     */
    NO_TURNS(21, /*!!!*/ Severity.IMMEDIATE_BAN),
    /**
     * в PVP оружие либо не было выбрано перед выстрелом, либо команда выбора имела неверный параметр
     */
    WEAPON_SELECT_ILLEGAL(22, Severity.DISCARD),
    /**
     * в PVP повторно пришла клиентская команда {@link com.pragmatix.pvp.messages.battle.client.PvpActionEx.ActionCmd#endTurn} - это чит
     */
    ACTION_END_TURN_REPEAT(23, Severity.DISCARD),
    /**
     * в PVP оружие было использовано за ход слишком много раз
     */
    PVP_WEAPON_USAGE_HIGH(24, Severity.DISCARD),
    /**
     * ход длился больше 60 секунд
     */
    LONG_TURN(25, Severity.REPORT),
    /**
     * игрок прошел подозрительно быстро сложного босса (начиная с {@link CheatersCheckerService#fastWinMinBossId})
     */
    FAST_WIN(26, Severity.REPORT),
    /**
     * сообщаем, что часть ходов была в оффлайне и мы не можем провалидировать время хода
     */
    WIN_IN_OFFLINE(27, Severity.REPORT),
    ;

    /**
     * Степень серьёзности чита (и какое, следовательно, предпринимается действие)
     *
     * На этом множестве определён строгий порядок: т.е. объявленные ниже "больше" объявленных раньше в терминах {@link Comparable#compareTo(Object)}
     */
    private enum Severity {

        /**
         * не установлено => действие не определено
         */
        UNDEFINED,
        /**
         * нет чита => действие не требуется
         */
        ZERO,
        /**
         * подозрение на чит => добавляется в отчёт
         */
        REPORT,
        /**
         * точно читерство => бой/ход не засчитывается
         */
        DISCARD,

        // ...

        /**
         * точно _серьёзное_ читерство => бой/ход не засчитывается + немедленный бан (на 2 недели)
         */
        IMMEDIATE_BAN
    }

    private int type;
    private Severity severity;

    CheatTypeEnum(int type, Severity severity) {
        this.type = type;
        this.severity = severity;
    }

    @Override
    public int getType() {
        return type;
    }

    private Severity getSeverity() {
        return severity;
    }

    public boolean isValid() {
        return ! isMoreSevereThan(OK);
    }

    public boolean mustBeReported() {
        return severity.compareTo(Severity.REPORT) >= 0;
    }

    /**
     * @return true если бой без всяких сомнений является читерским и не засчитывается, иначе false
     */
    public boolean mustBeDiscarded() {
        return severity.compareTo(Severity.DISCARD) >= 0;
    }

    /**
     * @return true если бой не только не засчитывается, но игрок немедленно отправляется сервером в бан, иначе false
     */
    public boolean mustBeBanned() {
        return severity.compareTo(Severity.IMMEDIATE_BAN) >= 0;
    }

    /**
     * Проверка, что серьёзность другого чита _строго_ больше данного
     *
     * @param another другой вид чита
     * @return true если {@code this} обладает более сильной severity чем {@code another}, иначе (в том числе если severity равны) false
     */
    public boolean isMoreSevereThan(CheatTypeEnum another) {
        return this.severity.compareTo(another.getSeverity()) > 0;
    }

    @Override
    public String toString() {
        return name() + "=>" + severity;
    }
}
