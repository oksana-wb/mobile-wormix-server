package com.pragmatix.app.messages.structures;

import com.pragmatix.app.common.CheatTypeEnum;
import com.pragmatix.app.common.ConnectionState;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 * Created: 22.01.2016 15:17
 * <p>
 * Структура - лог всего боя с боссом
 */
@Structure(nullable = true)
public class MissionLogStructure {
    /**
     * результат валидации: true - всё сошлось, false - игрок читерил
     */
    public boolean valid;

    /**
     * в случае valid=false - причина, по которой бой признан читерским
     */
    private CheatTypeEnum reason = CheatTypeEnum.UNCHECKED;

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
    private int desynchValue;

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
    public BackpackItemStructure[] supposedTotalItems;

    /**
     * Лог всех ходов в рамках этого боя (накопленный сервером)
     */
    public List<TurnStructure> turns = new ArrayList<>();

    public List<Time_ConnectionState> connectionEvents = new ArrayList<>();

    /**
     * Описание состава команды на момент начала боя (какие юниты, какие на них шапки, каков уровень, HP и т.д)
     * Каждый элемент списка относится к одному члену команды и является набором пар "ключ-значение"
     * (Список может быть пустым в случае, если валидация боя не выявила ошибок)
     */
    @Ignore
    public final List<Map<String, Object>> teamSnapshot = new ArrayList<>();

    public int offlineTime() {
        int result = 0;
        int disconnectTime = 0;
        for(Time_ConnectionState connectionEvent : connectionEvents) {
            if(connectionEvent.state == ConnectionState.disconnect){
                disconnectTime = connectionEvent.time;
            } else if(connectionEvent.state == ConnectionState.connect && disconnectTime > 0){
                result += connectionEvent.time - disconnectTime;
            }
        }
        return result;
    }

    public void clear() {
        turns.clear();
    }

    public void start(int initialPlayerHP) {
        this.startPlayerHP = initialPlayerHP;
    }

    public void add(TurnStructure turn) {
        turns.add(turn);
    }

    public void finish(short totalTurnsCount, int totalDamageToPlayer, int totalDamageToBoss, BackpackItemStructure[] totalItems) {
        this.supposedTotalTurnsCount = totalTurnsCount;
        this.supposedTotalDamageToPlayer = totalDamageToPlayer;
        this.supposedTotalDamageToBoss = totalDamageToBoss;
        this.supposedTotalItems = totalItems;
    }

    /**
     * Устанавливает reason, перетирая прежний только в том случае, если {@code newReason} более серьезен, чем предыдущий
     *
     * @param newReason новый reason
     * @return то значение, которое оказалось в итоге: если было заменено, то новое, иначе прежнее.
     */
    public CheatTypeEnum setReason(CheatTypeEnum newReason) {
        return setReason(newReason, 0);
    }

    /**
     * Устанавливает reason и desynchValue, перетирая прежние только в том случае, если {@code newReason} более серьезен, чем предыдущий
     *
     * @param newReason    новый reason
     * @param desynchValue величина расхождения для newReason
     * @return то значение reason, которое оказалось в итоге: если было заменено, то новое, иначе прежнее.
     */
    public CheatTypeEnum setReason(CheatTypeEnum newReason, int desynchValue) {
        if(newReason.isMoreSevereThan(this.reason)) {
            this.reason = newReason;
            // + устанавливаем consequence
            if(this.reason.mustBeBanned()) {
                consequence = "BAN";
            } else if(this.reason.mustBeDiscarded()) {
                consequence = "DISCARD";
            }
            // + устанавливаем desynchValue, относящийся именно к этому виду
            this.desynchValue = desynchValue;
        }
        return this.reason;
    }

    public CheatTypeEnum getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "MissionLogStructure{" +
                "valid=" + valid +
                ", reason=" + reason +
                ", playerHP=" + playerHP +
                ", bossHP=" + bossHP +
                ", supposedTotalTurnsCount=" + supposedTotalTurnsCount +
                ", supposedTotalDamageToPlayer=" + supposedTotalDamageToPlayer +
                ", supposedTotalDamageToBoss=" + supposedTotalDamageToBoss +
                ", supposedTotalItems=[" + (supposedTotalItems != null ? supposedTotalItems.length : "null") + ']' +
                ", turns=[" + turns.size() + ']' +
                '}';
    }
}
