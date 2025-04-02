package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.gameapp.secure.SecuredResponse;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.util.Arrays;
import java.util.List;

/**
 * команда от сервера говорит о начале боя
 */
@Command(10006)
public class StartBattleResult implements SecuredResponse {
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;
    /**
     * массив id реагентов, которые могут выпасть в бою. длинной от 0-3
     * id могут повторяться
     */
    public byte[] reagentsForBattle;
    /**
     * награда в случае поражения
     */
    public List<GenericAwardStructure> defeatAward;

    public String sessionKey;

    public StartBattleResult() {
    }

    public StartBattleResult(long battleId, byte[] reagentsForBattle, List<GenericAwardStructure> defeatAward, String sessionKey) {
        this.battleId = battleId;
        this.reagentsForBattle = reagentsForBattle;
        this.defeatAward = defeatAward;
        this.sessionKey = sessionKey;
    }

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "StartBattleResult{" +
                "battleId=" + battleId +
                ", reagentsForBattle=" + Arrays.toString(reagentsForBattle) +
                ", defeatAward=" + defeatAward +
                '}';
    }
}
