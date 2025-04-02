package com.pragmatix.pvp.services.matchmaking;

import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.services.matchmaking.lobby.ProposalStat;
import jskills.ITeam;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.11.12 14:30
 */
public interface BattleProposal extends Comparable<BattleProposal> {

    boolean tryLock();

    void unlock();

    boolean inValidState();

    int getSize();

    int getLevel();

    int getRating();

    int getDailyRating();

    // для реализации песочницы (первые N боёв)
    int getBattlesCount();

    int getGroupHp();

    int getGroupSize();

    int[] getReactionLevel();

    BattleWager getWager();

    BattleBuffer getBattleBuffer();

    void cancel();

    ITeam getJSkillTeam();

    void setMatchQuality(double matchQuality);

    int getGridQuality();

    void incGridQuality();

    ProposalStat getStat();

    Object getAuxMatchParams();

    int getVersion();

    String getClientAddress();

    byte getSocialNetId();

    int getRank();
}
