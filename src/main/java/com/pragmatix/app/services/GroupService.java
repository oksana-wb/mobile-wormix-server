package com.pragmatix.app.services;

import com.pragmatix.achieve.common.BonusItem;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.app.common.TeamMemberType;
import com.pragmatix.app.init.LevelCreator;
import com.pragmatix.app.messages.client.AddToGroup;
import com.pragmatix.app.messages.client.ReorderGroup;
import com.pragmatix.app.messages.client.ToggleTeamMember;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.model.Level;
import com.pragmatix.app.model.StuffHaving;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.group.*;
import com.pragmatix.app.settings.ItemRequirements;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.Arrays;
import java.util.Map;

import static com.pragmatix.app.common.MoneyType.REAL_MONEY;
import static com.pragmatix.app.common.ShopResultEnum.MIN_REQUIREMENTS_ERROR;
import static com.pragmatix.app.common.ShopResultEnum.NOT_ENOUGH_MONEY;
import static com.pragmatix.gameapp.common.SimpleResultEnum.ERROR;
import static com.pragmatix.gameapp.common.SimpleResultEnum.SUCCESS;
import static org.apache.commons.lang3.ArrayUtils.indexOf;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.04.2014 14:25
 */
@Service
public class GroupService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final int MAX_TEAM_MEMBERS = 7;

    public static byte MAX_EXTRA_GROUP_SLOTS = 3;

    @Resource
    private LevelCreator levelCreator;

    @Resource
    private ProfileService profileService;

    @Resource
    private DaoService daoService;

    @Resource
    @Qualifier(value = "friendGroupPriceSettings")
    private ItemRequirements friendGroupPriceSettings;

    @Resource
    @Qualifier(value = "soclanGroupPriceSettings")
    private ItemRequirements soclanGroupPriceSettings;

    @Resource
    private StatisticService statisticService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private CraftService craftService;

    @Resource
    private SkinService skinService;

    @Value("#{achieveBonusItemsMap}")
    private Map<Integer, BonusItem> achieveBonusItems;

    @Value("#{mercenariesConf}")
    private Map<Integer, MercenaryBean> mercenariesConf;

    //id бесплатного наемника
    private int freeMmerchenaryId = -1;

    @PostConstruct
    public void init() {
        for(Map.Entry<Integer, MercenaryBean> entry : mercenariesConf.entrySet()) {
            entry.getValue().id = entry.getKey();
        }
    }

    public ShopResultEnum addToGroup(final AddToGroup msg, final UserProfile profile) throws Exception {
        try {
            final int teamMemberId = msg.teamMemberId;
            if(profile.getId() == teamMemberId || msg.teamMemberType == TeamMemberType.Himself) {
                log.warn("Can't add myself in group");
                return ShopResultEnum.ERROR;
            }
            if(profile.containsInTeam(teamMemberId)) {
                log.warn("Can't add to group because user profile with id {} already exist", teamMemberId);
                return ShopResultEnum.ERROR;
            }
            final int[] wormsGroup = profile.getWormsGroup();
            final int prevTeamMemberIndex = msg.prevTeamMemberId != AddToGroup.NO_PREV_TEAM_MEMBER ? indexOf(wormsGroup, msg.prevTeamMemberId) : -1;
            // заменяем участника команды
            if(prevTeamMemberIndex > -1) {
                if(prevTeamMemberIndex >= wormsGroup.length) {
                    log.warn("Can't insert to group cause wrong index {} {}", prevTeamMemberIndex, Arrays.toString(wormsGroup));
                    return ShopResultEnum.ERROR;
                }
                for(int i = 0; i < wormsGroup.length; i++) {
                    if(profile.getId() == wormsGroup[i] && i == prevTeamMemberIndex) {
                        log.warn("Can't replace myself in group");
                        return ShopResultEnum.ERROR;
                    }
                }
            }


            ItemRequirements groupPriceSettings;
            MercenaryBean mercenaryBean;
            UserProfile teamMemberProfile = null;

            if(msg.teamMemberType == TeamMemberType.SoclanMember || msg.teamMemberType == TeamMemberType.Friend) {
                teamMemberProfile = profileService.getUserProfile(teamMemberId);
                if(teamMemberProfile == null) {
                    log.warn("Can't add to group because user profile with id {} not found", teamMemberId);
                    return ShopResultEnum.ERROR;
                }

                groupPriceSettings = new ItemRequirements();

                if(msg.teamMemberType == TeamMemberType.SoclanMember) {
                    // каждый купленный слот должен увеличивать лимит соклановцев в команде на 1 (http://jira.pragmatix-corp.com/browse/WORMIX-4302)
                    final int maxSoclanCount = 1 + profile.getExtraGroupSlotsCount();
                    // ...но брать в бой (активировать) можно только одного соклановца
                    final int maxActiveSoclanCount = 1;

                    // сколько соклановцев станет после выполнения этого добавления?
                    int soclanCount = 0;
                    int activeSoclanCount = 0;
                    TeamMember[] teamMembers = profile.getTeamMembers();
                    for(int i = 0; i < teamMembers.length; i++) {
                        TeamMember teamMember = teamMembers[i];
                        // считаем сначала уже бывших соклановцев:
                        if(teamMember instanceof SoclanTeamMember) {
                            // в i-м слоте - точно соклановец, но это...
                            soclanCount += 1;
                            if(prevTeamMemberIndex == i) {
                                // ...либо старого соклановца заменили на нового (активность могла поменяться)
                                activeSoclanCount += (msg.active ? 1 : 0);
                            } else {
                                // ...либо соклановец остался как был
                                activeSoclanCount += (teamMember.isActive() ? 1 : 0);
                            }
                        } else if(prevTeamMemberIndex == i) {
                            // в i-м слоте был не соклановец, заменили на соклановца
                            soclanCount += 1;
                            activeSoclanCount += (msg.active ? 1 : 0);
                        }
                    }
                    // + если добавили нового соклановца в конец
                    if(prevTeamMemberIndex <= -1) {
                        soclanCount += 1;
                        activeSoclanCount += (msg.active ? 1 : 0);
                    }
                    if(soclanCount > maxSoclanCount || activeSoclanCount > maxActiveSoclanCount) {
                        log.error("Нельзя добавить в команду соклановца {}: В команде станет {} соклановцев (из них {} активно), а может быть максимум {} ({} активно)", teamMemberId, soclanCount, activeSoclanCount, maxSoclanCount, maxActiveSoclanCount);
                        return ShopResultEnum.ERROR;
                    }
                    // игрок может быть загружен из базы, но пока не иметь UserProfileStructure в которой и содержится клановая информация
                    UserProfileStructure teamMemberStructure = profileService.getUserProfileStructure(teamMemberProfile);
                    int teamMemberClanId = teamMemberStructure.clanMember.getClanId();
                    if(profile.getClanId() == 0 || teamMemberClanId != profile.getClanId()) {
                        log.warn(String.format("Не в клане или не соклановец profileClanId=%s teamMemberClanId=%s", profile.getClanId(), teamMemberClanId));
                        return ShopResultEnum.ERROR;
                    }

                    groupPriceSettings.needMoney = soclanGroupPriceSettings.needMoney + teamMemberProfile.getLevel() * soclanGroupPriceSettings.needRealMoney;
                } else {
                    groupPriceSettings.needMoney = friendGroupPriceSettings.needMoney + teamMemberProfile.getLevel() * friendGroupPriceSettings.needRealMoney;
                }
                groupPriceSettings.needRealMoney = (int) Math.ceil((double) groupPriceSettings.needMoney / 100D);
            } else {
                // в команду берется наемник
                mercenaryBean = mercenariesConf.get(msg.teamMemberId);
                if(mercenaryBean == null) {
                    log.warn("Can't add mercenary to group because  mercenary with id {} not found", teamMemberId);
                    return ShopResultEnum.ERROR;
                }
                groupPriceSettings = new ItemRequirements(mercenaryBean.needMoney, mercenaryBean.needRealMoney, mercenaryBean.needLevel);
            }

            if(getCurrentMaxWormsCount(profile) < wormsGroup.length + 1 && prevTeamMemberIndex <= -1) {
                log.warn("Can not add profile to group because max count exceeded for his level {} and extra slots {}", profile.getLevel(), profile.getExtraGroupSlotsCount());
                return MIN_REQUIREMENTS_ERROR;
            }

            int decreaseMoney = 0;
            int decreaseRealMoney = 0;

            // считаем, сколько списать денег за добавление в команду
            if(msg.moneyType == REAL_MONEY) {
                decreaseRealMoney = groupPriceSettings.needRealMoney();
                // если недостаточно денег для совершения покупки
                if(profile.getRealMoney() < decreaseRealMoney) {
                    log.warn("Can't add to group: not enough real money...");
                    return NOT_ENOUGH_MONEY;
                }
            } else {
                decreaseMoney = groupPriceSettings.needMoney();
                // если недостаточно денег для совершения покупки
                if(profile.getMoney() < decreaseMoney) {
                    log.warn("Can't add to group: not enough money...");
                    return NOT_ENOUGH_MONEY;
                }
            }

            TeamMember teamMember = TeamMember.newTeamMember(msg.teamMemberType, teamMemberProfile, (_profile, race) -> skinService.getSkin(_profile, race));
            if(teamMember == null) {
                log.warn("Can't add to group: incorrect teamMemberType: {}", msg.teamMemberType);
                return ShopResultEnum.ERROR;
            }
            teamMember.setActive(msg.active);

            // проверяем, увеличилось ли количество активных членов команды:
            if(teamMember.isActive() &&
                    (prevTeamMemberIndex <= -1 // либо добавили нового активного
                            || !profile.getTeamMembers()[prevTeamMemberIndex].isActive())) { // либо заменили неактивного активным
                // и если после добавки/замены станет активно слишком много участников для данного левела, то ошибка
                Level curLevel = levelCreator.getLevel(profile.getLevel());
                if(curLevel != null) {
                    if(profile.getActiveTeamMembersCount() + 1 > curLevel.getMaxWormsCount()) {
                        log.warn("Can't add to group because max active count exceeded for level {}", profile.getLevel());
                        return MIN_REQUIREMENTS_ERROR;
                    }
                } else {
                    log.warn("Can't add to group because level number {} not found", profile.getLevel());
                    return ShopResultEnum.ERROR;
                }
            }

            if(prevTeamMemberIndex <= -1) {
                // добовляем в команду нового участника
                profile.addInTeam(teamMemberId, teamMember);
            } else {
                // заменяем в команде участника
                profile.replaceTeamMember(prevTeamMemberIndex, teamMemberId, teamMember);
            }

            final int price = decreaseMoney + decreaseRealMoney;

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    //сохроняем в БД твоего червя
                    if(profile.getWormsGroup().length == 2 && prevTeamMemberIndex <= -1) {
                        // добавили первого игрока в команду
                        daoService.getWormGroupDao().insertWormGroups(profile);
                    } else {
                        daoService.getWormGroupDao().updateWormGroups(profile);
                    }

                    //сохроняем статистику покупки червя и id добавленного червя
                    statisticService.addToGroupStatistic(profile.getId(), msg.moneyType, price, (long) teamMemberId, profile.getLevel());

                }
            });

            profile.setRealMoney(profile.getRealMoney() - decreaseRealMoney);
            profile.setMoney(profile.getMoney() - decreaseMoney);

            //перестраиваем кешь
            profile.getUserProfileStructure().wormsGroup = profileService.createWormGroupStructures(profile);

            return ShopResultEnum.SUCCESS;
        } catch (Exception ex) {
            log.error("AddToGroup ERROR: " + ex.toString(), ex);
            return ShopResultEnum.ERROR;
        }
    }

    public SimpleResultEnum removeFromGroup(final UserProfile profile, final int teamMemberId) {
        try {
            if(profile.getId() == teamMemberId) {
                log.warn("Can't decrement myself from group");
                return SimpleResultEnum.ERROR;
            }
            if(!profile.containsInTeam(teamMemberId)) {
                log.warn("Can't decrement from group because user profile with id {} not found in group", teamMemberId);
                return SimpleResultEnum.ERROR;
            }
            //удаляем члена команды
            profile.removeFromTeam(teamMemberId);

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    //удаляем червя из группы в БД
                    if(profile.getWormsGroup().length == 1) {
                        // в команде не осталось игроков
                        daoService.getWormGroupDao().deleteWormGroups(profile.getId());
                    } else {
                        daoService.getWormGroupDao().updateWormGroups(profile);
                    }
                    //сохраняем статистику об удалении червя из команды
                    statisticService.removeFromGroupStatistic(profile.getId(), (long) teamMemberId, profile.getLevel());
                }
            });

            //перестраиваем кешь
            profile.getUserProfileStructure().wormsGroup = profileService.createWormGroupStructures(profile);

            return SimpleResultEnum.SUCCESS;
        } catch (Exception ex) {
            log.error("RemoveFromGroup ERROR: " + ex.toString(), ex);
            return SimpleResultEnum.ERROR;
        }
    }

    public SimpleResultEnum reorderGroup(ReorderGroup msg, final UserProfile profile) throws Exception {
        try {
            if(!contentEquals(profile.getWormsGroup(), msg.reorderedWormGroup)) {
                log.error(String.format("команда ReorderGroupResult не валидна: присланный массив id игроков команды не соответствует команде игрока! %s wormGroup:%s"
                        , msg, Arrays.toString(profile.getWormsGroup())));
                return ERROR;
            }

            // устанавливаем новый порядок очередности ходов членов команды
            int[] wormsGroup = new int[msg.reorderedWormGroup.length];
            TeamMember[] teamMembers = new TeamMember[msg.reorderedWormGroup.length];
            int[] reorderedWormGroup = msg.reorderedWormGroup;
            for(int i = 0; i < reorderedWormGroup.length; i++) {
                wormsGroup[i] = reorderedWormGroup[i];
                int oldTeamMemberIndex = indexOf(profile.getWormsGroup(), wormsGroup[i]);
                teamMembers[i] = profile.getTeamMembers()[oldTeamMemberIndex];
            }
            profile.setWormsGroup(wormsGroup);
            profile.setTeamMembers(teamMembers);

            profile.setTeamMembersDirty(true);

            //перестраиваем кешь
            profile.getUserProfileStructure().wormsGroup = profileService.createWormGroupStructures(profile);

            return SUCCESS;
        } catch (Exception ex) {
            log.error("ReorderGroup ERROR: " + ex.toString(), ex);
            return ERROR;
        }
    }

    private boolean contentEquals(int[] wormsGroup, int[] reorderedWormGroup) {
        if(wormsGroup.length != reorderedWormGroup.length) {
            return false;
        }
        for(int teamMemberId : wormsGroup) {
            boolean found = false;
            for(long profileId : reorderedWormGroup) {
                if(teamMemberId == profileId) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                return false;
            }
        }
        return true;
    }

    @Null
    public StuffHaving getDefaultTeamMemberStuff(int teamMemberId, TeamMember teamMember) {
        if(teamMember instanceof FriendTeamMember) {
            return null;
        } else if(teamMember instanceof MercenaryTeamMember) {
            MercenaryBean mercenaryBean = mercenariesConf.get(teamMemberId);
            if(mercenaryBean != null) {
                return mercenaryBean;
            } else {
                log.error(String.format("Mercenary not found by id [%s]", teamMemberId));
            }
        }
        return null;
    }

    public short getDefaultTeamMemberHat(int teamMemberId, TeamMember teamMember) {
        StuffHaving stuffHaving = getDefaultTeamMemberStuff(teamMemberId, teamMember);
        return stuffHaving != null ? stuffHaving.getHatId() : 0;
    }

    public short getDefaultTeamMemberKit(int teamMemberId, TeamMember teamMember) {
        StuffHaving stuffHaving = getDefaultTeamMemberStuff(teamMemberId, teamMember);
        return stuffHaving != null ? stuffHaving.getKitId() : 0;
    }


    public SimpleResultEnum toggleTeamMember(ToggleTeamMember msg, final UserProfile profile) {
        final int teamMemberId = msg.teamMemberId;
        try {
            if(profile.getId() == teamMemberId) {
                log.warn("Can't toggle myself in group");
                return SimpleResultEnum.ERROR;
            }
            TeamMember teamMember = profile.getFriendTeamMemberNullable(teamMemberId);
            if(teamMember == null) {
                log.warn("Can't toggle member because team member [{}] not found in group", teamMemberId);
                return SimpleResultEnum.ERROR;
            }
            if(teamMember.isActive() == msg.active) {
                log.warn("Team member [{}] alredy {}", teamMemberId, msg.active ? "active" : "disabled");
                return SimpleResultEnum.ERROR;
            }

            byte alreadyActive = profile.getActiveTeamMembersCount();
            Level curLevel = levelCreator.getLevel(profile.getLevel());
            if(curLevel == null) {
                log.warn("Can't toggle member because level number {} not found", profile.getLevel());
                return SimpleResultEnum.ERROR;
            } else if(msg.active && alreadyActive + 1 > curLevel.getMaxWormsCount()) {
                log.warn("Can't activate another member because [{}] members are already active: maximum for level [{}]", alreadyActive, curLevel);
                return SimpleResultEnum.ERROR;
            }
            if(teamMember instanceof SoclanTeamMember && msg.active) {
                // проверяем: брать в бой (активировать) по-прежнему можно только одного соклановца (http://jira.pragmatix-corp.com/browse/WORMIX-4302)
                for(TeamMember otherTeamMember : profile.getTeamMembers()) {
                    if(otherTeamMember != teamMember && otherTeamMember instanceof SoclanTeamMember && otherTeamMember.isActive()) {
                        log.error("В команде уже есть активный соклановец");
                        return SimpleResultEnum.ERROR;
                    }
                }
            }

            teamMember.setActive(msg.active);

            profile.setTeamMembersDirty(true);

            //перестраиваем кешь
            profile.getUserProfileStructure().wormsGroup = profileService.createWormGroupStructures(profile);

            return SimpleResultEnum.SUCCESS;
        } catch (Exception ex) {
            log.error("ToggleTeamMember ERROR: " + ex.toString(), ex);
            return SimpleResultEnum.ERROR;
        }
    }

    public void tryAddFreeMerchenary(final UserProfile profile) {
        tryAddFreeMerchenary(profile, false);
    }

    public void increaseAttackAndArmorToFriendsIfNeed(final UserProfile profile) {
        TeamMember[] teamMembers = profile.getTeamMembers();
        for(int i = 0; i < teamMembers.length; i++) {
            TeamMember teamMember = teamMembers[i];
            if (teamMember instanceof FriendTeamMember) {
                FriendTeamMember friendTeamMember = (FriendTeamMember) teamMember;
                int level = (friendTeamMember.attack + friendTeamMember.armor) / 2;
                int maxAvailablePoints = levelCreator.getMaxAvailablePoints(level);
                if (friendTeamMember.armor + friendTeamMember.attack < maxAvailablePoints) {
                    byte friendTeamMemberAttack = (byte) Math.round(friendTeamMember.attack * maxAvailablePoints / (2 * level));
                    byte friendTeamMemberArmor = (byte) (maxAvailablePoints - friendTeamMemberAttack);
                    teamMembers[i] = new FriendTeamMember(friendTeamMember, friendTeamMemberAttack, friendTeamMemberArmor);

                    profile.setTeamMembersDirty(true);
                }
            }
        }
        //перестраиваем кешь
        UserProfileStructure userProfileStructure = profile.getUserProfileStructure();
        if(userProfileStructure != null && profile.isTeamMembersDirty()) {
            userProfileStructure.wormsGroup = profileService.createWormGroupStructures(profile);
        }
    }

    public void tryAddFreeMerchenary(final UserProfile profile, boolean createWormGroupStructures) {
        //Выдача бесплатного бота в команду
        if(profile.getLevel() >= 5 && profile.getWormsGroup().length == 1) {

            addFreeMerchenary(profile);

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    daoService.getWormGroupDao().insertWormGroups(profile);
                }
            });

            //перестраиваем кешь
            UserProfileStructure userProfileStructure = profile.getUserProfileStructure();
            if(userProfileStructure != null && createWormGroupStructures)
                userProfileStructure.wormsGroup = profileService.createWormGroupStructures(profile);
        }
    }

    public void addFreeMerchenary(UserProfile profile) {
        TeamMember teamMember = TeamMember.newTeamMember(TeamMemberType.Merchenary, null, null);
        profile.addInTeam(freeMmerchenaryId, teamMember);
        log.info("Added free mercenary [{}]:{} for [{}]", freeMmerchenaryId, teamMember, profile);
    }

    /**
     * Обрезает легендарные предметы и результаты крафата на соклановце до обычных (для справедливости)
     * <p>
     * Например, супер акваланг станет аквалангом (ачивочный предмет), а эпичный самурайский шлем - добротным (результат крафта)
     *
     * @param soclanWormStructure - червь-соклановец в моей команде
     */
    public void trimSoclanStuff(WormStructure soclanWormStructure) {
        if(craftService.isHatCrafted(soclanWormStructure.hat)) {
            soclanWormStructure.hat = craftService.truncateCraft(soclanWormStructure.hat);
        } else {
            soclanWormStructure.hat = trancateBonusItem(soclanWormStructure.hat);
        }
        if(craftService.isKitCrafted(soclanWormStructure.kit)) {
            soclanWormStructure.kit = craftService.truncateCraft(soclanWormStructure.kit);
        } else {
            soclanWormStructure.kit = trancateBonusItem(soclanWormStructure.kit);
        }
    }

    private short trancateBonusItem(short itemId) {
        short originItemId = itemId;
        BonusItem bonusItem = achieveBonusItems.get((int) itemId);
        while (bonusItem != null && bonusItem.replaces > 0) {
            originItemId = (short) bonusItem.replaces;
            bonusItem = achieveBonusItems.get(bonusItem.replaces);
        }
        return originItemId;
    }

//====================== Getters and Setters =================================================================================================================================================

    public Map<Integer, MercenaryBean> getMercenariesConf() {
        return mercenariesConf;
    }

    public void setMercenariesConf(Map<Integer, MercenaryBean> mercenariesConf) {
        this.mercenariesConf = mercenariesConf;
    }

    /**
     * @return Текущий максимальный размер worm group (включая неактивных), с учётом купленных доп.слотов
     */
    public int getCurrentMaxWormsCount(UserProfile profile) {
        Level curLevel = levelCreator.getLevel(profile.getLevel());
        if(curLevel == null) {
            log.error("Can't add to group because level number {} not found", profile.getLevel());
            return 0;
        }
        return curLevel.getMaxWormsCount().byteValue() + profile.getExtraGroupSlotsCount();
    }
}
