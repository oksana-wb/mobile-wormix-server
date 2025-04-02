package com.pragmatix.pvp.services;

import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.task.TaskLock;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.messages.handshake.client.CreateBattleRequest;
import com.pragmatix.pvp.model.BattleBuffer;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.battletracking.BattleStateTrackerFactory;
import com.pragmatix.pvp.services.battletracking.PvpBattleTrackerService;
import com.pragmatix.pvp.services.matchmaking.BattleProposal;
import com.pragmatix.pvp.services.matchmaking.TeamBattleProposal;
import com.pragmatix.pvp.services.matchmaking.UserBattleProposal;
import com.pragmatix.pvp.services.matchmaking.lobby.WagerMatchmakingLobby;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.11.12 11:21
 */
@Service
public class BattleFactory {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private PvpService pvpService;

    @Resource
    private PvpBattleTrackerService pvpBattleTrackerService;

    @Resource
    private BattleStateTrackerFactory battleStateTrackerFactory;

    @Resource
    private CraftService craftService;

    @Value("${BattleFactory.trackPvpActionsEnabled:true}")
    private boolean trackPvpActionsEnabled = true;

    public BattleBuffer createBattle(PvpUser creator, BattleWager wager) {
        switch (wager.battleType) {
            case FRIEND_PvP:
                return createFriendBattle(creator);
            case PvE_FRIEND:
                return createPveBattle(creator, wager);
            case PvE_PARTNER:
                return createPvePartnerBattle(creator, wager);
            case WAGER_PvP_DUEL:
            case WAGER_PvP_3_FOR_ALL:
            case WAGER_PvP_2x2:
                if(wager == BattleWager.WAGER_50_2x2_FRIENDS)
                    return createWagerBattle(creator, wager, new TaskLock());
                else
                    return createWagerBattle(creator, wager, new TaskLock());
        }
        return null;
    }

    public BattleBuffer createFriendBattle(PvpUser creator) {
        CreateBattleRequest cmd = creator.getCreateBattleRequest();
        long battleId = pvpService.getNextBattleId();
        BattleBuffer battleBuffer = new BattleBuffer(battleId, cmd.wager.battleType, (int) cmd.mapId, battleStateTrackerFactory.getBattleStateTracker(cmd.wager.battleType, cmd.wager));
        battleBuffer.setWager(cmd.wager);
        battleBuffer.setMissionId(cmd.missionIds);
        battleBuffer.setCreator(creator);
        Set<Byte> teams = new HashSet<>();
        for(int i = 0; i < cmd.participants.length; i++) {
            long participantId = cmd.participants[i];
            byte socialNetId = creator.getSocialNetId();
            if(participantId != creator.getProfileId() && cmd.friendSocialNetId > 0)
                socialNetId = cmd.friendSocialNetId;
            BattleParticipant battleParticipant = new BattleParticipant(participantId, socialNetId, BattleParticipant.State.needProfile, i, cmd.teamIds[i], creator.getMainServer());
            battleBuffer.addParticipant(battleParticipant);
            teams.add(battleParticipant.getPlayerTeam());
        }
        battleBuffer.setTurningNumByTeam(new byte[teams.size()]);
        battleBuffer.getTurningNumByTeam()[0] = 0;

        battleBuffer.reagentsForBattle = ArrayUtils.EMPTY_BYTE_ARRAY;

        pvpBattleTrackerService.addBattleIfAbsent(battleBuffer);
        return battleBuffer;
    }

    public BattleBuffer createPveBattle(PvpUser creator, BattleWager wager) {
        CreateBattleRequest cmd = creator.getCreateBattleRequest();
        long battleId = pvpService.getNextBattleId();
        BattleBuffer battleBuffer = new BattleBuffer(battleId, cmd.wager.battleType, (int) cmd.mapId, battleStateTrackerFactory.getBattleStateTracker(cmd.wager.battleType, cmd.wager));
        battleBuffer.setWager(wager);
        battleBuffer.setMissionId(cmd.missionIds);
        battleBuffer.setCreator(creator);
        Set<Byte> teams = new HashSet<>();
        for(int i = 0; i < cmd.participants.length; i++) {
            long participantId = cmd.participants[i];
            byte socialNetId = creator.getSocialNetId();
            if(participantId != creator.getProfileId() && cmd.friendSocialNetId > 0)
                socialNetId = cmd.friendSocialNetId;
            BattleParticipant battleParticipant = new BattleParticipant(participantId, socialNetId, BattleParticipant.State.needProfile, i, 0, creator.getMainServer());
            battleBuffer.addParticipant(battleParticipant);
            teams.add(battleParticipant.getPlayerTeam());
        }
        for(int i = 0; i < cmd.missionTeamSize; i++) {
            int missionBotNum = cmd.participants.length + i;
            BattleParticipant battleParticipant = new BattleParticipant(missionBotNum, (byte) 0, BattleParticipant.State.connectedAndHasProfile, missionBotNum, 1, null);
            battleBuffer.addParticipant(battleParticipant);
            teams.add(battleParticipant.getPlayerTeam());
        }
        battleBuffer.setTurningNumByTeam(new byte[teams.size()]);
        battleBuffer.getTurningNumByTeam()[0] = 0;
        battleBuffer.startPvpBattleLogIf(trackPvpActionsEnabled);

        pvpBattleTrackerService.addBattleIfAbsent(battleBuffer);
        return battleBuffer;
    }

    public BattleBuffer createPvePartnerBattle(PvpUser creator, BattleWager wager) {
        CreateBattleRequest cmd = creator.getCreateBattleRequest();
        long battleId = pvpService.getNextBattleId();
        BattleBuffer battleBuffer = new BattleBuffer(battleId, cmd.wager.battleType, (int) cmd.mapId, battleStateTrackerFactory.getBattleStateTracker(cmd.wager.battleType, cmd.wager));
        battleBuffer.setWager(wager);
        battleBuffer.setMissionId(cmd.missionIds);
        battleBuffer.setCreator(creator);
        battleBuffer.setLock(new TaskLock());
        // добавляем в участники пока только себя
        for(int i = 0; i < cmd.participants.length; i++) {
            long participantId = cmd.participants[i];
            byte socialNetId = creator.getSocialNetId();
            if(participantId != creator.getProfileId() && cmd.friendSocialNetId > 0)
                socialNetId = cmd.friendSocialNetId;
            BattleParticipant battleParticipant = new BattleParticipant(participantId, socialNetId, BattleParticipant.State.needProfile, -1, 0, creator.getMainServer());
            battleBuffer.addParticipant(battleParticipant);
        }
        battleBuffer.startPvpBattleLogIf(trackPvpActionsEnabled);

        pvpBattleTrackerService.addBattleIfAbsent(battleBuffer);
        return battleBuffer;
    }

    public BattleBuffer createWagerBattle(PvpUser creator, BattleWager wager, TaskLock lock) {
        CreateBattleRequest cmd = creator.getCreateBattleRequest();
        long battleId = pvpService.getNextBattleId();
        BattleBuffer battleBuffer = new BattleBuffer(battleId, wager.battleType, (int) cmd.mapId, battleStateTrackerFactory.getBattleStateTracker(wager.battleType, cmd.wager));
        battleBuffer.setWager(wager);
        battleBuffer.setCreator(creator);
        battleBuffer.setLock(lock);
        // добавляем в участники пока только себя и друга если есть
        for(int i = 0; i < cmd.participants.length; i++) {
            long participantId = cmd.participants[i];
            byte socialNetId = creator.getSocialNetId();
            if(participantId != creator.getProfileId() && cmd.friendSocialNetId > 0)
                socialNetId = cmd.friendSocialNetId;
            BattleParticipant battleParticipant = new BattleParticipant(participantId, socialNetId, BattleParticipant.State.needProfile, i, 0, creator.getMainServer());
            battleBuffer.addParticipant(battleParticipant);
        }
        battleBuffer.startPvpBattleLogIf(trackPvpActionsEnabled);

        pvpBattleTrackerService.addBattleIfAbsent(battleBuffer);
        return battleBuffer;
    }

    public void mergeParticipantsIntoWagerBattle(BattleProposal creator, List<BattleProposal> battleProposals, WagerMatchmakingLobby matchmakingLobby) {
        BattleBuffer battle = creator.getBattleBuffer();
        int levelForReagents = 0;

        if(battle.getBattleType() == PvpBattleType.WAGER_PvP_DUEL
                || battle.getBattleType() == PvpBattleType.WAGER_PvP_3_FOR_ALL
                ) {
            // перво наперво выставляем подобранным участникам новый battleId
            // и инициатору, т.к. заявки могли быть поданы оптом
            ((UserBattleProposal) creator).getUser().setBattleId(battle.getBattleId());
            for(BattleProposal battleProposal : battleProposals) {
                ((UserBattleProposal) battleProposal).getUser().setBattleId(battle.getBattleId());
            }

            int teamsCount = battle.getBattleType().getNeedParticipants() + 1;

            List<BattleProposal> participants = new ArrayList<BattleProposal>(battleProposals.size() + 1);
            participants.add(creator);
            participants.addAll(battleProposals);

            if(log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder("merge battles:");
                for(BattleProposal participant : participants) {
                    sb.append("\n").append(participant.getBattleBuffer().dumpBattleInitInfo());
                }
                log.debug(sb.toString());
            }

            // определяем очередность хода
            matchmakingLobby.playTurn(participants);

            List<BattleParticipant> battleParticipants = new ArrayList<BattleParticipant>();
            int levelsSum = 0;
            for(int i = 0; i < participants.size(); i++) {
                UserBattleProposal battleProposal = (UserBattleProposal) participants.get(i);
                battleProposal.setLobbyTime(System.currentTimeMillis() - battleProposal.getLobbyTime());

                BattleParticipant battleParticipant = battleProposal.getBattleParticipant();
                battleParticipant.setPlayerNum((byte) i);
                battleParticipant.setPlayerTeam((byte) i);

                battleParticipants.add(battleParticipant);

                levelsSum += battleProposal.getLevel();
            }
            battle.getParticipants().clear();
            battle.getParticipants().addAll(battleParticipants);

            battle.setTurningNumByTeam(new byte[teamsCount]);

            levelForReagents = levelsSum / participants.size();
        } else if(battle.getBattleType() == PvpBattleType.WAGER_PvP_2x2) {
            // перво наперво выставляем подобранным участникам новый battleId
            // и инициатору, т.к. заявки могли быть поданы оптом
            setBattleId((TeamBattleProposal) creator, battle.getBattleId());
            for(BattleProposal battleProposal : battleProposals) {
                setBattleId((TeamBattleProposal) battleProposal, battle.getBattleId());
            }

            List<BattleProposal> participants = new ArrayList<BattleProposal>(battleProposals.size() + 1);
            participants.add(creator);
            participants.addAll(battleProposals);

            List<TeamBattleProposal> teams = new ArrayList<TeamBattleProposal>();
            if(log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder("merge battles:");
                for(BattleProposal participant : teams) {
                    sb.append("\n").append(participant.getBattleBuffer().dumpBattleInitInfo());
                }
                log.debug(sb.toString());
            }

            if(allHaveSize(participants, 1)) {
                Collections.shuffle(participants);

                // объединяем в команды
                TeamBattleProposal team1 = new TeamBattleProposal((UserBattleProposal) participants.get(0), (UserBattleProposal) participants.get(1));
                TeamBattleProposal team2 = new TeamBattleProposal((UserBattleProposal) participants.get(2), (UserBattleProposal) participants.get(3));

                teams.add(team1);
                teams.add(team2);
            } else if(allHaveSize(participants, 2)) {
                teams.add((TeamBattleProposal) participants.get(0));
                teams.add((TeamBattleProposal) participants.get(1));
            } else {
                UserBattleProposal userBattleProposal1 = null;
                UserBattleProposal userBattleProposal2 = null;
                TeamBattleProposal team = null;
                for(BattleProposal battleProposal : participants) {
                    if(battleProposal.getSize() == 1) {
                        if(userBattleProposal1 == null) {
                            userBattleProposal1 = (UserBattleProposal) battleProposal;
                        } else {
                            userBattleProposal2 = (UserBattleProposal) battleProposal;
                        }
                    } else {
                        team = (TeamBattleProposal) battleProposal;
                    }
                }
                teams.add(new TeamBattleProposal(userBattleProposal1, userBattleProposal2));
                teams.add(team);
            }

            levelForReagents = fillBattleParticipantsFor(battle, teams, matchmakingLobby);
        } else if(battle.getBattleType() == PvpBattleType.PvE_PARTNER) {
            PvpUser creatorPvpUser = ((UserBattleProposal) creator).getUser();
            // выставляем подобранным участникам новый battleId
            creatorPvpUser.setBattleId(battle.getBattleId());
            for(BattleProposal battleProposal : battleProposals) {
                ((UserBattleProposal) battleProposal).getUser().setBattleId(battle.getBattleId());
            }

            List<BattleProposal> participants = new ArrayList<BattleProposal>(2);
            participants.add(creator);
            participants.addAll(battleProposals);

            if(log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder("merge battles:");
                for(BattleProposal participant : participants) {
                    sb.append("\n").append(participant.getBattleBuffer().dumpBattleInitInfo());
                }
                log.debug(sb.toString());
            }

            List<BattleParticipant> battleParticipants = new ArrayList<BattleParticipant>(4);
            for(int i = 0; i < participants.size(); i++) {
                UserBattleProposal battleProposal = (UserBattleProposal) participants.get(i);
                battleProposal.setLobbyTime(System.currentTimeMillis() - battleProposal.getLobbyTime());

                BattleParticipant battleParticipant = battleProposal.getBattleParticipant();
                battleParticipant.setPlayerNum((byte) i);

                battleParticipants.add(battleParticipant);
            }

            CreateBattleRequest cmd = battle.getCreator().getCreateBattleRequest();
            for(int i = 0; i < cmd.missionTeamSize; i++) {
                int missionBotNum = participants.size() + i;
                BattleParticipant battleParticipant = new BattleParticipant(missionBotNum, (byte) 0, BattleParticipant.State.connectedAndHasProfile, missionBotNum, 1, null);
                battleParticipants.add(battleParticipant);
            }
            battle.setTurningNumByTeam(new byte[2]);

            battle.getParticipants().clear();
            battle.getParticipants().addAll(battleParticipants);
        } else {
            throw new IllegalArgumentException("Не предусмотренный тип боя! [" + battle.getBattleType() + "]");
        }

        if(levelForReagents > 0) {
            battle.reagentsForBattle = craftService.getReagentsForPvpBattle(battle.getWager(), levelForReagents);
        }
    }

    private boolean allHaveSize(List<BattleProposal> battleProposals, int size) {
        for(BattleProposal battleProposal : battleProposals) {
            if(battleProposal.getSize() != size)
                return false;
        }
        return true;
    }

    public void setBattleId(TeamBattleProposal battleProposal, long battleId) {
        for(PvpUser pvpUser : battleProposal.getTeam()) {
            pvpUser.setBattleId(battleId);
        }
    }

    private int fillBattleParticipantsFor(BattleBuffer battle, List<TeamBattleProposal> teams, WagerMatchmakingLobby matchmakingLobby) {
        // определяем очередность хода
        matchmakingLobby.playTurn(teams);

        List<BattleParticipant> battleParticipants = new ArrayList<BattleParticipant>(4);
        int levelsSum = 0;
        int playerNum = 0;
        for(int i = 0; i < teams.size(); i++) {
            byte playerTeam = (byte) i;
            TeamBattleProposal team = teams.get(i);
            PvpUser[] usersTeam = team.getTeam();
            for(int j = 0; j < usersTeam.length; j++) {
                BattleParticipant battleParticipant = team.getTeamParticipants().get(j);
                battleParticipant.setPlayerNum((byte) playerNum);
                battleParticipant.setPlayerTeam(playerTeam);

                // первоначальная заяка игрока (в случае WAGER_PvP_2x2_FRIENDS она совпадает с team)
                TeamBattleProposal battleProposal = battleParticipant.getBattleProposal();
                battleProposal.setLobbyTime(System.currentTimeMillis() - battleProposal.getLobbyTime());

                battleParticipants.add(battleParticipant);
                playerNum++;
            }

            levelsSum += team.getLevel();
        }
        battle.getParticipants().clear();
        battle.getParticipants().addAll(battleParticipants);

        battle.setTurningNumByTeam(new byte[teams.size()]);

        return levelsSum / teams.size();
    }

}
