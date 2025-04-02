package com.pragmatix.pvp.services.matchmaking.lobby;

import javax.validation.constraints.NotNull;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 26.04.13 15:27
 */
public class ProposalStat {

    public enum MatchType {
        SANDBOX,
        LEVEL,
        TEAM_SIZE,
        HP,
        EXTRA,
        SKILL,
        RANK,
    }

    public int matchedCandidats = 0;

    private int notMatchByLevelFor = 0;

    private int notMatchByHpFor = 0;

    private int notMatchByTeamSizeFor = 0;

    private int notMatchBySkillFor = 0;

    private int notMatchBySandboxFor = 0;

    private int notMatchByRankFor = 0;

    private int notMatchByExtraFor = 0;

    private AtomicInteger notMatchByLevelThis = new AtomicInteger();

    private AtomicInteger notMatchByHpThis = new AtomicInteger();

    private AtomicInteger notMatchByTeamSizeThis = new AtomicInteger();

    private AtomicInteger notMatchBySkillThis = new AtomicInteger();

    private AtomicInteger notMatchBySandboxThis = new AtomicInteger();

    private AtomicInteger notMatchByExtraThis = new AtomicInteger();

    private AtomicInteger notMatchByRankThis = new AtomicInteger();

    public void incNotMatchCounterFor(@NotNull MatchType type) {
        switch (type) {
            case LEVEL:
                notMatchByLevelFor++; break;
            case HP:
                notMatchByHpFor++; break;
            case TEAM_SIZE:
                notMatchByTeamSizeFor++; break;
            case SANDBOX:
                notMatchBySandboxFor++; break;
            case EXTRA:
                notMatchByExtraFor++; break;
            case SKILL:
                notMatchBySkillFor++; break;
            case RANK:
                notMatchByRankFor++; break;
        }
    }

    public void incNotMatchCounterThis(@NotNull MatchType type) {
        getNotMatchCounterThis(type).incrementAndGet();
    }

    private AtomicInteger getNotMatchCounterThis(@NotNull MatchType type) {
        switch (type) {
            case LEVEL:
                return notMatchByLevelThis;
            case HP:
                return notMatchByHpThis;
            case TEAM_SIZE:
                return notMatchByTeamSizeThis;
            case SANDBOX:
                return notMatchBySandboxThis;
            case EXTRA:
                return notMatchByExtraThis;
            case SKILL:
                return notMatchBySkillThis;
            case RANK:
                return notMatchByRankThis;
        }
        return new AtomicInteger();
    }

    public String printNotMatchByThis() {
        return String.format("%s/%s/%s/%s/%s/%s/%s", notMatchBySandboxThis, notMatchByLevelThis, notMatchByTeamSizeThis, notMatchByHpThis, notMatchByExtraThis, notMatchBySkillThis, notMatchByRankThis);
    }

    public String printNotMatchByFor() {
        return String.format("%s/%s/%s/%s/%s/%s/%s", notMatchBySandboxFor, notMatchByLevelFor, notMatchByTeamSizeFor, notMatchByHpFor, notMatchByExtraFor, notMatchBySkillFor, notMatchByRankFor);
    }
/*
    grid:0 (massMatchedCandidats:1/ratingMatchedCandidats:0) sandbox:0/level:4/hp:1/skill:1 # 0/5/0/1
    0(0/0)0/4/1/0:0/5/0/1
    0(0/0)0/4/0/0:0/5/0/0
    0(0/0)0/0/0/0:0/0/0/0
    0(0/0)0/6/0/0:0/14/0/4
    0(0/0)0/2/0/0:0/3/1/0
    0(0/0)0/2/0/0:0/3/2/0
    0(0/0)0/4/1/0:0/2/0/0
    0(0/0)0/5/0/0:0/11/4/1
    0(1/0)0/5/0/1:0/1/1/0
    0(0/0)0/4/0/0:0/1/0/1

*/

}
