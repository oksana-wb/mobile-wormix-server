package com.pragmatix.pvp.messages.battle.client;

import com.pragmatix.chat.ChatAction;
import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.pvp.messages.PvpCommandI;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.SecureResult;
import com.pragmatix.serialization.annotations.Resize;

/**
 * Команда отправляеться клиентом когда необходимо отправить сообщение в чат
 * 
 * Created by IntelliJ IDEA.
 * User: denver
 * Date: 20.03.2010
 * Time: 1:44:48
 *
 * @see com.pragmatix.pvp.controllers.PvpController#onPvpChatMessage(PvpChatMessage, com.pragmatix.pvp.model.PvpUser)
 */
@Command(17)
public class PvpChatMessage extends SecuredCommand implements PvpCommandI {

    /**
	 * playerNum участника который отправил сообщение
	 */
	public byte playerNum;
    /**
     * тип сообщения. Обычное мли стикер
     */
	public ChatAction action;
    /**
	 * сообщение в чат
	 */
	public String message;
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;
    /**
     * сообщение членам команды
     */
    public boolean teamsMessage;


    public PvpChatMessage() {
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public long getBattleId() {
        return battleId;
    }

    @Override
    public String toString() {
        return "PvpChatMessage{" +
                "playerNum=" + playerNum +
                ", action=" + action +
                ", message='" + message + '\'' +
                ", battleId=" + battleId +
                ", teamsMessage=" + teamsMessage +
                ", secureResult=" + secureResult +
                '}';
    }
}
