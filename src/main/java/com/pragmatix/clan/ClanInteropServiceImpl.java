package com.pragmatix.clan;

import com.pragmatix.app.common.*;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BanService;
import com.pragmatix.app.services.ProfileBonusService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.ShopService;
import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.app.settings.IItemRequirements;
import com.pragmatix.app.settings.ItemRequirements;
import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.request.LoginBase;
import com.pragmatix.clanserver.messages.response.CommonResponse;
import com.pragmatix.clanserver.messages.response.InviteFriendToClan;
import com.pragmatix.clan.structures.ClanMemberStructure;
import com.pragmatix.clanserver.messages.response.InviteToClanResponse;
import com.pragmatix.clanserver.messages.structures.InviteStructure;
import com.pragmatix.clanserver.domain.Clan;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.domain.Price;
import com.pragmatix.common.utils.VarInt;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.04.13 17:04
 */
@Service
public class ClanInteropServiceImpl {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ProfileService profileService;

    @Resource
    private BanService banService;

    @Resource
    private ShopService shopService;

    @Resource
    private RatingService ratingService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Value("${clan.memberMinLevel:4}")
    private int clanMemberMinLevel = 4;

    @Value("${clan.creatorMinLevel:7}")
    private int clanCreatorMinLevel = 7;

    @Value("${clan.creatorMinRating:200}")
    private int clanCreatorMinRating = 200;

    public boolean reserveFunds(int socialId, int profileId, Price price, VarInt reservation) {
        UserProfile userProfile = profileService.getUserProfile((long) profileId, false);
        if(userProfile != null) {
            MoneyType moneyType = MoneyType.valueOf(price.currency);
            if(moneyType == MoneyType.REAL_MONEY) {
                reservation.value = userProfile.getRealMoney();
                return userProfile.getRealMoney() >= price.amount;
            } else {
                reservation.value = userProfile.getMoney();
                return userProfile.getMoney() >= price.amount;
            }
        }
        return false;
    }

    public boolean withdrawFunds(int socialId, int profileId, Price price, int reservation, int clanId) {
        UserProfile userProfile = profileService.getUserProfile((long) profileId, false);
        if(userProfile != null) {
            ItemType itemType = extractItemType(price);
            IItemRequirements itemRequirements = extractItemRequirements(price);

            ShopResultEnum shopResult = shopService.tryBuyItem(userProfile, itemRequirements, itemType, (byte) price.currency, 1, clanId);
            return shopResult.isSuccess();
        }
        return false;
    }

    private IItemRequirements extractItemRequirements(Price price) {
        ItemRequirements itemRequirements = new ItemRequirements();
        // проверка на минимальный уровень выполняется ранее в beforeCreateClan
        itemRequirements.setNeedLevel(1);

        MoneyType moneyType = MoneyType.valueOf(price.currency);
        if(moneyType == MoneyType.REAL_MONEY) {
            itemRequirements.setNeedRealMoney(price.amount);
        } else {
            itemRequirements.setNeedMoney(price.amount);
        }
        return itemRequirements;
    }

    private ItemType extractItemType(Price price) {
        switch (price.product) {
            case CREATE_CLAN:
                return ItemType.CREATE_CLAN;
            case EXPAND_CLAN:
                return ItemType.EXPAND_CLAN;
            case CHANGE_CLAN_EMBLEM:
                return ItemType.CHANGE_CLAN_EMBLEM;
            case CHANGE_CLAN_DESCRIPTION:
                return ItemType.CHANGE_CLAN_DESCRIPTION;
            case RENAME_CLAN:
                return ItemType.RENAME_CLAN;
            case DONATION:
                return ItemType.DONATION;
        }
        log.error("кланы: не зарегистрированный тип покупки {}", price.product);
        return ItemType.NONE;
    }

    public int sendInvite(short socialId, int profileId, ClanMember clanMember, Clan clan, InviteToClanResponse response) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("приглашение в клан [%s]: clanMember=%s, profileId=%s", clan.id, clanMember.profileId, profileId));
        }
        if(banService.isBanned((long) profileId)) {
            String msg = String.format("приглашение в клан [%s]: игрок забанен [%s]", clan.id, profileId);
            response.logMessage = msg;
            log.warn(msg);
            return 1;
        }
        UserProfile userProfile = profileService.getUserProfile((long) profileId);
        if(userProfile != null) {
            Session session = Sessions.get(userProfile);
            if(session != null) {
                InviteStructure inv = new InviteStructure();
                inv.clanId = clan.id;
                inv.clanName = clan.name;
                inv.clanEmblem = clan.emblem;
                inv.profileId = clanMember.profileId;
                inv.stringProfileId = clanMember.socialProfileId;
                inv.rank = clanMember.rank;
                inv.name = clanMember.name;
                inv.inviteDate = new Date();

                InviteFriendToClan msg = new InviteFriendToClan(inv);

                Messages.toUser(msg, userProfile, Connection.MAIN);
            } else if(log.isDebugEnabled()) {
                String msg = String.format("приглашение в клан [%s]: игрок оффлайн [%s]", clan.id, profileId);
                response.logMessage = msg;
                log.debug(msg);
            }
            return 0;
        } else {
            String msg = String.format("приглашение в клан [%s]: профиль не найден [%s]", clan.id, profileId);
            response.logMessage = msg;
            log.warn(msg);
            return 1;
        }
    }

    public int refreshClan(ClanMember member) {
        UserProfile profile = profileService.getUserProfile((long) member.profileId, false);
        if(profile != null && profile.getUserProfileStructure() != null) {
            ((ClanMemberStructure) profile.getUserProfileStructure().clanMember).init(member);
            //перестраиваем кешь команды т.к. в команде мог быть соклановец который теперь должен быть заблокирован (или наоборот)
            profile.getUserProfileStructure().wormsGroup = profileService.createWormGroupStructures(profile);
        }
        return 0;
    }

    public int deleteClan(Clan clan, ClanMember member) {
        refreshClan(member);
        UserProfile profile = profileService.getUserProfile((long) member.profileId, false);
        int comeback = Math.min(member.donationCurrSeasonComeback + member.donationPrevSeasonComeback, clan.treas);
        awardProfile(clan, profile, AwardTypeEnum.DELETE_CLAN, comeback);
        return comeback;
    }

    public int expelFromClan(Clan clan, ClanMember member, int compensationInRuby) {
        refreshClan(member);
        UserProfile profile = profileService.getUserProfile((long) member.profileId);
        if(profile != null) {
            awardProfile(clan, profile, AwardTypeEnum.EXPELLED_FROM_CLAN, compensationInRuby);
            profileService.updateSync(profile);
        }
        return 0;
    }

    public int cashMedals(Clan clan, ClanMember member, int gainInRuby) {
        UserProfile profile = profileService.getUserProfile((long) member.profileId, false);
        awardProfile(clan, profile, AwardTypeEnum.CASH_MEDALS, gainInRuby);
        return 0;
    }

    public void awardProfile(Clan clan, UserProfile profile, AwardTypeEnum awardType, int realMoney) {
        if(profile != null && realMoney > 0) {
            GenericAward award = new GenericAward();
            award.setRealMoney(realMoney);
            profileBonusService.awardProfile(award, profile, awardType, String.format("clanId=%s '%s'", clan.id, clan.name));
        }
    }

    public int beforeInviteToClan(ClanMember host, short candidateSocialId, int candidateProfileId, InviteToClanResponse response) {
        int result = 1;//ошибка
        UserProfile candidateProfile = profileService.getUserProfile((long) candidateProfileId);
        if(candidateProfile != null) {
            result = candidateProfile.getLevel() >= clanMemberMinLevel ? 0 : 1;
            if(result == 1){
                String msg = String.format("для приглашения в клан кандидату не хватает уровня %s < %s", candidateProfile.getLevel(), clanMemberMinLevel);
                response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_LEVEL;
                response.logMessage = msg;
                log.warn(msg);
            }
        }
        return result;
    }

    public int beforeCreateClan(ClanMember creator, CommonResponse<LoginBase> response) {
        int result = 1;//ошибка
        UserProfile creatorProfile = profileService.getUserProfile((long) creator.profileId);
        if(creatorProfile != null) {
            result = creatorProfile.getLevel() >= clanCreatorMinLevel && creatorProfile.getRating() >= clanCreatorMinRating ? 0 : 1;
            if(result != 0){
                if(creatorProfile.getLevel() < clanCreatorMinLevel){
                    String msg = String.format("для создания клана не хватает уровня %s < %s", creatorProfile.getLevel(), clanCreatorMinLevel);
                    response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_LEVEL;
                    response.logMessage = msg;
                    log.warn(msg);
                }
                if(creatorProfile.getRating() < clanCreatorMinRating){
                    String msg = String.format("для создания клана не хватает рейтинга %s < %s", creatorProfile.getLevel(), clanCreatorMinRating);
                    response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_RATING;
                    response.logMessage = msg;
                    log.warn(msg);
                }
            }
        } else {
            log.warn("не найден профиль [{}]", creator.profileId);
        }
        return result;
    }

    public int beforeJoinClan(ClanMember candidate, CommonResponse<LoginBase> response) {
        int result = 1;//ошибка
        UserProfile candidateProfile = profileService.getUserProfile((long) candidate.profileId);
        if(candidateProfile != null) {
            result = candidateProfile.getLevel() >= clanMemberMinLevel ? 0 : 1;
            if(result == 1){
                String msg = String.format("для присоединения к клану не хватает уровня %s < %s", candidateProfile.getLevel(), clanMemberMinLevel);
                response.serviceResult = ServiceResult.ERR_NOT_ENOUGH_LEVEL;
                response.logMessage = msg;
                log.warn(msg);
            }
        }
        return result;
    }

    public void onCloseSeason() {
        ratingService.onCloseClanSeason();
    }

//====================== Getters and Setters =================================================================================================================================================


    public int getClanMemberMinLevel() {
        return clanMemberMinLevel;
    }

    public void setClanMemberMinLevel(int clanMemberMinLevel) {
        this.clanMemberMinLevel = clanMemberMinLevel;
    }

    public int getClanCreatorMinLevel() {
        return clanCreatorMinLevel;
    }

    public void setClanCreatorMinLevel(int clanCreatorMinLevel) {
        this.clanCreatorMinLevel = clanCreatorMinLevel;
    }

    public int getClanCreatorMinRating() {
        return clanCreatorMinRating;
    }

    public void setClanCreatorMinRating(int clanCreatorMinRating) {
        this.clanCreatorMinRating = clanCreatorMinRating;
    }
}
