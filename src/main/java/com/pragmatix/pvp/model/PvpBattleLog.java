package com.pragmatix.pvp.model;

import com.pragmatix.app.common.CheatTypeEnum;
import com.pragmatix.pvp.messages.battle.client.PvpActionEx;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 11.07.2016 12:16
 *         <p>
 * Журнал боя, подлежащий валидации для отлова читеров в PVP
 *
 * @see com.pragmatix.app.messages.structures.MissionLogStructure - аналог в single player миссиях
 */
public class PvpBattleLog {

    /**
     * сводка всех ходов боя
     */
    private Deque<Turn> turns = new LinkedList<>();

    /**
     * должен ли журнал боя быть сохранен на диск? (определяется в зависимости от хода боя и обнаружения читов)
     */
    private transient boolean needToBeSaved = false;

    public class Turn {

        /**
         * Номер хода
         */
        public int turnNum;

        /**
         * Время начала
         */
        public long start;

        /**
         * Кто ходит (номер по счету)
         */
        public byte playerNum;

        /**
         * результат валидации: true - всё сошлось, false - игрок читерил
         */
        public boolean valid = true;

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
         * в случае valid=false - фрейм, на котором был определён данный чит
         */
        public Long cheatFrame;

        /**
         * Сколько выстрелов за этот ход сделано каждым оружием: {weaponId -> count}
         */
        public Map<Integer, AtomicInteger> shotsByWeapon = new HashMap<>();

        /**
         * Какое оружие сейчас выбрано
         */
        public int currentWeaponId;

        /**
         * Количество последовательных выстрелов из текущего выбранного оружия
         */
        transient public int consecutiveShotActions;

        /**
         * Может ли пришедший ранее выстрел ещё пока быть отменен приходом {@link PvpActionEx.ActionCmd#cancelShot}
         */
        transient public boolean shotMightBeCancelled = false;

        /**
         * Текущая ожидаемая команда, которая будет означать выстрел (одна из charge/release/point)
         */
        transient public PvpActionEx.ActionCmd currentShootCommand = PvpActionEx.ActionCmd.release;

        /**
         * Флаг: ход был окончен приходом команды {@link PvpActionEx.ActionCmd#endTurn}
         */
        transient public boolean finished = false;

        
        public Turn(int turnNum, byte playerNum, long startTime) {
            this.turnNum = turnNum;
            this.playerNum = playerNum;
            this.start = startTime;
        }

        public CheatTypeEnum setReason(CheatTypeEnum newReason, long frame) {
            if (newReason.isMoreSevereThan(this.reason)) {
                this.reason = newReason;
                if (!newReason.isValid()) {
                    this.cheatFrame = frame;
                    // + устанавливаем consequence
                    if (this.reason.mustBeBanned()) {
                        this.consequence = "BAN";
                    } else if (this.reason.mustBeDiscarded()) {
                        this.consequence = "DISCARD";
                    }
                }
            }
            return this.reason;
        }

        public CheatTypeEnum getReason() {
            return reason;
        }
    }

    public Turn getCurrentTurn() {
        return turns.getLast();
    }

    public void startNewTurnBy(BattleParticipant player, int turnNum, long startTime) {
        if (!player.isEnvParticipant()) {
            turns.addLast(new Turn(turnNum, player.getPlayerNum(), startTime));
        }
    }

    public boolean isNeedToBeSaved() {
        return needToBeSaved;
    }

    public void setNeedToBeSaved(boolean needToBeSaved) {
        this.needToBeSaved = needToBeSaved;
    }
}
