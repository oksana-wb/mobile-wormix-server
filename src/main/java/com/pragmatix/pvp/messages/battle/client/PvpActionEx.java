package com.pragmatix.pvp.messages.battle.client;

import com.pragmatix.gameapp.secure.SecuredCommand;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import javax.validation.constraints.Null;

/**
 * Команда содержит массив событий которые произошли в pvp бою
 * User: denis
 * Date: 19.05.2010
 * Time: 0:40:22
 *
 * @see com.pragmatix.pvp.controllers.PvpController#onPvpExCommand(PvpActionEx, com.pragmatix.pvp.model.PvpUser)
 */
@Command(1007)
public class PvpActionEx extends SecuredCommand implements CountedCommandI {

    /**
     * номер фрейма, с которого начинается период
     */
    @Resize(TypeSize.UINT32)
    public long firstFrame;

    /**
     * номер фрейма, которым оканчивается период
     */
    @Resize(TypeSize.UINT32)
    public long lastFrame;

    public short turnNum;

    public short commandNum;
    /**
     * уникальный id боя
     */
    @Resize(TypeSize.UINT32)
    public long battleId;

    public byte playerNum;
    /**
     * "сырая" последовательность клиентских команд
     *
     * @see <a href="http://gitlab.pragmatix-corp.com/wormix/client/blob/experiments/src/ru/pragmatix/wormix/serialization/client/PvpActionExBinarySerializer.as#L114-135">PvpActionExBinarySerializer.as</a> - клиентский десериализатор
     */
    @Resize(TypeSize.UINT32)
    public long[] ids;


    /**
     * Поддерживаемые парсером коды клиентских команд, содержащихся в массиве ids
     */
    public enum ActionCmd {
        selectWeapon    (0x00004200, Kind.BACKPACK),
        selectWeaponSrv (0x00004204, Kind.OTHER), // аналог selectWeapon, обрабатывается только сервером
        charge          (0x00002104, Kind.SHOOT),
        release         (0x00002005, Kind.SHOOT),
        point           (0x00002206, Kind.SHOOT),
        jumpShoot       (0x00001105, Kind.SHOOT), // заменяет любой другой ожидаемый выстрел
        cancelShot      (0x00002009, Kind.OTHER), // отменяет предыдущий выстрел
        endTurn         (0x00005000, Kind.CONTROL),
        backpackOpen    (0x00004001, Kind.BACKPACK),
        backpackClose   (0x00004002, Kind.BACKPACK),
        jump1           (0x00001003, Kind.JUMP),
        jump2           (0x00001003, Kind.JUMP),

        ;

        enum Kind {
            SHOOT,
            BACKPACK,
            CONTROL,
            JUMP,
            OTHER
        }

        // код приходящий от клиента
        private final long code;

        // к какому классу команд относится эта команда?
        private final Kind kind;

        ActionCmd(long code, Kind kind) {
            this.code = code;
            this.kind = kind;
        }

        public long getCode() {
            return code;
        }

        public boolean isShoot() {
            return kind == Kind.SHOOT;
        }

        public boolean isBackpack() {
            return kind == Kind.BACKPACK;
        }

        public boolean isControl() {
            return kind == Kind.CONTROL;
        }

        public boolean isJump() {
            return kind == Kind.JUMP;
        }

        public static long paramsCount(long cmdId) {
            return (cmdId & 0x00000F00) >> 8;
        }

        @Null
        public static ActionCmd valueOf(long code) {
            for (ActionCmd actionCmd : values()) {
                if (actionCmd.code == code) {
                    return actionCmd;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return name() + "(0x" + Long.toHexString(code) + ')';
        }
    }

    public PvpActionEx() {
    }

    public PvpActionEx(long battleId, int turnNum, int commandNum, int playerNum) {
        this.battleId = battleId;
        this.turnNum = (short) turnNum;
        this.commandNum = (short) commandNum;
        this.playerNum = (byte) playerNum;
        this.ids = new long[0];
    }

    @Override
    public String getSessionKey() {
        return null;
    }

    @Override
    public String toString() {
        return "PvpActionEx{" +
                "battleId=" + battleId +
                ", turnNum=" + turnNum +
                ", commandNum=" + commandNum +
                ", playerNum=" + playerNum +
                ", firstFrame=" + firstFrame +
                ", lastFrame=" + lastFrame +
                ", secureResult=" + secureResult +
                '}';
    }

    @Override
    public short getCommandNum() {
        return commandNum;
    }

    @Override
    public short getTurnNum() {
        return turnNum;
    }

    @Override
    public byte getPlayerNum() {
        return playerNum;
    }

    @Override
    public long getBattleId() {
        return battleId;
    }
}
