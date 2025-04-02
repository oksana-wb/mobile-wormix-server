package com.pragmatix.app.model;

import com.pragmatix.achieve.domain.RemoteServerRequestMeta;
import com.pragmatix.app.common.BackpackUtils;
import com.pragmatix.app.common.BattleState;
import com.pragmatix.app.common.Locale;
import com.pragmatix.app.common.Race;
import com.pragmatix.app.domain.*;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.messages.structures.MissionLogStructure;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.model.group.SoclanTeamMember;
import com.pragmatix.app.model.group.TeamMember;
import com.pragmatix.app.services.BattleService;
import com.pragmatix.app.services.RaceService;
import com.pragmatix.arena.coliseum.ColiseumEntity;
import com.pragmatix.arena.mercenaries.MercenariesEntity;
import com.pragmatix.clanserver.domain.Rank;
import com.pragmatix.craft.domain.ReagentsEntity;
import com.pragmatix.notify.NotifyEvent;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.quest.dao.QuestEntity;
import com.pragmatix.sessions.IUser;
import io.vavr.Tuple2;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Null;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Профайл пользователя
 * <p/>
 * User: denis
 * Date: 07.11.2009
 * Time: 16:18:07
 */
public class UserProfile implements IUser, Comparable {

    private static final Logger log = LoggerFactory.getLogger(UserProfile.class);

    public static final String FLASH_VERSION = "FLASH_VERSION";
    public static final String INTERCOM_SECURE_TOKEN = "INTERCOM_SECURE_TOKEN";
    /**
     * идентификатор пользователя в соц. сети
     */
    public final int id;
    /**
     * Строковый ID
     */
    public String profileStringId;
    /**
     * произвольное имя
     */
    private String name;
    /**
     * кол-во доступных переименований
     */
    private byte renameAct;
    /**
     * кол-во доступных переименований за Vip30
     */
    private byte renameVipAct;
    /**
     * Id социальной сети
     */
    private byte socialId;
    /**
     * Количество игровых денег
     */
    private int money;
    /**
     * Количество реалов
     */
    private int realMoney;
    /**
     * Рейтинг игрока
     */
    private int rating;
    /**
     * броня червя
     */
    private byte armor;

    /**
     * атака червя, увеличивает силу удара
     */
    private int attack;
    /**
     * количество доступных боёв
     */
    private volatile int battlesCount;
    /**
     * уровень червя
     */
    private byte level;
    /**
     * опыт червя
     */
    private int experience;
    /**
     * id шляпы которая на голове
     */
    private short hat;
    /**
     * раса
     */
    private byte race;
    /**
     * набор имеющихся рас
     */
    private short races;
    /**
     * времы смены расы (в секундах)
     */
    private int selectRaceTime;
    /**
     * купленные скины. Старший бит указывает что скин является активныы для своеё расы
     */
    private byte[] skins = ArrayUtils.EMPTY_BYTE_ARRAY;
    /**
     * снаряжение
     */
    private short kit;
    /**
     * массив шапок, амулетов и тд
     */
    private short[] stuff;
    /**
     * массив предметов имеющих срок действия
     */
    private byte[] temporalStuff;
    /**
     * время последнего боя (в секундах)
     */
    private int lastBattleTime;
    /**
     * время последнего входа в игру
     */
    private Date lastLoginTime;
    /**
     * время выхода из игры (в секундах)
     */
    private int logoutTime;
    /**
     * время когда последний раз обыскивали домик (в секундах)
     * если 0, то никогда
     */
    public int lastSearchTime;
    /**
     * время когда данный игрок последний раз обыскивал домик друга (в секугдах)
     */
    private int lastSearchFriendTime;
    /**
     * время когда данный игрок последний раз делал платеж (в секундах)
     */
    private int lastPaymentTime;
    /**
     * время начала любого боя
     */
    private long startBattleTime;
    /**
     * команда игрока
     */
    private int[] wormsGroup;
    /**
     * Количество _купленных_ дополнительных слотов для TeamMember'ов (0...3)
     */
    private byte extraGroupSlotsCount;

    private TeamMember[] teamMembers;

    private boolean teamMembersDirty;
    /**
     * структура профайла для отправки на клиент
     */
    private UserProfileStructure userProfileStructure;
    /**
     * массив доступного оружия (WeaponRecord)
     * id - weaponId идентификатор оружия
     * value - само оружие
     */
    private List<BackpackItem> backpack;
    /**
     * неполный но упорядоченный по убыванию уровней список друзей
     */
    private int[] orderedFriends = ArrayUtils.EMPTY_INT_ARRAY;
    /**
     * находится ли игрок в бою в данный момент времени
     */
    private final AtomicInteger battleState = new AtomicInteger(BattleState.NOT_IN_BATTLE.getType());
    /**
     * находиться ли игрок в онлайне или нет
     */
    private volatile boolean online = false;
    /**
     * последний id боя окончание которого учтено
     */
    private long lastProcessedBattleId;
    /**
     * уникальный id боя
     */
    private long battleId;
    /**
     * тип боя
     */
    private short missionId;
    /**
     * тип PVP боя
     */
    public volatile PvpBattleType pvpBattleType;
    public int pvpChangeStateTime;
    /**
     * список ходов в текущей миссии с боссом
     */
    @Null // - если не находится в мисси с боссом
    private MissionLogStructure missionLog;
    /**
     * количество боёв подряд которые игрок
     * скорее всего считерил
     */
    private byte cheatInstantWin;
    /**
     * обыскал игрока которого в базе нет
     */
    private byte cheatHouseSearch;
    /**
     * кол-во быстрых обысков друзей
     */
    private byte cheatInstantHouseSearch;
    /**
     * количество непрерывных заходов (каждый день), максимум ограничен 10-тю
     */
    private byte loginSequence;
    /**
     * скорость реакции
     */
    private int reactionRate;
    /**
     * Набор ID наград выданных за взятие ачивок
     */
    private int[] grantedAchieveAwards;
    /**
     * id последней пройденной миссии
     */
    private short currentMission;
    /**
     * id последней пройденной новой миссии
     */
    private short currentNewMission;
    /**
     * набор примененных рецептов игрока
     */
    private short[] recipes;
    /**
     * реагенты для сбора рецептов
     */
    private volatile ReagentsEntity reagents = null;

    public byte[] reagentsForBattle;

    public List<GenericAwardStructure> defeatBattleAward;
    /**
     * кол-во игроков вернувшихся в игру, с последнего логина
     */
    private volatile short comebackedFriends;
    /**
     * флаг говорит о том, что поля данного класса менялись
     */
    private volatile boolean dirty;
    /**
     * флаг говорит того что игрок вернулся в игру после долгого перерыва и не определился кого из друзей за это благодарить
     * будет установлен если ранее были приглашения вернуться от друзеё и будет сброшен когда один из друзей получит за это награду
     */
    private boolean needRewardCallbackers;

    private long lastProcessedPvpBattleId;
    /**
     * защита от флуда со стороны клиента
     */
    private int lastCallbackedFriendId = 0;
    /**
     * при логине проверяем нужно ли выдать приз за призовое место клана
     * после проверки выставляется в id закрытого сезона
     */
    private byte closedClanSeasonId = 0;
    /**
     * при логине проверяем нужно ли выдать приз за сезон
     * после проверки выставляется месяц закрытого сезона
     */
    private byte closedSeasonMonth = 0;
    /**
     * мастерство игрока
     */
    private TrueSkillEntity trueSkillEntity = null;
    /**
     * сообщение которое будет отослано, при восстановлении 5-го боя
     */
    public NotifyEvent missionRestoredNotifyEvent;
    /**
     * сообщение которое будет отослано, при восстановлении 1-го боя
     */
    public NotifyEvent missionRestoredNotifyEventFirst;
    /**
     * рюкзак игрока
     */
    private volatile BackpackConfEntity backpackConfs = null;

    // месяц и день, когда был запрошен и выдан ежедневный бонус
    private short pickUpDailyBonus;

    private com.pragmatix.app.common.Locale locale = Locale.NONE;

    public short specialDealItemId;

    public byte specialDealRubyPrice;

    private volatile ColiseumEntity coliseumEntity = null;

    private volatile MercenariesEntity mercenariesEntity = null;

    private volatile QuestEntity questEntity = null;

    private volatile CookiesEntity cookiesEntity;

    public int version;

    // лучший ранг
    private byte bestRank;

    private int rankPoints;

    // время когда истекает действие VIP аккаутна (в секундах)
    private int vipExpiryTime;

    private int vipSubscriptionId;

    private volatile DepositEntity[] deposits = null;

    public final AtomicLong remoteServerRequestNum = new AtomicLong(0);

    public final List<RemoteServerRequestMeta> remoteServerRequestQueue = new CopyOnWriteArrayList<>();

    private String countryCode;
    private String currencyCode;

    private Date levelUpTime;

    private short releaseAward;

    public UserProfile(Long id) {
        this.id = id.intValue();
    }

    public UserProfile(UserProfileEntity userProfileEntity, WormGroupsEntity wormGroupEntities, Tuple2<Integer, Byte> rankValues) {
        this.id = userProfileEntity.getId().intValue();
        init(userProfileEntity, wormGroupEntities, rankValues);

        this.dirty = false;
    }

    public void init(UserProfileEntity userProfileEntity, WormGroupsEntity wormGroupEntities, Tuple2<Integer, Byte> rankValues) {
        this.name = userProfileEntity.getName() != null ? userProfileEntity.getName() : "";
        this.money = Math.max(userProfileEntity.getMoney(), 0);
        this.realMoney = Math.max(userProfileEntity.getRealmoney(), 0);
        this.rating = userProfileEntity.getRating();
        this.armor = (byte) userProfileEntity.getArmor();
        this.attack = (byte) userProfileEntity.getAttack();
        this.battlesCount = userProfileEntity.getBattlesCount();
        this.level = (byte) userProfileEntity.getLevel();
        this.experience = userProfileEntity.getExperience();
        // ленивое разделение mixedHat на hatId и raceId
        if(userProfileEntity.getRace() == null) {
            this.race = (byte) RaceService.getRaceId(userProfileEntity.getHat());
            this.hat = RaceService.getHatId(userProfileEntity.getHat());
        } else {
            this.race = userProfileEntity.getRace().byteValue();
            this.hat = userProfileEntity.getHat();
        }
        this.skins = userProfileEntity.getSkins() != null ? userProfileEntity.getSkins() : ArrayUtils.EMPTY_BYTE_ARRAY;
        this.kit = userProfileEntity.getKit() != null ? userProfileEntity.getKit() : 0;
        this.stuff = userProfileEntity.getStuff() != null ? userProfileEntity.getStuff() : new short[0];
        this.temporalStuff = userProfileEntity.getTemporalStuff() != null ? userProfileEntity.getTemporalStuff() : new byte[0];
        setLastBattleTime(userProfileEntity.getLastBattleTime().getTime());
        this.lastLoginTime = userProfileEntity.getLastLoginTime();
        setLastSearchTime(userProfileEntity.getLastSearchTime());
        this.loginSequence = userProfileEntity.getLoginSequence() != null ? userProfileEntity.getLoginSequence() : 0;
        this.reactionRate = userProfileEntity.getReactionRate() != null ? userProfileEntity.getReactionRate() : 0;
        this.currentMission = userProfileEntity.getCurrentMission() != null ? userProfileEntity.getCurrentMission() : 0;
        this.currentNewMission = userProfileEntity.getCurrentNewMission() != null ? userProfileEntity.getCurrentNewMission() : 0;
        this.pickUpDailyBonus = userProfileEntity.getPickUpDailyBonus() != null ? userProfileEntity.getPickUpDailyBonus() : 0;
        setLastSearchFriendTime(0L);
        this.cheatHouseSearch = 0;
        this.cheatInstantWin = 0;
        this.cheatInstantHouseSearch = 0;
        this.locale = userProfileEntity.getLocale() != null ? Locale.valueOf(userProfileEntity.getLocale()) : Locale.RU;
        this.races = userProfileEntity.getRaces() == null ?
                Race.setRace(Race.valueOf(this.race))
                : userProfileEntity.getRaces();
        this.selectRaceTime = userProfileEntity.getSelectRaceTime() != null ? userProfileEntity.getSelectRaceTime() : 0;
        this.renameAct = userProfileEntity.getRenameAct() != null ? userProfileEntity.getRenameAct() : 2;
        this.renameVipAct = userProfileEntity.getRenameVipAct() != null ? userProfileEntity.getRenameVipAct() : 0;
        this.logoutTime = userProfileEntity.getLogoutTime() != null ? (int) (userProfileEntity.getLogoutTime().getTime() / 1000L) : 0;

        // оставляем только примененные рецепты
        if(userProfileEntity.getRecipes() != null && userProfileEntity.getRecipes().length > 0) {
            boolean hasNotApplied = false;
            for(short recipeId : userProfileEntity.getRecipes()) {
                if(recipeId < 0) {
                    hasNotApplied = true;
                    break;
                }
            }
            if(hasNotApplied) {
                this.recipes = new short[0];
                for(short recipeId : userProfileEntity.getRecipes()) {
                    if(recipeId > 0) {
                        this.recipes = ArrayUtils.add(this.recipes, recipeId);
                    }
                }
                // обновим профиль в базе, чтобы удалить не использованные рецепты
                this.dirty = true;
            } else {
                this.recipes = userProfileEntity.getRecipes();
            }
        } else {
            this.recipes = new short[0];
        }

        this.comebackedFriends = userProfileEntity.getComebackedFriends() != null ? userProfileEntity.getComebackedFriends() : 0;
        this.vipExpiryTime = userProfileEntity.getVipExpiryTime() != null ? (int) (userProfileEntity.getVipExpiryTime().getTime() / 1000L) : 0;
        this.renameVipAct = userProfileEntity.getRenameVipAct() != null && isVipActive() ? userProfileEntity.getRenameVipAct() : 0;
        this.lastPaymentTime = userProfileEntity.getLastPaymentDate() != null ? (int) (userProfileEntity.getLastPaymentDate().getTime() / 1000L) : 0;
        this.vipSubscriptionId = userProfileEntity.getVipSubscriptionId() != null ? userProfileEntity.getVipSubscriptionId() : 0;

        initWormGroup(wormGroupEntities);

        this.rankPoints = rankValues._1;
        this.bestRank = rankValues._2;

        this.countryCode =  userProfileEntity.getCountryCode();;
        this.currencyCode = userProfileEntity.getCurrencyCode();

        this.levelUpTime = userProfileEntity.getLevelUpTime();
        this.releaseAward = userProfileEntity.getReleaseAward() != null ? userProfileEntity.getReleaseAward() : 0;

        this.dirty = false;
    }

    //== Рюкзак ==

    private boolean backpackRemove(Integer weaponId) {
        return backpack.remove(new BackpackItem(weaponId));
    }

    /**
     * вернет IItem по id
     *
     * @param weaponId идентификатор оружия
     * @return Item или null если нет такого
     */
    public BackpackItem getBackpackItemByWeaponId(Integer weaponId) {
        for(BackpackItem backpackItem : backpack) {
            if(backpackItem.getWeaponId() == weaponId)
                return backpackItem;
        }
        return null;
    }

    /**
     * добавит новый предмен игроку
     *
     * @param backpackItem инстанс предмета
     */
    public void addBackpackItem(BackpackItem backpackItem) {
        backpack.add(backpackItem);
        //меняем данные в структуре которую будем отправлять на клиент
        if(userProfileStructure != null) {
            userProfileStructure.backpack = ArrayUtils.add(userProfileStructure.backpack, BackpackUtils.toItem(backpackItem.getWeaponId(), backpackItem.getCount()));
        }
    }

    /**
     * установимть новое значение количества оружия у игрока
     *
     * @param weaponId id оружия у которого необходимо изменить количество
     * @param countInt новое значение количества
     */
    public void setBackpackItemCount(Integer weaponId, int countInt) {
        short count = (short) Math.min(Short.MAX_VALUE, countInt);
        getBackpackItemByWeaponId(weaponId).setCount(count);
        //меняем данные в структуре которую будем отправлять на клиент
        if(userProfileStructure != null) {
            for(int i = 0; i < userProfileStructure.backpack.length; i++) {
                short _weaponId = BackpackUtils.weaponId(userProfileStructure.backpack[i]);
                if(_weaponId == weaponId) {
                    userProfileStructure.backpack[i] = BackpackUtils.toItem(weaponId, count);
                    break;
                }
            }
        }
    }

    /**
     * удалить оружие из рюкзака
     *
     * @param weaponIds набор id оружий которые необходимо удалить
     * @return true если из рюкзака было удалено хоть одно (закончившееся) оружие
     */
    public boolean deleteBackpackItems(Set<Integer> weaponIds) {
        int expendedWeaponsCount = 0;
        for(Integer weaponId : weaponIds) {
            BackpackItem backpackItem = getBackpackItemByWeaponId(weaponId);
            if(backpackItem != null && backpackItem.isEmpty() && backpackRemove(weaponId)) {
                expendedWeaponsCount++;
            }
        }
        return expendedWeaponsCount > 0;
    }


    //== Команда ==

    public void initWormGroup(WormGroupsEntity wormGroupEntities) {
        int[] wormsGroup;
        // если есть команда
        if(wormGroupEntities != null) {
            int wormsGroupLength = wormGroupEntities.getTeamMembersCount();
            wormsGroup = Arrays.copyOf(wormGroupEntities.getTeamMembers(), wormsGroupLength);
            this.extraGroupSlotsCount = wormGroupEntities.getExtraGroupSlotsCount().byteValue();
        } else {
            // если нет команды, добавляем в неё только себя
            wormsGroup = new int[]{getId().intValue()};
            this.extraGroupSlotsCount = 0;
        }
        this.wormsGroup = wormsGroup;
        initTeamMembers(wormGroupEntities);
    }

    public void initTeamMembers(@Null WormGroupsEntity wormGroupEntities) {
        teamMembers = new TeamMember[wormsGroup.length];
        for(int i = 0; i < wormsGroup.length; i++) {
            int teamMemberId = wormsGroup[i];
            if(teamMemberId == id) {
                teamMembers[i] = null;
            } else if(wormGroupEntities != null) {
                teamMembers[i] = TeamMember.newTeamMember(wormGroupEntities.acceptTeamMemberMeta(i));
                String name = wormGroupEntities.getTeamMemberName(teamMemberId);
                if(name != null) {
                    teamMembers[i].setName(name);
                }
            }
        }
    }

    public int[] getWormsGroup() {
        return wormsGroup;
    }

    public void addInTeam(int profileId, @Null TeamMember teamMember) {
        wormsGroup = ArrayUtils.add(wormsGroup, profileId);
        teamMembers = ArrayUtils.add(teamMembers, teamMember);
    }

    public void replaceTeamMember(int teamMemberIndex, int profileId, @Null TeamMember teamMember) {
        wormsGroup[teamMemberIndex] = profileId;
        teamMembers[teamMemberIndex] = teamMember;
    }

    public void removeFromTeam(int profileId) {
        int index = ArrayUtils.indexOf(wormsGroup, profileId);
        wormsGroup = ArrayUtils.remove(wormsGroup, index);
        teamMembers = ArrayUtils.remove(teamMembers, index);
    }

    public boolean containsInTeam(int profileId) {
        return ArrayUtils.contains(wormsGroup, profileId);
    }

    @Null
    public TeamMember getFriendTeamMemberNullable(int teamMemberId) {
        TeamMember teamMember = ArrayUtils.contains(wormsGroup, teamMemberId) ? teamMembers[ArrayUtils.indexOf(wormsGroup, teamMemberId)] : null;
        if(teamMember == null) {
            log.info(String.format("[%s] член команды на найден по id [%s] wormsGroup=%s teamMembers=%s",
                    toString(), teamMemberId, Arrays.toString(wormsGroup), Arrays.toString(teamMembers)));
        }
        return teamMember;
    }

    public TeamMember getFriendTeamMember(int teamMemberId) {
        TeamMember teamMember = ArrayUtils.contains(wormsGroup, teamMemberId) ? teamMembers[ArrayUtils.indexOf(wormsGroup, teamMemberId)] : null;
        if(teamMember == null) {
            throw new IllegalStateException(String.format("[%s] член команды на найден по id [%s] wormsGroup=%s teamMembers=%s",
                    toString(), teamMemberId, Arrays.toString(wormsGroup), Arrays.toString(teamMembers)));
        }
        return teamMember;
    }

    public int getTeamSize() {
        int result = 0;
        if(userProfileStructure != null) {
            for(WormStructure wormStructure : userProfileStructure.wormsGroup()) {
                if(WormStructure.isActive(wormStructure))
                    result++;
            }
        }
        return result;
    }

    public void setWormsGroup(int[] wormsGroup) {
        this.wormsGroup = wormsGroup;
    }

    public short[] getStuff() {
        return stuff;
    }

    public void setStuff(short[] stuff) {
        this.stuff = stuff;
        if(userProfileStructure != null) {
            userProfileStructure.stuff = this.stuff;
        }
        dirty = true;
    }

    public byte[] getTemporalStuff() {
        return temporalStuff;
    }

    public void setTemporalStuff(byte[] temporalStuff) {
        this.temporalStuff = temporalStuff;
        if(userProfileStructure != null) {
            userProfileStructure.temporalStuff = this.temporalStuff;
        }
        dirty = true;
    }

    public Long getId() {
        return (long) id;
    }

    public long getProfileId() {
        return id;
    }

    @Override
    public byte getSocialId() {
        return socialId;
    }

    public void setSocialId(byte socialId) {
        this.socialId = socialId;
    }

    public String getName() {
        return name;
    }

    public void unsetName() {
        setName("");
    }

    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        WormStructure wormStructure = getWormStructure();
        if(wormStructure != null) {
            wormStructure.name = this.name;
        }
        dirty |= !Objects.equals(this.name, oldName);
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        // баланс не должен уйти в минус
        this.money = Math.max(money, 0);
        //меняем данные в структуре которую будем отправлять на клиент
        if(userProfileStructure != null) {
            userProfileStructure.money = this.money;
        }
        this.dirty = true;
    }

    public int getRealMoney() {
        return realMoney;
    }

    public void setRealMoney(int realMoney) {
        // баланс не должен уйти в минус
        this.realMoney = Math.max(realMoney, 0);
        //меняем данные в структуре которую будем отправлять на клиент
        if(userProfileStructure != null) {
            userProfileStructure.realMoney = this.realMoney;
        }
        this.dirty = true;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
        //меняем данные в структуре которую будем отправлять на клиент
        if(userProfileStructure != null) {
            userProfileStructure.rating = rating;
        }
        this.dirty = true;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = (byte) armor;
        //меняем данные в структуре которую будем отправлять на клиент
        WormStructure wormStructure = getWormStructure();
        if(wormStructure != null) {
            wormStructure.armor = (byte) armor;
        }
        this.dirty = true;
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = (byte) attack;
        //меняем данные в структуре которую будем отправлять на клиент
        WormStructure wormStructure = getWormStructure();
        if(wormStructure != null) {
            wormStructure.attack = (byte) attack;
        }
        this.dirty = true;
    }

    public int getBattlesCount() {
        return battlesCount;
    }

    public void setBattlesCount(int battlesCount) {
        if(battlesCount < 0)
            log.error(String.format("[%s] battlesCount in negative! battlesCount=%s, profile.battlesCount=%s", id, battlesCount, this.battlesCount), new Exception(""));
        battlesCount = Math.max(0, battlesCount);
        this.dirty |= this.battlesCount != battlesCount;
        this.battlesCount = battlesCount;
        // все бои доступны, необходимости отсылать уведомление уже нет
        if(battlesCount >= BattleService.MAX_BATTLE_COUNT && missionRestoredNotifyEvent != null) {
            //todo debug trace
            Logger log = LoggerFactory.getLogger(this.getClass());
            if(log.isDebugEnabled()) log.debug("{}.needSend -> false", missionRestoredNotifyEvent);

            missionRestoredNotifyEvent.needSend = false;
        }
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = (byte) level;
        //меняем данные в структуре которую будем отправлять на клиент
        WormStructure wormStructure = getWormStructure();
        if(wormStructure != null) {
            wormStructure.level = (byte) level;
        }
        this.dirty = true;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
        //меняем данные в структуре которую будем отправлять на клиент
        WormStructure wormStructure = getWormStructure();
        if(wormStructure != null) {
            wormStructure.experience = experience;
        }
        this.dirty = true;
    }

    public short getHat() {
        return hat;
    }

    public short getHat(int teamMemberId) {
        if(teamMemberId == id) {
            return hat;
        } else {
            TeamMember teamMember = getFriendTeamMember(teamMemberId);
            if(teamMember != null) {
                return teamMember.getHat();
            }
        }
        return 0;
    }

    public void setHat(short hat) {
        this.hat = hat;
        WormStructure wormStructure = getWormStructure();
        if(wormStructure != null) {
            wormStructure.hat = hat;
        }
        this.dirty = true;
    }

    public boolean setHat(int teamMemberId, short hat) {
        if(teamMemberId == id) {
            setHat(hat);
            return true;
        } else {
            TeamMember teamMember = getFriendTeamMember(teamMemberId);
            if(teamMember != null) {
                boolean result = teamMember.setHat(hat);
                if(!result) {
                    log.warn("[{}] одеть шапку [{}] не удалось!", teamMemberId, hat);
                    return false;
                }
                teamMembersDirty = true;

                WormStructure wormStructure = getWormStructure(teamMemberId);
                if(wormStructure != null) {
                    wormStructure.hat = hat;
                }
                return true;
            }
        }
        return false;
    }

    public short getRace() {
        return race;
    }

    public boolean inRace(Race race) {
        return race.getShortType() == this.race;
    }

    public void setRace(Race race) {
        setRace(race.getByteType());
    }

    public void setRace(short race) {
        WormStructure wormStructure = getWormStructure();
        if(wormStructure != null) {
            wormStructure.race = (byte) race;
        }
        this.dirty |= this.race != race;
        this.race = (byte) race;
    }

    public short getRaces() {
        return races;
    }

    public void setRaces(short races) {
        this.dirty |= this.races != races;
        this.races = races;
    }

    public short getKit() {
        return kit;
    }

    public short getKit(int teamMemberId) {
        if(teamMemberId == id) {
            return kit;
        } else {
            TeamMember teamMember = getFriendTeamMember(teamMemberId);
            if(teamMember != null) {
                return teamMember.getKit();
            }
        }
        return 0;
    }

    public short getStructureHat(int teamMemberId) {
        WormStructure wormStructure = getWormStructure(teamMemberId);
        if(wormStructure != null) {
            return wormStructure.hat;
        }
        return 0;
    }

    public short getStructureKit(int teamMemberId) {
        WormStructure wormStructure = getWormStructure(teamMemberId);
        if(wormStructure != null) {
            return wormStructure.kit;
        }
        return 0;
    }

    public void setKit(short kit) {
        this.kit = kit;
        WormStructure wormStructure = getWormStructure();
        if(wormStructure != null) {
            wormStructure.kit = kit;
        }
        this.dirty = true;
    }

    public boolean setKit(int teamMemberId, short kit) {
        if(teamMemberId == id) {
            setKit(kit);
            return true;
        } else {
            TeamMember teamMember = getFriendTeamMember(teamMemberId);
            if(teamMember != null) {
                boolean result = teamMember.setKit(kit);
                if(!result) {
                    log.warn("[{}] одеть артефакт [{}] не удалось!", teamMemberId, kit);
                    return false;
                }
                teamMembersDirty = true;

                WormStructure wormStructure = getWormStructure(teamMemberId);
                if(wormStructure != null) {
                    wormStructure.kit = kit;
                }
                return true;
            }
        }
        return false;
    }

    public int[] getOrderedFriends() {
        return orderedFriends;
    }

    public void setOrderedFriends(int[] orderedFriends) {
        this.orderedFriends = orderedFriends;
    }

    public UserProfileStructure getUserProfileStructure() {
        return userProfileStructure;
    }

    public void setUserProfileStructure(UserProfileStructure userProfileStructure) {
        this.userProfileStructure = userProfileStructure;
    }

    @Null
    public WormStructure getWormStructure() {
        return getWormStructure(id);
    }

    @Null
    public WormStructure getWormStructure(long teamMemberId) {
        if(userProfileStructure != null) {
            for(WormStructure wormStructure : userProfileStructure.wormsGroup()) {
                if(wormStructure != null) {
                    if(wormStructure.ownerId == teamMemberId) {
                        return wormStructure;
                    }
                } else {
                    log.error("null wormStructure in wormsGroup {}", Arrays.toString(userProfileStructure.wormsGroup()));
                }
            }
        }
        return null;
    }

    public BattleState getBattleState() {
        return BattleState.valueOf(battleState.get());
    }

    public boolean inBattleState(BattleState battleState) {
        return this.battleState.get() == battleState.getType();
    }

    public boolean compareAndSetBattleState(BattleState exceptedState, BattleState newState) {
        return this.battleState.compareAndSet(exceptedState.getType(), newState.getType());
    }

    public BattleState setBattleState(BattleState battleState) {
        return BattleState.valueOf(this.battleState.getAndSet(battleState.getType()));
    }

    public void wipeSimpleBattleState() {
        setBattleState(BattleState.NOT_IN_BATTLE);
        setBattleId(0);
        setMissionId((short) 0);
        missionLog = null;
        defeatBattleAward = null;
    }

    public long getLastBattleTime() {
        return lastBattleTime * 1000L;
    }

    public void setLastBattleTime(long lastBattleTime) {
        this.lastBattleTime = (int) (lastBattleTime / 1000L);
        this.dirty = true;
    }

    public long getStartBattleTime() {
        return startBattleTime;
    }

    public void setStartBattleTime(long startBattleTime) {
        this.startBattleTime = startBattleTime;
    }

    public Date getLastLoginTime() {
        return lastLoginTime;
    }

    public LocalDateTime getLastLoginDateTime() {
        long lastLoginTimeMillis = lastLoginTime != null ? lastLoginTime.getTime() : 0;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(lastLoginTimeMillis), ZoneId.systemDefault());
    }

    public void setLastLoginTime(Date lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
        this.dirty = true;
    }

    public void setLastLoginDateTime(LocalDateTime lastLoginTime) {
        Instant instant = lastLoginTime.atZone(ZoneId.systemDefault()).toInstant();
        setLastLoginTime(Date.from(instant));
    }

    public int getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(int logoutTime) {
        this.logoutTime = logoutTime;
        this.dirty = true;
    }

    public Date getLastSearchTime() {
        return lastSearchTime > 0 ? new Date(lastSearchTime * 1000L) : null;
    }

    public void setLastSearchTime(Date lastSearchTime) {
        this.lastSearchTime = lastSearchTime != null ? (int) (lastSearchTime.getTime() / 1000L) : 0;
        this.dirty = true;
    }

    public long getLastSearchFriendTime() {
        return lastSearchFriendTime * 1000L;
    }

    public void setLastSearchFriendTime(long lastSearchFriendTime) {
        this.lastSearchFriendTime = (int) (lastSearchFriendTime / 1000L);
    }

    public long getBattleId() {
        return battleId;
    }

    public void setBattleId(long battleId) {
        this.battleId = battleId;
    }

    public byte getCheatInstantWin() {
        return cheatInstantWin;
    }

    public void setCheatInstantWin(byte cheatInstantWin) {
        this.cheatInstantWin = cheatInstantWin;
    }

    public byte getCheatHouseSearch() {
        return cheatHouseSearch;
    }

    public void setCheatHouseSearch(byte cheatHouseSearch) {
        this.cheatHouseSearch = cheatHouseSearch;
    }

    public byte getCheatInstantHouseSearch() {
        return cheatInstantHouseSearch;
    }

    public void setCheatInstantHouseSearch(byte cheatInstantHouseSearch) {
        this.cheatInstantHouseSearch = cheatInstantHouseSearch;
    }

    public byte getLoginSequence() {
        return loginSequence;
    }

    public void setLoginSequence(byte loginSequence) {
        this.dirty |= this.loginSequence != loginSequence;
        this.loginSequence = loginSequence;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(o == null || getClass() != o.getClass()) {
            return false;
        }
        UserProfile that = (UserProfile) o;
        return id == that.id;
    }

    @Override
    public int compareTo(Object o) {
        UserProfile profile = (UserProfile) o;
        if(this.equals(profile)) {
            return 0;
        } else if(this.getId() > profile.getId()) {
            return 1;
        } else {
            return -1;
        }
    }

    public int getReactionRate() {
        return reactionRate;
    }

    public void setReactionRate(int reactionRate) {
        this.reactionRate = reactionRate;
        //меняем в кеше
        if(userProfileStructure != null) {
            userProfileStructure.reactionRate = this.reactionRate;
        }
        this.dirty = true;
    }

    public short getCurrentMission() {
        return currentMission;
    }

    public void setCurrentMission(short currentMission) {
        this.currentMission = currentMission;
        this.dirty = true;
    }

    public short getCurrentNewMission() {
        return currentNewMission;
    }

    public void setCurrentNewMission(short currentNewMission) {
        this.currentNewMission = currentNewMission;
        this.dirty = true;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        dirty |= locale != this.locale;
        this.locale = locale;
    }

    public byte getRenameAct() {
        return renameAct;
    }

    public void setRenameAct(int renameAct) {
        int oldValue = this.renameAct;
        if(renameAct < 0) renameAct = 0;
        this.renameAct = (byte) renameAct;
        dirty |= this.renameAct != oldValue;
    }

    public byte getRenameVipAct() {
        if(isVipActive()){
           return renameVipAct;
        } else {
            setRenameVipAct(0);
            return 0;
        }
    }

    public void setRenameVipAct(int renameVipAct) {
        int oldValue = this.renameVipAct;
        if(renameVipAct < 0) renameVipAct = 0;
        this.renameVipAct = (byte) renameVipAct;
        dirty |= this.renameVipAct != oldValue;
    }



    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public String getProfileStringId() {
        return profileStringId;
    }

    public void setProfileStringId(String profileStringId) {
        this.profileStringId = profileStringId;
        if(userProfileStructure != null) {
            userProfileStructure.profileStringId = this.profileStringId;
        }
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return profileStringId == null || profileStringId.isEmpty() ? String.valueOf(id) : String.format("%s:%s", profileStringId, id);
    }

    public int[] getGrantedAchieveAwards() {
        return grantedAchieveAwards;
    }

    public void setGrantedAchieveAwards(int[] grantedAchieveAwards) {
        this.grantedAchieveAwards = grantedAchieveAwards;
    }

    public void cleanGrantedAchieveAwards() {
        grantedAchieveAwards = null;
    }

    public short[] getRecipes() {
        return recipes;
    }

    public void setRecipes(short[] recipes) {
        this.recipes = recipes;
        if(userProfileStructure != null) {
            userProfileStructure.recipes = this.recipes;
        }
        dirty = true;
    }

    public byte getBestRank() {
        return bestRank;
    }

    public void setBestRank(int bestRank) {
        dirty |= this.bestRank != bestRank;
        if(userProfileStructure != null) {
            userProfileStructure.bestRank = (byte) bestRank;
        }
        this.bestRank = (byte) bestRank;
    }

    public int getRankPoints() {
        return rankPoints;
    }

    public void setRankPoints(int rankPoints) {
        dirty |= this.rankPoints != rankPoints;
        if(userProfileStructure != null) {
            userProfileStructure.rankPoints = rankPoints;
        }
        this.rankPoints = rankPoints;
    }

    public ReagentsEntity getReagents() {
        return reagents;
    }

    public void setReagents(ReagentsEntity reagents) {
        this.reagents = reagents;
    }

    public byte[] getReagentsForBattle() {
        return reagentsForBattle;
    }

    public void setReagentsForBattle(@Null byte[] reagentsForBattle) {
        this.reagentsForBattle = reagentsForBattle;
    }

    public long getLastProcessedPvpBattleId() {
        return lastProcessedPvpBattleId;
    }

    public void setLastProcessedPvpBattleId(long lastProcessedPvpBattleId) {
        this.lastProcessedPvpBattleId = lastProcessedPvpBattleId;
    }

    public short getComebackedFriends() {
        return comebackedFriends;
    }

    public void incComebackedFriends() {
        comebackedFriends++;
        dirty = true;
    }

    public void setComebackedFriends(short comebackedFriends) {
        this.comebackedFriends = comebackedFriends;
        dirty = true;
    }

    public boolean isNeedRewardCallbackers() {
        return needRewardCallbackers;
    }

    public void setNeedRewardCallbackers(boolean needRewardCallbackers) {
        this.needRewardCallbackers = needRewardCallbackers;
    }

    public long getLastCallbackedFriendId() {
        return (long) lastCallbackedFriendId;
    }

    public void setLastCallbackedFriendId(long lastCallbackedFriendId) {
        this.lastCallbackedFriendId = (int) lastCallbackedFriendId;
    }

    public int getGroupCount() {
        return wormsGroup != null ? getActiveTeamMembersCount() : 1;
    }

    public short getMissionId() {
        return missionId;
    }

    public void setMissionId(short missionId) {
        this.missionId = missionId;
    }

    // геттер/сеттер к missionLog
    public MissionLogStructure createMissionLog() {
        this.missionLog = new MissionLogStructure();
        return this.missionLog;
    }

    @Null
    public MissionLogStructure getMissionLog() {
        return missionLog;
    }

    public void setMissionLog(MissionLogStructure missionLog) {
        this.missionLog = missionLog;
    }

    public int getLastPaymentTime() {
        return lastPaymentTime;
    }

    public LocalDateTime getLastPaymentDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(lastPaymentTime), ZoneId.systemDefault());
    }

    public void setLastPaymentTime(long lastPaymentTime) {
        this.lastPaymentTime = (int) (lastPaymentTime / 1000L);
    }

    public int getClanId() {
        if(userProfileStructure != null && userProfileStructure.clanMember != null) {
            return userProfileStructure.clanMember.getClanId();
        } else {
            return 0;
        }
    }

    public Rank getRankInClan() {
        if(userProfileStructure != null && userProfileStructure.clanMember != null) {
            return userProfileStructure.clanMember.getRank();
        } else {
            return Rank.SOLDIER;
        }
    }

    public byte getExtraGroupSlotsCount() {
        return extraGroupSlotsCount;
    }

    public void setExtraGroupSlotsCount(byte extraGroupSlotsCount) {
        this.extraGroupSlotsCount = extraGroupSlotsCount;
        if(userProfileStructure != null) {
            userProfileStructure.extraGroupSlotsCount = this.extraGroupSlotsCount;
        }
        this.teamMembersDirty = true;
    }

    // из имеющихся teamMember'ов сколько активно, включая себя
    public byte getActiveTeamMembersCount() {
        byte result = 1; // сам игрок - всегда активен
        for(TeamMember teamMember : teamMembers) {
            if(teamMember != null && teamMember.isActive()) {
                result++;
            }
        }
        return result;
    }

    // из имеющихся teamMember'ов сколько активно, включая себя кроме соклана
    public byte getActiveTeamMembersCountExceptSoclan() {
        byte result = 1; // сам игрок - всегда активен
        for(TeamMember teamMember : teamMembers) {
            if(teamMember != null && teamMember.isActive() && !(teamMember instanceof SoclanTeamMember)) {
                result++;
            }
        }
        return result;
    }

    public TeamMember[] getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(TeamMember[] teamMembers) {
        this.teamMembers = teamMembers;
    }

    public boolean isTeamMembersDirty() {
        return teamMembersDirty;
    }

    public void setTeamMembersDirty(boolean teamMembersDirty) {
        this.teamMembersDirty = teamMembersDirty;
    }

    /**
     * является ли профиль "заглушкой", профилем игрока удаленного из базы
     */
    public boolean isStubProfile() {
        return level == 0;
    }

    public TrueSkillEntity getTrueSkillEntity() {
        return trueSkillEntity;
    }

    public void setTrueSkillEntity(TrueSkillEntity trueSkillEntity) {
        this.trueSkillEntity = trueSkillEntity;
    }

    public boolean isClosedClanSeasonAwardGranted(int closedClanSeasonId) {
        return this.closedClanSeasonId == (byte) closedClanSeasonId;
    }

    public void setClosedClanSeasonAwardGranted(int closedSeasonId) {
        this.closedClanSeasonId = (byte) closedSeasonId;
    }

    public boolean isClosedSeasonAwardGranted(int closedSeasonMonth) {
        return this.closedSeasonMonth == (byte) closedSeasonMonth;
    }

    public void setClosedSeasonMonth(int closedSeasonMonth) {
        this.closedSeasonMonth = (byte) closedSeasonMonth;
    }

    public String toStringAsObject() {
        return "UserProfile@" + Integer.toHexString(super.hashCode());
    }

    public long getLastProcessedBattleId() {
        return lastProcessedBattleId;
    }

    public void setLastProcessedBattleId(long lastProcessedBattleId) {
        this.lastProcessedBattleId = lastProcessedBattleId;
    }

    public BackpackConfEntity getBackpackConfs() {
        return backpackConfs;
    }

    public void setBackpackConfs(BackpackConfEntity backpackConfs) {
        this.backpackConfs = backpackConfs;
    }

    public void setBackpack(List<BackpackItem> backpack) {
        this.backpack = new CopyOnWriteArrayList<>(backpack);
    }

    public List<BackpackItem> getBackpack() {
        return backpack;
    }

    private int getPickUpDailyBonusDay() {
        return pickUpDailyBonus % 100;
    }

    private int getPickUpDailyBonusMonth() {
        return pickUpDailyBonus / 100;
    }

    public short getPickUpDailyBonus() {
        return pickUpDailyBonus;
    }

    public void setPickUpDailyBonus(Date pickUpDailyBonus) {
        this.pickUpDailyBonus = (short) ((pickUpDailyBonus.getMonth() + 1) * 100 + pickUpDailyBonus.getDate());
        dirty = true;
    }

    public boolean isMatchPickUpDailyBonus(Date date) {
        return getPickUpDailyBonusMonth() == date.getMonth() + 1 && getPickUpDailyBonusDay() == date.getDate();
    }

    public ColiseumEntity getColiseumEntity() {
        return coliseumEntity;
    }

    public void setColiseumEntity(ColiseumEntity coliseumEntity) {
        this.coliseumEntity = coliseumEntity;
    }

    public MercenariesEntity getMercenariesEntity() {
        return mercenariesEntity;
    }

    public void setMercenariesEntity(MercenariesEntity mercenariesEntity) {
        this.mercenariesEntity = mercenariesEntity;
    }

    public QuestEntity getQuestEntity() {
        return questEntity;
    }

    public void setQuestEntity(QuestEntity questEntity) {
        this.questEntity = questEntity;
    }

    public int getSelectRaceTime() {
        return selectRaceTime;
    }

    public void setSelectRaceTime(int selectRaceTime) {
        dirty |= selectRaceTime != this.selectRaceTime;
        this.selectRaceTime = selectRaceTime;
    }

    public void setProfileId(long profileId) {
        //do nothing
    }

    public void setGroupCount(int groupCount) {
        //do nothing
    }

    public byte[] getSkins() {
        return skins;
    }

    public void setSkins(byte[] skins) {
        this.skins = skins;
        dirty = true;
    }

    public CookiesEntity getCookiesEntity() {
        return cookiesEntity;
    }

    public void setCookiesEntity(CookiesEntity cookiesEntity) {
        this.cookiesEntity = cookiesEntity;
    }

    public int getVipExpiryTime() {
        return vipExpiryTime;
    }

    public boolean isVipActive(){
        return vipExpiryTime * 1000L > System.currentTimeMillis();
    }

    public void setVipExpiryTime(int vipExpiryTime) {
        dirty |= vipExpiryTime != this.vipExpiryTime;
        this.vipExpiryTime = vipExpiryTime;
        if(userProfileStructure != null && userProfileStructure.rentedItems != null) {
            userProfileStructure.rentedItems.activeUntil = vipExpiryTime;
        }
    }

    @Null // - если ещё не подгружены через DepositService.getDepositsFor(profile)
    public DepositEntity[] getDeposits() {
        return deposits;
    }

    public int getDepositsCount() {
        if (deposits != null) {
            return deposits.length;
        } else {
            return 0;
        }
    }

    public void setDeposits(DepositEntity[] deposits) {
        this.deposits = deposits;
    }

    public void addDeposit(DepositEntity deposit) {
        if (deposits == null) {
            deposits = new DepositEntity[]{ deposit };
        } else {
            deposits = ArrayUtils.add(deposits, deposit);
        }
    }

    public void removeDeposit(DepositEntity deposit) {
        deposits = ArrayUtils.removeElement(deposits, deposit);
    }

    public int getVipSubscriptionId() {
        return vipSubscriptionId;
    }

    public void setVipSubscriptionId(int vipSubscriptionId) {
        dirty |= vipSubscriptionId != this.vipSubscriptionId;
        this.vipSubscriptionId = vipSubscriptionId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        dirty |= !Objects.equals(countryCode, this.countryCode);
        this.countryCode = countryCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        dirty |= !Objects.equals(currencyCode, this.currencyCode);
        this.currencyCode = currencyCode;
    }

    public Date getLevelUpTime() {
        return levelUpTime;
    }

    public void setLevelUpTime(Date levelUpTime) {
        dirty |= !Objects.equals(levelUpTime, this.levelUpTime);
        this.levelUpTime = levelUpTime;
    }

    public short getReleaseAward() {
        return releaseAward;
    }

    public void setReleaseAward(short releaseAward) {
        dirty |= releaseAward != this.releaseAward;
        this.releaseAward = releaseAward;
    }

}
