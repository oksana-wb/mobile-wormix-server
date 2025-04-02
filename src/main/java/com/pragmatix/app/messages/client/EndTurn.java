package com.pragmatix.app.messages.client;

import com.pragmatix.app.messages.structures.TurnStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;
import com.pragmatix.serialization.annotations.Resize;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 21.01.2016 17:58
 *
 *         Отчёт серверу об окончании хода в миссии с боссом (лог хода)
 *
 * @see com.pragmatix.app.controllers.BattleController#onEndTurn(EndTurn, UserProfile)
 */
@Command(120)
public class EndTurn extends SecuredCommand {
    /**
     * id миссии
     * 0 - бой с ботами
     */
    public short missionId;

    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;

    public short fakeByteArraySize; // путаем следы: делаем вид, что дальше идёт byte[]

    public TurnStructure turn;

    /**
     * во время хода была обнаружена попытка взлома
     */
    public short banType;
    /**
     * доп. информация по бану
     */
    public String banNote;

    public String sessionKey;

    @Override
    public String getSessionKey() {
        return sessionKey;
    }

    @Override
    public String toString() {
        return "EndTurn{" +
                "missionId=" + missionId +
                ", battleId=" + battleId +
                ", turn=" + turn +
                (banType > 0 ?  ", banType=" + banType +
                ", banNote=" + banNote : "") +
                ", sessionKey=" + sessionKey +
                ", secureResult=" + secureResult +
                '}';
    }
}
