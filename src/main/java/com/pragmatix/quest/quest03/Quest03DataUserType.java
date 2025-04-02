package com.pragmatix.quest.quest03;

import com.pragmatix.quest.dao.QuestDataUserType;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.06.2016 12:25
 */
public class Quest03DataUserType extends QuestDataUserType {

    @Override
    public Class<com.pragmatix.quest.quest03.Data> returnedClass() {
        return Data.class;
    }

}
