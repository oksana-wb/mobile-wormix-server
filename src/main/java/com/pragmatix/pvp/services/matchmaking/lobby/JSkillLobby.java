package com.pragmatix.pvp.services.matchmaking.lobby;

import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.services.TrueSkillService;
import com.pragmatix.app.services.rating.RankService;
import com.pragmatix.common.utils.VarObject;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.WagerValue;
import com.pragmatix.pvp.model.PvpUser;
import com.pragmatix.pvp.services.PvpDailyRegistry;
import com.pragmatix.pvp.services.matchmaking.BattleProposal;
import com.pragmatix.pvp.services.matchmaking.BlackListService;
import com.pragmatix.pvp.services.matchmaking.TeamBattleProposal;
import jskills.ITeam;
import jskills.Rating;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 10.04.13 12:18
 */
public abstract class JSkillLobby implements WagerMatchmakingLobby {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private TrueSkillService trueSkillService;

    @Resource
    private LobbyConf lobbyConf;

    @Resource
    private PvpDailyRegistry dailyRegistry;

    @Resource
    private BlackListService blackListService;

    @Resource
    private LevelCreator levelCreator;

    @Autowired(required = false)
    private RankService rankService;

    /**
     * вернет профайлы сопостовимые по силе и мастерству
     * который сделал такую же ставку как и @profile, для проведения соревнования
     *
     * @param battleProposal профайл игрока который сделал ставку
     * @param quantity       сколько соперников необходимо подобрать
     * @param candidates     будут добавлены найденные профайлы в количестве quantity или меньше
     */
    @Override
    public boolean findCandidatesByWager(BattleProposal battleProposal, int quantity, List<BattleProposal> candidates) {
        //берем профайлы по ставке
        Set<BattleProposal> proposalsByWager = getBattleProposals(battleProposal);
        Map<BattleProposal, String> notMatchReasonByCandidate = new HashMap<>();

        //фильтруем по версии клиента
        List<BattleProposal> proposalsByVersion = new ArrayList<>(proposalsByWager.size());
        for(BattleProposal proposal : proposalsByWager) {
            if(proposal.getSocialNetId() != battleProposal.getSocialNetId()
                    && !lobbyConf.isMatchDifferentPlatform()) {
                notMatchReasonByCandidate.put(proposal, "SOCIAL_NET");
                continue;
            }
            if(proposal.getVersion() != battleProposal.getVersion()) {
                notMatchReasonByCandidate.put(proposal, "VERSION");
                continue;
            }

            proposalsByVersion.add(proposal);
        }

        // собираем в набор противников подходящих по силе/массе
        List<BattleProposal> candidatesByMass = new ArrayList<>();
        addSameMassProposals(battleProposal, proposalsByVersion, candidatesByMass, quantity > 1, notMatchReasonByCandidate);

        if(log.isDebugEnabled()) {
            log.debug("wager:{} for {} there are same mass candidates:\n {})", battleProposal.getWager().getValue(), battleProposal, candidatesByMass);
        }

        List<ImmutablePair<BattleProposal, Double>> candidatesByMatchQuality = new ArrayList<>();

        //пробегаем по профайлам противников и ищем подходящего
        for(BattleProposal candidateProposal : candidatesByMass) {
            boolean mayMatched;
            //учитываем недавнего противника, или игнорируем это если такая проверка отключена в настройках
            if(lobbyConf.isCheckOpponents()) {
                // с эти противником игрок до этого не играл (больше чем положено)
                VarObject<String> reason = new VarObject<>();
                mayMatched = mayMatched(battleProposal, candidateProposal, reason);
                if(mayMatched) {
                    // а также не играл с остальными
                    for(ImmutablePair<BattleProposal, Double> candidateByMatchQuality : candidatesByMatchQuality) {
                        mayMatched = mayMatched(candidateProposal, candidateByMatchQuality.left, reason);
                        if(!mayMatched) {
                            notMatchReasonByCandidate.put(candidateProposal, "EACH_OTHER:" + reason.value);
                            break;
                        }
                    }
                } else {
                    notMatchReasonByCandidate.put(candidateProposal, reason.value);
                }
            } else {
                mayMatched = true;
            }

            if(mayMatched) {
                double matchQuality = trueSkillService.calculateMatchQuality(battleProposal, candidateProposal);

                if(matchByRating(battleProposal, candidateProposal, matchQuality)) {
                    // подходящий противник
                    candidateMatched(candidatesByMatchQuality, battleProposal, candidateProposal, matchQuality);
                } else if(lobbyConf.isIgnoreMatchQualityForNoduelBattles() && (
                        battleProposal.getWager() == BattleWager.WAGER_50_2x2 || battleProposal.getWager() == BattleWager.WAGER_50_3_FOR_ALL)) {
                    // нижний порог для этих видов боя отключен в настройках
                    candidateMatched(candidatesByMatchQuality, battleProposal, candidateProposal, matchQuality);
                } else if(lobbyConf.isIgnoreMatchQualityForDuel300Battles() && battleProposal.getWager() == BattleWager.WAGER_300_DUEL) {
                    // нижний порог для этих вида боя отключен в настройках
                    candidateMatched(candidatesByMatchQuality, battleProposal, candidateProposal, matchQuality);
                } else if(battleProposal.getWager().getWagerValue() == WagerValue.WV_0) {
                    // не учитываем мастерство при подборе напарника для прохождения боссов
                    candidateMatched(candidatesByMatchQuality, battleProposal, candidateProposal, matchQuality);
                } else {
                    notMatch(battleProposal, candidateProposal, ProposalStat.MatchType.SKILL);
                    notMatchReasonByCandidate.put(candidateProposal, "SKILL");
                }
            } else {
                notMatch(battleProposal, candidateProposal, ProposalStat.MatchType.EXTRA);
            }
        }

        if(log.isDebugEnabled())
            log.debug("wager:{} for {} there are appropriate candidates:\n {})", battleProposal.getWager().getValue(), battleProposal, candidatesByMatchQuality);

        if(candidatesByMatchQuality.size() > 0)
            Collections.sort(candidatesByMatchQuality, (o1, o2) -> {
                int res = o2.getLeft().getGridQuality() - o1.getLeft().getGridQuality();
                if(res == 0) {
                    res = o2.right.compareTo(o1.right);
                }
                return res;
            });

        int resultSize = 0;
        for(ImmutablePair<BattleProposal, Double> candidate : candidatesByMatchQuality) {
            BattleProposal candidateProposal = candidate.left;
            if(candidateProposal.getSize() > quantity - resultSize)
                continue;
            if(proposalsByWager.remove(candidateProposal)) {
                //  проверяем что его ещё не выбрали кандидатом в другой бой
                if(candidateProposal.tryLock()) {
                    double matchQuality = candidate.right;
                    candidateProposal.setMatchQuality(matchQuality);
                    battleProposal.setMatchQuality(matchQuality);
                    candidates.add(candidateProposal);
                    resultSize += candidateProposal.getSize();
                    if(resultSize == quantity) {
                        break;
                    }
                }
            }
        }

        if(resultSize == quantity) {
            if(log.isDebugEnabled())
                log.debug("matchmake success for {}: {}]", battleProposal, candidates);
            return true;
        } else {
            if(log.isDebugEnabled()) {
                log.debug("matchmake failure for {}, quantity={}; not matched candidates: {}", battleProposal, quantity, formatNotMatched(notMatchReasonByCandidate));
            }
            return false;
        }
    }

    protected void candidateMatched(List<ImmutablePair<BattleProposal, Double>> candidatesByMatchQuality, BattleProposal battleProposal, BattleProposal candidateProposal, double matchQuality) {
        candidatesByMatchQuality.add(new ImmutablePair<>(candidateProposal, matchQuality));
        battleProposal.getStat().matchedCandidats++;
    }

    public boolean mayMatched(BattleProposal proposal1, BattleProposal proposal2) {
        return mayMatched(proposal1, proposal2, /*ignored*/ new VarObject<>());
    }

    public boolean mayMatched(BattleProposal proposal1, BattleProposal proposal2, VarObject<String> reason) {
        TeamBattleProposal team1 = (TeamBattleProposal) proposal1;
        TeamBattleProposal team2 = (TeamBattleProposal) proposal2;
        for(PvpUser user1 : team1.getTeam()) {
            for(PvpUser user2 : team2.getTeam()) {
                // в течении дня с одним и тем же игроком разрешено соединять не более 3-х раз (по умолчанию)
                boolean maxBattlesExceed = Math.max(dailyRegistry.getBattlesCount(user1.getId(), user2.getId()), dailyRegistry.getBattlesCount(user2.getId(), user1.getId()))
                        >= getMaxBattlesWithSameUser();
                boolean isLastOpponents = lobbyConf.isCheckLastOpponent() && (user1.isLastMatchmakingOpponent(user2.getId()) || user2.isLastMatchmakingOpponent(user1.getId()));
                boolean isExcluded = lobbyConf.isCheckFakes() && (blackListService.isInBlackList(user1.getId(), user2.getId()) || blackListService.isInBlackList(user2.getId(), user1.getId()));
                boolean isPourDowners = lobbyConf.isCheckPourDowners() && (isPourDowners(user1) || isPourDowners(user2));
                boolean isSameClan = lobbyConf.isCheckClan() && isSameClan(user1, user2);
                boolean isSameIp = !lobbyConf.isMatchBySameIp(proposal1.getWager()) && isSameIp(proposal1, proposal2);

                if(maxBattlesExceed || isLastOpponents || isExcluded || isPourDowners || isSameClan || isSameIp) {
                    reason.value = maxBattlesExceed ? "MAX_BATTLES" : (
                            isLastOpponents ? "LAST_OPPONENT" : (
                                    isExcluded ? "EXCLUDED" : (
                                            isPourDowners ? "POUR_DOWNERS" : (
                                                    isSameClan ? "SAME_CLAN" : (
                                                            isSameIp ? "SAME_IP" : (
                                                                null))))));

                    return false;
                }
            }
        }
        return team1.getTeam().length > 0 && team2.getTeam().length > 0;
    }

    protected boolean isSameClan(PvpUser user1, PvpUser user2) {
        return user1.clanId > 0 && user2.clanId > 0 && user1.clanId == user2.clanId;
    }

    protected boolean isSameIp(BattleProposal proposal1, BattleProposal proposal2) {
        return !proposal1.getClientAddress().isEmpty() && !proposal2.getClientAddress().isEmpty() && proposal1.getClientAddress().equals(proposal2.getClientAddress());
    }

    protected int getMaxBattlesWithSameUser() {
        return lobbyConf.getMaxBattlesWithSameUser();
    }

    public boolean isPourDowners(PvpUser user) {
        if(user.getMinDailyRating() > lobbyConf.getWarnNegativeRating()) {
            return false;
        } else if(user.getMinDailyRating() <= lobbyConf.getMinNegativeRating()) {
            return true;
        } else {
            int missMass = 100 - ((user.getMinDailyRating() - lobbyConf.getMinNegativeRating()) * 50 / (lobbyConf.getWarnNegativeRating() - lobbyConf.getMinNegativeRating()));
            return new Random().nextInt(100) < missMass;
        }
    }

    protected boolean matchByRating(BattleProposal battleProposal, BattleProposal candidateProposal, double matchQuality) {
        double validProposalMatchQuality = lobbyConf.getBestMatchQuality() - (lobbyConf.getDeltaMatchQuality() * battleProposal.getGridQuality());
        double validCandidateMatchQuality = lobbyConf.getBestMatchQuality() - (lobbyConf.getDeltaMatchQuality() * candidateProposal.getGridQuality());
        double proposalRating = getConservativeRating(battleProposal.getJSkillTeam());
        double candidateRating = getConservativeRating(candidateProposal.getJSkillTeam());
        // кандидат мною "достижим"
        if(matchQuality > validProposalMatchQuality) {
            // если ждем одинаково или он ждет больше - он нам подходит безусловно
            // если кандидат ждет меньше - кандидат должен быть сильнее
            return battleProposal.getGridQuality() <= candidateProposal.getGridQuality() || candidateRating > proposalRating;
        } else if(matchQuality > validCandidateMatchQuality) {
            // я достижим для кандидата - инвертируем пред. условие
            return candidateProposal.getGridQuality() <= battleProposal.getGridQuality() || proposalRating > candidateRating;
        } else {
            return false;
        }
    }

    private double getConservativeRating(ITeam jSkillTeam) {
        // считаем сренеарифметический консервативный рейтинг команды
        double conservativeRating = 0;
        for(Rating rating : jSkillTeam.values()) {
            conservativeRating += rating.getConservativeRating();
        }
        return conservativeRating / (double) jSkillTeam.values().size();
    }

    protected void addSameMassProposals(BattleProposal battleProposal, Collection<BattleProposal> proposalsByWager, List<BattleProposal> candidates, boolean eachOther, Map<BattleProposal, String> notMatchReasonByCandidate) {
        for(BattleProposal candidateProposal : proposalsByWager) {
            if(battleProposal.equals(candidateProposal)) {
                continue;
            }
            ProposalStat.MatchType notMathType = isSameMassTeams(battleProposal, candidateProposal);
            if(notMathType == null) {
                if(!eachOther) {
                    // подошел кандидат и это дуель
                    candidates.add(candidateProposal);
                } else {
                    // кандидаты должны подходить каждый друг к другу
                    for(BattleProposal matchedCandidate : candidates) {
                        notMathType = isSameMassTeams(candidateProposal, matchedCandidate);
                        if(notMathType != null) {
                            break;
                        }
                    }
                    if(notMathType == null) {
                        // кандидат подходит ко всем кандитатам отобранным ранее
                        candidates.add(candidateProposal);
                    } else {
                        notMatch(battleProposal, candidateProposal, notMathType);
                        notMatchReasonByCandidate.put(candidateProposal, "EACH_OTHER:" + notMathType);
                    }
                }
            } else {
                notMatch(battleProposal, candidateProposal, notMathType);
                notMatchReasonByCandidate.put(candidateProposal, notMathType.toString());
            }
        }
    }

    private void notMatch(BattleProposal battleProposal, BattleProposal candidateProposal, ProposalStat.MatchType notMathType) {
        if(log.isDebugEnabled()) {
            log.debug("{} not matched {} by {}", battleProposal, candidateProposal, notMathType);
        }
        battleProposal.getStat().incNotMatchCounterFor(notMathType);
        candidateProposal.getStat().incNotMatchCounterThis(notMathType);
    }

    private static String formatNotMatched(Map<BattleProposal, String> notMatchReasonByCandidate) {
        StringBuilder notMatchedCandidates = new StringBuilder();
        for(Map.Entry<BattleProposal, String> proposalAndReason : notMatchReasonByCandidate.entrySet()) {
            BattleProposal candidate = proposalAndReason.getKey();
            String reason = proposalAndReason.getValue();
            notMatchedCandidates.append("\n\t- mismatched ");
            notMatchedCandidates.append(reason);
            notMatchedCandidates.append(": ");
            notMatchedCandidates.append(candidate);
            notMatchedCandidates.append(',');
        }
        return notMatchedCandidates.toString();
    }

    /**
     * вернет профайл в очереди ожидания на бой за ставку
     *
     * @param profile профайл игрока который сделал ставку
     * @param unlock
     */
    @Override
    public boolean addBattleProposalByWager(BattleProposal profile, boolean unlock) {
        if(profile.inValidState()) {
            //берем профайлы по ставке
            Set<BattleProposal> proposalsByWager = getBattleProposals(profile);

            if(unlock) {
                profile.unlock();
            }
            proposalsByWager.add(profile);
            if(log.isDebugEnabled()) {
                log.debug(String.format("wager:%s add %s)", profile.getWager().getValue(), profile));
                log.debug("lobby: " + proposalsByWager);
            }
            return true;
        }
        return false;
    }

    abstract protected Set<BattleProposal> getBattleProposals(BattleProposal profile);

    /**
     * удалит профайл с очереди ожидания на бой за ставку
     *
     * @param battleProposal профайл игрока который сделал ставку
     * @return true если удалить удалось
     */
    @Override
    public boolean removeBattleProposalByWager(BattleProposal battleProposal) {
        battleProposal.cancel();
        //берем профайлы по ставке
        Set<BattleProposal> proposalsByWager = getBattleProposals(battleProposal);
        if(proposalsByWager != null) {
            if(log.isDebugEnabled()) {
                log.debug(String.format("wager:%s remove %s)", battleProposal.getWager().getValue(), battleProposal));
                log.debug("lobby: " + proposalsByWager);
            }
            return proposalsByWager.remove(battleProposal);
        } else {
            return false;
        }
    }

    /**
     * принадлежат ли участники одной весовой категории
     *
     * @param team          участник
     * @param candidateTeam оппонент
     * @return 0 - подошли; 1 - не подошли по уровню; 2 - не подошли по массе; 3 -  не подошли по рэйтингу
     */
    @Null
    protected ProposalStat.MatchType isSameMassTeams(BattleProposal team, BattleProposal candidateTeam) {
        //убираем проверку на уровень и жизни для ставки на 300
        if(team.getWager().getWagerValue() == WagerValue.WV_300 || team.getWager().getWagerValue() == WagerValue.WV_0)
            return null;

        // песочница
        int sandboxRatingDelimiter = lobbyConf.getSandboxRatingDelimiter(team.getWager(), team.getLevel());

        int teamNoZeroRating = Math.max(team.getRating(), 0);
        int candidateTeamNoZeroRating = Math.max(candidateTeam.getRating(), 0);

        if(sandboxRatingDelimiter > 0 && (
                (teamNoZeroRating < sandboxRatingDelimiter && candidateTeamNoZeroRating >= sandboxRatingDelimiter)
                        || (candidateTeamNoZeroRating < sandboxRatingDelimiter) && teamNoZeroRating >= sandboxRatingDelimiter)
                ) {
            return ProposalStat.MatchType.SANDBOX;
        }
        int sandboxBattlesDelimiter = lobbyConf.getSandboxBattlesDelimiter(team.getWager(), team.getLevel());
        if(sandboxBattlesDelimiter > 0 && (
                (team.getBattlesCount() < sandboxBattlesDelimiter && candidateTeam.getBattlesCount() >= sandboxBattlesDelimiter)
                        || (candidateTeam.getBattlesCount() < sandboxBattlesDelimiter) && team.getBattlesCount() >= sandboxBattlesDelimiter)
                ) {
            return ProposalStat.MatchType.SANDBOX;
        }
        int differenceLevel;
        // не допускаем большой разницы в уровнях владельцев команд
        if(lobbyConf.isUseLevelDiffFactor()) {
            // используем множитель
            int teamLevel = levelCreator.getLevelNonNull(getLevel(team)).getLevelHp();
            int candidateTeamLevel = levelCreator.getLevelNonNull(getLevel(candidateTeam)).getLevelHp();

            differenceLevel = (int) Math.round(Math.max(teamLevel, candidateTeamLevel) * lobbyConf.getLevelDiffFactor());
            if(Math.abs(teamLevel - candidateTeamLevel) > differenceLevel) {
                return ProposalStat.MatchType.LEVEL;
            }
        } else {
            // используем фиксированное значение
            if(Math.abs(getLevel(team) - getLevel(candidateTeam)) > lobbyConf.getEnemyLevelRange()) {
                return ProposalStat.MatchType.LEVEL;
            }
        }
        if(!isSameHp(team, candidateTeam)) {
            return ProposalStat.MatchType.HP;
        }
        if(!isSameGroupSize(team, candidateTeam)) {
            return ProposalStat.MatchType.TEAM_SIZE;
        }
        if(!isSameRank(team, candidateTeam)) {
            return ProposalStat.MatchType.RANK;
        }
        return null;
    }

    private int getLevel(BattleProposal proposal) {
        return Math.min(30, proposal.getLevel());
    }

    private boolean isSameRank(BattleProposal team, BattleProposal candidateTeam) {
        if(rankService == null || !rankService.isEnabled())
            return true;
        int rank1 = team.getRank();
        int rank2 = candidateTeam.getRank();
        return 2 * Math.max(rank1, rank2) - Math.min(rank1, rank2) < lobbyConf.getRankThreshold();
    }

    private boolean isSameHp(BattleProposal team, BattleProposal candidateTeam) {
        // вычисляем разброс HP в соответствии с временем ожидания заявок (GridQuality)
        int differenceTeamHp = (int) (Math.min(team.getGroupHp(), candidateTeam.getGroupHp()) * Math.min((lobbyConf.getHpDiffFactor()) + (lobbyConf.getDeltaHpDiffFactor() * team.getGridQuality()), lobbyConf.getMaxHpDiffFactor()));
        int differenceCandidateHp = (int) (Math.min(team.getGroupHp(), candidateTeam.getGroupHp()) * Math.min((lobbyConf.getHpDiffFactor()) + (lobbyConf.getDeltaHpDiffFactor() * candidateTeam.getGridQuality()), lobbyConf.getMaxHpDiffFactor()));

        // кандидат мною "достижим"
        if(Math.abs(team.getGroupHp() - candidateTeam.getGroupHp()) <= differenceTeamHp) {
            // если ждем одинаково или он ждет больше - он нам подходит безусловно
            // если кандидат ждет меньше - кандидат должен быть сильнее
            return team.getGridQuality() <= candidateTeam.getGridQuality() || candidateTeam.getGroupHp() >= team.getGroupHp();
        } else if(Math.abs(team.getGroupHp() - candidateTeam.getGroupHp()) <= differenceCandidateHp) {
            // я достижим для кандидата - инвертируем пред. условие
            return candidateTeam.getGridQuality() <= team.getGridQuality() || team.getGroupHp() >= candidateTeam.getGroupHp();
        } else {
            return false;
        }
    }

    private boolean isSameGroupSize(BattleProposal team, BattleProposal candidatTeam) {
        return team.getGroupSize() == candidatTeam.getGroupSize();
    }

    public <T extends BattleProposal> void playTurn(List<T> teams) {
        if(teams.size() < 2) {
            return;
        }

        Collections.sort(teams);

        for(int i = teams.size() - 1; i > 0; i--) {
            T proposalRatingLess = teams.get(i);
            T proposalRatingMore = teams.get(i - 1);
            if(compare(proposalRatingLess, proposalRatingMore)) {
                teams.set(i - 1, proposalRatingLess);
                teams.set(i, proposalRatingMore);
            }
        }
    }

    public boolean compare(BattleProposal battleProposal1, BattleProposal battleProposal2) {
        int[] r1 = battleProposal1.getReactionLevel();
        int[] r2 = battleProposal2.getReactionLevel();
        int reactionLevel_1 = Math.max(1, r1[0] + r2[1]);
        int reactionLevel_2 = Math.max(1, r2[0] + r1[1]);

        if(reactionLevel_1 == reactionLevel_2)
            return new Random().nextBoolean();
        else
            return new Random().nextInt(reactionLevel_1 * reactionLevel_1 + reactionLevel_2 * reactionLevel_2) < reactionLevel_1 * reactionLevel_1;
    }

    //====================== Getters and Setters =================================================================================================================================================

    public LobbyConf getLobbyConf() {
        return lobbyConf;
    }

    public void setLobbyConf(LobbyConf lobbyConf) {
        this.lobbyConf = lobbyConf;
    }

    public void setTrueSkillService(TrueSkillService trueSkillService) {
        this.trueSkillService = trueSkillService;
    }
}
