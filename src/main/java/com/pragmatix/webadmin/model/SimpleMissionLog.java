package com.pragmatix.webadmin.model;

import com.pragmatix.app.common.CheatTypeEnum;
import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.app.messages.structures.Time_ConnectionState;
import com.pragmatix.app.messages.structures.TurnStructure;

import java.util.List;

public class SimpleMissionLog {
    /**
     * результат валидации: true - всё сошлось, false - игрок читерил
     */
    public boolean valid;
    /**
     * в случае valid=false - причина, по которой бой признан читерским
     */
    public CheatTypeEnum reason = CheatTypeEnum.UNCHECKED;
    /**
     * в случае valid=false - к каким последствиям для игрока приведет:
     * DISCARD - отмена боя
     * BAN - бан
     */
    public String consequence;
    /**
     * в случае valid=false - величина расхождения (для тех типов, к которым применимо):
     * desynchValue = supposed(client) - actual(sever)
     */
    public int desynchValue;
    /**
     * Изначальное количество здоровья игрока (по версии сервера на момент начала битвы)
     */
    public int startPlayerHP;
    /**
     * Полное количество здоровья игрока (просуммированное сервером по появившимся червям от клиента за всю битву)
     */
    public int playerHP;
    /**
     * Полное количество здоровья босса в данной битве (просуммированное сервером по появившимся червям от клиента за всю битву)
     */
    public int bossHP;
    /**
     * Показывает, что HP уже были проинициализированны (после прихода нулевого хода)
     */
    public boolean initedHP = false;
    /**
     * Полное количество ходов в течение боя (по версии клиента в конце битвы)
     */
    public short supposedTotalTurnsCount;
    /**
     * Полный урон, полученный _игроком_ (по версии клиента в конце битвы)
     */
    public int supposedTotalDamageToPlayer;
    /**
     * Полный урон, полученный _боссом_ (по версии клиента в конце битвы)
     */
    public int supposedTotalDamageToBoss;
    /**
     * Всё оружие, потраченное _игроком_ за бой (по версии клиента в конце битвы)
     */
    public List<BackpackItemStructure> supposedTotalItems = List.of();
    /**
     * Лог всех ходов в рамках этого боя (накопленный сервером)
     */
    public List<TurnStructure> turns = List.of();

    public List<Time_ConnectionState> connectionEvents = List.of();
    /**
     * Описание состава команды на момент начала боя (какие юниты, какие на них шапки, каков уровень, HP и т.д)
     * Каждый элемент списка относится к одному члену команды и является набором пар "ключ-значение"
     * (Список может быть пустым в случае, если валидация боя не выявила ошибок)
     */
    public List<TeamUnit> teamSnapshot = List.of();

    public static class TeamUnit {
        public long id;
        public int type;
        public short hat;
        public int hatHp;
        public short kit;
        public int kitHp;
        public int maxHpLimit;
        public int level;
        public int levelHp;
        public int race;
        public int raceHp;
        public int hp;
        public int attack;
        public int armor;

        @Override
        public String toString() {
            return "{" +
                    "id=" + id +
                    ", hat=" + hat +
                    ", hatHp=" + hatHp +
                    ", kit=" + kit +
                    ", kitHp=" + kitHp +
                    ", maxHpLimit=" + maxHpLimit +
                    ", level=" + level +
                    ", levelHp=" + levelHp +
                    ", race=" + race +
                    ", raceHp=" + raceHp +
                    ", hp=" + hp +
                    ", attack=" + attack +
                    ", armor=" + armor +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "{" +
                "valid=" + valid +
                ", reason=" + reason +
                ", consequence='" + consequence + '\'' +
                ", desynchValue=" + desynchValue +
                ", startPlayerHP=" + startPlayerHP +
                ", playerHP=" + playerHP +
                ", bossHP=" + bossHP +
                ", initedHP=" + initedHP +
                ", supposedTotalTurnsCount=" + supposedTotalTurnsCount +
                ", supposedTotalDamageToPlayer=" + supposedTotalDamageToPlayer +
                ", supposedTotalDamageToBoss=" + supposedTotalDamageToBoss +
                ", supposedTotalItems=" + supposedTotalItems +
                ", turns=" + turns +
                ", connectionEvents=" + connectionEvents +
                ", teamSnapshot=" + teamSnapshot +
                '}';
    }
}
