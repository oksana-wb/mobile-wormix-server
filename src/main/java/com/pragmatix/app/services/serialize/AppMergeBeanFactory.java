package com.pragmatix.app.services.serialize;

import com.pragmatix.clan.structures.ClanMemberStructure;
import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.serialization.DefaultMergeBeanFactoryImpl;
import org.springframework.stereotype.Service;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.04.2016 11:03
 */
@Service
public class AppMergeBeanFactory extends DefaultMergeBeanFactoryImpl {

    public AppMergeBeanFactory() {
        regFactory(ClanMemberStructure.class, markedFields -> {
                    int clanId = (int) markedFields.get("clanId");
                    if(clanId > 0) {
                        ClanMember clanMember = new ClanMember();
                        clanMember.clan = new Clan();
                        clanMember.clan.id = clanId;
                        return new ClanMemberStructure(clanMember);
                    } else {
                        return new ClanMemberStructure(null);
                    }
                }
        );
    }

}
