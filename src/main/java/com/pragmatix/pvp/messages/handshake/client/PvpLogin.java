package com.pragmatix.pvp.messages.handshake.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.gameapp.secure.SecuredLogin;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.SecureResult;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Structure;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 20.11.12 12:02
 */
@Structure(isAbstract = true)
public abstract class PvpLogin extends SecuredLogin {
    /**
     * id своего профайла
     */
    @Resize(TypeSize.UINT32)
    public long profileId;

    /**
     * id игрока в соц. сети
     */
    public String profileStringId;

    /**
     * id социальной сети
     */
    public byte socialNetId;

    /**
     * автризационный ключ
     */
    public String authKey;

    /**
     * имя игрока в соц сети
     */
    public String profileName;

    /**
     * адрес главного игрового сервера игрока
     */
    public short mainServerId;

    @Override
    public String toString() {
        return "profileId=" + socialNetId+":"+profileId +
                ", stringId='" + profileStringId + '\'' +
                ", profileName='" + profileName + '\'' +
                ", mainServerId=" + mainServerId;
    }
}
