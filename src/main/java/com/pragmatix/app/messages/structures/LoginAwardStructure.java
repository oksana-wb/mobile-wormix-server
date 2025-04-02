package com.pragmatix.app.messages.structures;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.messages.server.AwardGranted;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Награды, выдаваемые игроку в момент логина
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 01.10.12 11:14
 */
@Structure
public class LoginAwardStructure {
    /**
     * за что награда
     */
    public AwardTypeEnum awardType;
    /**
     * чем награждаем
     */
    public List<GenericAwardStructure> awards;
    /**
     * доп. параметр
     */
    public String attach = "";

    public LoginAwardStructure() {
    }

    public LoginAwardStructure(AwardGranted message) {
        this.awardType = message.awardType;
        this.awards = message.awards;
        this.attach = message.attach;
    }

    public LoginAwardStructure(AwardTypeEnum awardType, List<GenericAwardStructure> awards, String attach) {
        this.awardType = awardType;
        this.awards = awards;
        this.attach = attach;
    }

    @Override
    public String toString() {
        return "{" + awardType.name() +
                " " + awards +
                (!attach.isEmpty() ? " attach='" + attach + '\'' : "") +
                '}';
    }
}
