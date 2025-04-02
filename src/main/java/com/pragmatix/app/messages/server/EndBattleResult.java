package com.pragmatix.app.messages.server;

import com.pragmatix.app.common.BattleResultEnum;
import com.pragmatix.app.messages.client.EndBattle;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Resize;

import java.util.List;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 * Created: 21.01.2016 17:05
 * <p>
 * ответ от сервера о корректности завершения боя после валидации его результата
 * @see com.pragmatix.app.messages.client.EndBattle
 * @see com.pragmatix.app.controllers.BattleController#onEndBattle(com.pragmatix.app.messages.client.EndBattle, com.pragmatix.app.model.UserProfile)
 */
@Command(10121)
public class EndBattleResult implements SecuredResponse {

    public enum EndBattleValidateResult implements TypeableEnum {
        OK(0),
        CHEAT(1),
        DUPLICATED(2),
        INVALID(3);

        public final int type;

        EndBattleValidateResult(int type) {
            this.type = type;
        }

        @Override
        public int getType() {
            return type;
        }
    }

    /**
     * Результат окончания боя, полученный от клиента
     */
    public EndBattleValidateResult validateResult;
    /**
     * Результат окончания боя, полученный от клиента
     */
    @Resize(TypeSize.UINT32)
    public long result;
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;
    /**
     * Id миссии
     * 0 - бой с ботом
     */
    public short missionId;

    public List<GenericAwardStructure> award;

    @Ignore
    public BattleResultEnum cleanedResult;

    public String sessionKey;

    public EndBattleResult() {
    }

    public EndBattleResult(EndBattleValidateResult validateResult, EndBattle msg, List<GenericAwardStructure> award, String sessionKey) {
        this.validateResult = validateResult;

        this.result = msg.resultRaw;
        this.battleId = msg.battleId;
        this.missionId = msg.missionId;

        this.award = award;

        this.cleanedResult = msg.result;
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "EndBattleResult(" + validateResult + "){" +
                "result=" + cleanedResult +
                ", battleId=" + battleId +
                ", missionId=" + missionId +
                ", award=" + award +
//                ", sessionKey='" + sessionKey + '\'' +
                '}';
    }
}
