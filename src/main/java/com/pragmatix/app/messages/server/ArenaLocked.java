package com.pragmatix.app.messages.server;

import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;

import java.lang.reflect.Type;

/**
 * команда от сервера клиенту говорит о том, что
 * для запроса арены нужно подождать такое-то количество секунд
 * или что проход миссии заблокирован
 * <p/>
 * User: denis
 * Date: 05.12.2009
 * Time: 21:24:54
 */
@Command(10007)
public class ArenaLocked {

    public enum ArenaLockedCause implements TypeableEnum {

        CAUSE_NO_BATTLES(1),
        CAUSE_MISSION_LOCKED(2),
        CAUSE_WRONG_MISSION(3),;

        private int type;

        ArenaLockedCause(int type) {
            this.type = type;
        }

        @Override
        public int getType() {
            return type;
        }
    }

    @Resize(TypeSize.UINT32)
    public long delay;

    public short currentMission;

    public ArenaLockedCause cause;

    public ArenaLocked() {
    }

    public ArenaLocked(long delay, short currentMission) {
        this.delay = delay;
        this.currentMission = currentMission;
        this.cause = ArenaLockedCause.CAUSE_NO_BATTLES;
    }

    public ArenaLocked(ArenaLockedCause cause, short currentMission) {
        this.currentMission = currentMission;
        this.cause = cause;
    }

    @Override
    public String toString() {
        return "ArenaLocked{" +
                "delay=" + delay +
                ", currentMission=" + currentMission +
                ", cause=" + cause +
                '}';
    }
}
