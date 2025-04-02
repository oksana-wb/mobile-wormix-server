package com.pragmatix.app.services;

import com.pragmatix.app.common.BoostFamily;
import com.pragmatix.app.init.StuffCreator;
import com.pragmatix.app.messages.server.StuffExpired;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.StuffHaving;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.group.SoclanTeamMember;
import com.pragmatix.app.model.group.TeamMember;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.IGameApp;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.sessions.Connection;
import com.pragmatix.gameapp.sessions.Connections;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.server.Server;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.pragmatix.app.common.Connection.MAIN;
import static com.pragmatix.app.services.TemporalStuffService.*;
import static com.pragmatix.gameapp.common.SimpleResultEnum.ERROR;
import static com.pragmatix.gameapp.common.SimpleResultEnum.SUCCESS;

@Service
public class StuffService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private StuffCreator stuffCreator;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private TemporalStuffService temporalStuffService;

    @Resource
    private PermanentStuffService permanentStuffService;

    @Resource
    private ProfileService profileService;

    @Resource
    private IGameApp gameApp;

    @Resource
    private GroupService groupService;

    private boolean initialized = false;

    public void init() {
        initialized = true;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void cronTask() {
        if(initialized) {
            List<Short> expiredStuff = new ArrayList<>(2);
            int expiredStuffProfilesCount = 0;
            int expiredStuffsCount = 0;
            for(Session session : gameApp.getSessions()) {
                Object user = session.getUser();
                if(user instanceof UserProfile) {
                    UserProfile userProfile = (UserProfile) user;
                    if(userProfile.isOnline()) {
                        // удаляем истекшие предметы
                        removeExpiredStuff(userProfile, expiredStuff, true);

                        if(expiredStuff.size() > 0) {
                            // если такие есть, уведомляем клиент
                            expiredStuffProfilesCount++;
                            expiredStuffsCount += expiredStuff.size();

                            StuffExpired stuffExpired = new StuffExpired(expiredStuff, userProfile.getHat(), userProfile.getKit());
                            sentToMainConnection(stuffExpired, session);

                            expiredStuff.clear();
                        }
                    }
                }
            }
            Server.sysLog.info("истекло {} предметов у {} игроков", expiredStuffsCount, expiredStuffProfilesCount);
        }
    }

    // отправить сообщение игроку в MAIN коннект
    private void sentToMainConnection(Object message, Session session) {
        if(session != null) {
            Connection connection = session.getConnection(MAIN);
            if(connection != null) {
                connection.send(message, gameApp.getSerializer());
            }
        }
    }

    public void sentStuffExpiredToMainConnection(UserProfile profile, List<Short> expiredStuff) {
        StuffExpired stuffExpired = new StuffExpired(expiredStuff, profile.getHat(), profile.getKit());
        sentToMainConnection(stuffExpired,  gameApp.getSessions().get(profile));
    }

    public void removeExpiredStuff(UserProfile userProfile) {
        removeExpiredStuff(userProfile, null, true);
    }

    public void removeExpiredStuff(UserProfile userProfile, @Null List<Short> expiredStuff, boolean removeForTeam) {
        if(expiredStuff == null) {
            expiredStuff = new ArrayList<>(2);
        }
        byte[] processedTemporalStuff = temporalStuffService.removeExpiredStuff(userProfile.getTemporalStuff(), expiredStuff);

        for(Short stuffId : expiredStuff) {
            Stuff stuff = getStuff(stuffId);
            if(stuff != null) {
                deselectStuffIfMatch(userProfile, stuff, dailyRegistry.getPrevHat(userProfile.getId()), dailyRegistry.getPrevKit(userProfile.getId()));
            }
        }

        if(log.isDebugEnabled()) {
            if(expiredStuff.size() > 0) {
                log.debug("[{}] удалены временные предметы {}, актуальны {}", userProfile, expiredStuff, TemporalStuffService.toStringTemporalStuff(processedTemporalStuff));
            } else if(processedTemporalStuff.length > 0) {
                log.debug("[{}] временные предметы {}", userProfile, TemporalStuffService.toStringTemporalStuff(processedTemporalStuff));
            }
        }

        if(expiredStuff.size() > 0) {
            userProfile.setTemporalStuff(processedTemporalStuff);
        }

        try {
            if(removeForTeam) {
                for(int i = 1; i < userProfile.getWormsGroup().length; i++) {
                    Long teamMemberProfileId = (long) userProfile.getWormsGroup()[i];
                    TeamMember teamMember = userProfile.getTeamMembers()[i];
                    // своя шапка может быть только у соклановца
                    if(teamMember instanceof SoclanTeamMember) {
                        UserProfile teamMemberProfile = profileService.getUserProfile(teamMemberProfileId);
                        if(teamMemberProfile != null) {
                            ArrayList<Short> memberExpiredStuff = new ArrayList<>(2);
                            removeExpiredStuff(teamMemberProfile, memberExpiredStuff, false);

                            // уведомить игрока, что ему сняли временный предмет
                            if(teamMemberProfile.isOnline() && memberExpiredStuff.size() > 0) {
                                StuffExpired stuffExpired = new StuffExpired(memberExpiredStuff, teamMemberProfile.getHat(), teamMemberProfile.getKit());
                                sentToMainConnection(stuffExpired, gameApp.getSessions().get(teamMemberProfile));
                            }

                            UserProfileStructure userProfileStructure = userProfile.getUserProfileStructure();
                            if(userProfileStructure != null && memberExpiredStuff.size() > 0 && i < userProfileStructure.wormsGroup().length) {
                                WormStructure wormStructure = userProfileStructure.wormsGroup()[i];
                                if(wormStructure.ownerId == teamMemberProfileId) {
                                    wormStructure.hat = teamMemberProfile.getHat();
                                    wormStructure.kit = teamMemberProfile.getKit();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    // удаляем временные шапки из массива постоянных
    public void removeOldTempStuff(UserProfile userProfile) {
        int needRemove = 0;
        for(short stuffId : userProfile.getStuff()) {
            Stuff stuff = getStuff(stuffId, false);
            if(stuff == null) {
                needRemove++;
                Server.sysLog.info("[{}] удаляем не существующий предмет {}", userProfile, stuffId);
            } else if(stuff.isTemporal()) {
                needRemove++;
                Server.sysLog.info("[{}] удаляем временный предмет из постоянных {}:{}", userProfile, stuffId, stuff.getName());
            }
        }

        if(needRemove > 0) {
            short[] stuffArr = new short[userProfile.getStuff().length - needRemove];
            int i = 0;
            for(short stufId : userProfile.getStuff()) {
                Stuff stuff = getStuff(stufId, false);
                if(stuff != null && !stuff.isTemporal()) {
                    stuffArr[i] = stufId;
                    i++;
                }
            }

            userProfile.setStuff(stuffArr);
        }

        // снимаем предметы, которых уже нет
        if(userProfile.getHat() > 0 && !isExist(userProfile, userProfile.getHat())) {
            deselectHat(userProfile, userProfile.getId().intValue());
        }
        if(userProfile.getKit() > 0 && !isExist(userProfile, userProfile.getKit())) {
            deselectKit(userProfile, userProfile.getId().intValue());
        }
    }

    public boolean addStuffPermanentlyOrTemporarily(UserProfile profile, short stuffId) {
        Stuff stuff = getStuff(stuffId);
        if(stuff == null) {
            return false;
        }
        if(stuff.isTemporal()) {
            return addStuff(profile, stuffId, stuff.getExpireTime(), Stuff.EXPIRE_TIME_UNIT, true);
        } else {
            return addStuff(profile, stuffId);
        }
    }

    public boolean addOrExpandTemporalStuff(UserProfile profile, short stuffId) {
        Stuff stuff = getStuff(stuffId);
        if(stuff == null) {
            return false;
        }
        if(stuff.isTemporal()) {
            if(temporalStuffService.isExist(profile, stuffId)){
                long expireTime = temporalStuffService.getExpireDate(profile, stuffId);
                return addStuff(profile, stuffId,Math.max(0, expireTime - System.currentTimeMillis()) + stuff.getExpireTime(), Stuff.EXPIRE_TIME_UNIT, true);
            } else {
                return addStuff(profile, stuffId, stuff.getExpireTime(), Stuff.EXPIRE_TIME_UNIT, true);
            }
        } else {
            return false;
        }
    }

    public boolean addStuff(UserProfile profile, short stuffId) {
        return addStuff(profile, stuffId, true);
    }

    /**
     * покупка нового предмета (шапки или артефакта)
     *
     * @param profile профайл игрока купивший шапку
     * @param stuffId id предмета
     */
    public boolean addStuff(UserProfile profile, short stuffId, boolean setStuff) {
        Stuff stuff = getStuff(stuffId);
        if(stuff == null) {
            return false;
        }
        if(stuff.isBoost()) {
            log.error("Ускоритель не может быть постоянным! {}", stuff);
            return false;
        }

        boolean result = permanentStuffService.addStuff(profile, stuff);

        if(result && setStuff) {
            setStuff(profile, stuff);
        }
        return result;
    }

    /**
     * выдать шапку на время
     *
     * @param profile профайл игрока
     * @param stuffId id предмета
     * @param expire  на сколько выдать шапку
     */
    public boolean addStuff(UserProfile profile, short stuffId, long expire, TimeUnit timeUnit, boolean setStuff) {
        return addStuff(profile, stuffId, expire, timeUnit, setStuff, false);
    }

    public boolean addStuff(UserProfile profile, short stuffId, long expire, TimeUnit timeUnit, boolean setStuff, boolean expand) {
        Stuff stuff = getStuff(stuffId);
        if(stuff == null) {
            return false;
        }
        boolean result = temporalStuffService.addStuff(profile, stuff, expire, timeUnit, expand);

        if(result && setStuff && !stuff.isBoost()) {
            setStuff(profile, stuff);
        }
        return result;
    }

    /**
     * выдать предмет до определенного времени
     *
     * @param profile             профайл игрока
     * @param stuffId             id предмета
     * @param expireTimeInSeconds до когоко времени выдать предмет
     */
    public boolean addStuffUntilTime(UserProfile profile, short stuffId, int expireTimeInSeconds, boolean setStuff) {
        return addStuff(profile, stuffId, expireTimeInSeconds - AppUtils.currentTimeSeconds(), TimeUnit.SECONDS, setStuff);
    }

    protected SimpleResultEnum setStuff(UserProfile profile, Stuff stuff) {
        return setStuff(profile, profile.getId().intValue(), stuff);
    }

    protected SimpleResultEnum setStuff(UserProfile profile, int teamMemberId, Stuff stuff) {
        if(stuff.isBoost()) {
            log.error("нельзя одеть ускоритель! {}", stuff);
            return ERROR;
        }
        if(alreadyWearing(profile, stuff.getStuffId())) {
            log.warn("предмет [{}] уже одет!", stuff.getStuffId());
            return ERROR;
        }
        if(stuff.isKit()) {
            boolean result = profile.setKit(teamMemberId, stuff.getStuffId());
            if(!result) {
                return ERROR;
            }

            // если одевается постоянный предмет на хозяина, запоминаем его, чтобы потом вернуть его когда снимем временный по истечении времени
            if(!stuff.isTemporal() && teamMemberId == profile.getId()) {
                dailyRegistry.setPrevKit(profile.getId(), profile.getKit());
            }
        } else {
            boolean result = profile.setHat(teamMemberId, stuff.getStuffId());
            if(!result) {
                return ERROR;
            }
            // если одевается постоянный предмет на хозяина, запоминаем его, чтобы потом вернуть его когда снимем временный по истечении времени
            if(!stuff.isTemporal() && teamMemberId == profile.getId()) {
                dailyRegistry.setPrevHat(profile.getId(), profile.getHat());
            }
        }
        return SUCCESS;
    }

    private boolean alreadyWearing(UserProfile profile, short stuffId) {
        if(profile.getKit() == stuffId || profile.getHat() == stuffId) {
            return true;
        }
        for(TeamMember teamMember : profile.getTeamMembers()) {
            if(teamMember != null && (teamMember.getKit() == stuffId || teamMember.getHat() == stuffId)) {
                return true;
            }
        }
        return false;
    }

    public void removeDublicate(UserProfile profile) {
        TeamMember[] teamMembers = profile.getTeamMembers();
        for(int i = 0; i < teamMembers.length; i++) {
            TeamMember teamMember = teamMembers[i];
            if(teamMember != null) {
                int teamMemberId = profile.getWormsGroup()[i];
                if(teamMember.getHat() > 0 && teamMember.getHat() == profile.getHat()) {
                    log.error(String.format("[%s] удаляем дубликат %s", profile, getStuff(teamMember.getHat())));
                    deselectHat(profile, teamMemberId);
                }
                if(teamMember.getKit() > 0 && teamMember.getKit() == profile.getKit()) {
                    log.error(String.format("[%s] удаляем дубликат %s", profile, getStuff(teamMember.getKit())));
                    deselectKit(profile, teamMemberId);
                }
            }
        }
    }

    @Null
    public Stuff getStuff(int stuffId) {
        return getStuff((short) stuffId);
    }

    @Null
    public Stuff getStuff(short stuffId) {
        return getStuff(stuffId, true);
    }

    @Null
    public Stuff getStuff(short stuffId, boolean orThrow) {
        Stuff stuff = stuffCreator.getStuff(stuffId);
        if(stuff == null && orThrow) {
            try {
                throw new IllegalArgumentException("предмет не найден по id [" + stuffId + "]");
            } catch (IllegalArgumentException e) {
                log.error(e.toString(), e);
            }
        }
        return stuff;
    }

    @Null
    public Stuff getHat(short hatId) {
        Stuff stuff = getStuff(hatId);
        if(stuff != null) {
            if(!stuff.isKit()) {
                return stuff;
            } else {
                log.error("предмет не является шапкой {}", stuff);
                return null;
            }
        }
        return null;
    }

    @Null
    public Stuff getKit(short kitId) {
        Stuff stuff = getStuff(kitId);
        if(stuff != null) {
            if(stuff.isKit()) {
                return stuff;
            } else {
                log.error("предмет не является артефактом {}", stuff);
                return null;
            }
        }
        return null;
    }

    /**
     * Удалить предмет
     *
     * @param profile профайл игрока у которого будем удалять шапку
     * @param stuffId id предмета которую нужно удалить
     * @return результат
     */
    public boolean removeStuff(UserProfile profile, short stuffId) {
        Stuff stuff = getStuff(stuffId);
        if(stuff == null) {
            return false;
        }
        if(stuff.isTemporal()) {
            if(temporalStuffService.isExist(profile, stuffId)) {
                temporalStuffService.removeStuffFor(profile, stuffId);
                deselectStuffIfMatch(profile, stuff, dailyRegistry.getPrevHat(profile.getId()), dailyRegistry.getPrevKit(profile.getId()));

                return true;
            }
        } else if(permanentStuffService.isExist(profile, stuffId)) {
            permanentStuffService.removeStuffFor(profile, stuffId);
            // при снятии постоянного предета prevHatId и prevKitId не актуальны
            deselectStuffIfMatch(profile, stuff, (short) 0, (short) 0);

            return true;
        }
        return false;
    }

    public void deselectStuffIfMatch(UserProfile profile, Stuff stuff, short prevHatId, short prevKitId) {
        // сначало нужно проверить не одета ли эта шапка, если одета, то снять
        if(stuff.isHat()) {
            if(profile.getHat() == stuff.getStuffId()) {
                profile.setHat((short) 0);

                //после снятия временной шапки, возвращаем постоянную шапку ту, что была до этого
                if(stuff.isTemporal() && prevHatId > 0) {
                    Stuff prevHat = getHat(prevHatId);
                    if(prevHat != null && permanentStuffService.isExist(profile, prevHatId)) {
                        selectHat(profile, profile.getId().intValue(), prevHatId);
                    }
                }
            } else {
                // проверяем не одета ли шапка на члена команды
                TeamMember[] teamMembers = profile.getTeamMembers();
                for(int i = 0; i < teamMembers.length; i++) {
                    TeamMember teamMember = teamMembers[i];
                    if(teamMember != null && teamMember.getHat() == stuff.getStuffId()) {
                        int teamMemberId = profile.getWormsGroup()[i];
                        deselectHat(profile, teamMemberId);
                    }
                }
            }
        } else if(stuff.isKit()) {
            if(profile.getKit() == stuff.getStuffId()) {
                // проверить не одета ли этот артефакт, если одет, то снять
                profile.setKit((short) 0);

                //после снятия временного артефакта, возвращаем постоянный артефакт тот, что был до этого
                if(stuff.isTemporal() && prevKitId > 0) {
                    Stuff prevKit = getKit(prevKitId);
                    if(prevKit != null && permanentStuffService.isExist(profile, prevKitId)) {
                        selectKit(profile, profile.getId().intValue(), prevKitId);
                    }
                }
            } else {
                // проверяем не одет ли артефакт на члена команды
                TeamMember[] teamMembers = profile.getTeamMembers();
                for(int i = 0; i < teamMembers.length; i++) {
                    TeamMember teamMember = teamMembers[i];
                    if(teamMember != null && teamMember.getKit() == stuff.getStuffId()) {
                        int teamMemberId = profile.getWormsGroup()[i];
                        deselectKit(profile, teamMemberId);
                    }
                }
            }
        }
    }

    /**
     * Снять шапку
     */
    public void deselectAll(UserProfile profile) {
        profile.setHat((short) 0);
        profile.setKit((short) 0);
        for(int i = 0; i < profile.getWormsGroup().length; i++) {
            int teamMemberId = profile.getWormsGroup()[i];
            TeamMember teamMember = profile.getTeamMembers()[i];
            if(teamMember != null) {
                boolean result = teamMember.setHat((short) 0) | teamMember.setKit((short) 0);
                if(result) {
                    profile.setTeamMembersDirty(true);

                    WormStructure wormStructure = profile.getWormStructure(teamMemberId);
                    if(wormStructure != null) {
                        StuffHaving stuffHaving = groupService.getDefaultTeamMemberStuff(teamMemberId, teamMember);
                        if(stuffHaving != null) {
                            wormStructure.hat = stuffHaving.getHatId();
                            wormStructure.kit = stuffHaving.getKitId();
                        } else {
                            wormStructure.hat = 0;
                            wormStructure.kit = 0;
                        }
                    }
                }
            }
        }
    }

    /**
     * Снять шапку
     */
    public SimpleResultEnum deselectHat(UserProfile profile, int teamMemberId) {
        if(teamMemberId == profile.getId()) {
            profile.setHat((short) 0);
        } else {
            TeamMember teamMember = profile.getFriendTeamMember(teamMemberId);
            if(teamMember != null) {
                boolean result = teamMember.setHat((short) 0);
                if(result) {
                    profile.setTeamMembersDirty(true);

                    WormStructure wormStructure = profile.getWormStructure(teamMemberId);
                    if(wormStructure != null) {
                        wormStructure.hat = groupService.getDefaultTeamMemberHat(teamMemberId, teamMember);
                    }
                }
            }
        }
        return SUCCESS;
    }

    /**
     * Убрать снаряжение
     */
    public SimpleResultEnum deselectKit(UserProfile profile, int teamMemberId) {
        if(teamMemberId == profile.getId()) {
            profile.setKit((short) 0);
        } else {
            TeamMember teamMember = profile.getFriendTeamMember(teamMemberId);
            if(teamMember != null) {
                boolean result = teamMember.setKit((short) 0);
                if(result) {
                    profile.setTeamMembersDirty(true);

                    WormStructure wormStructure = profile.getWormStructure(teamMemberId);
                    if(wormStructure != null) {
                        wormStructure.kit = groupService.getDefaultTeamMemberKit(teamMemberId, teamMember);
                    }
                }
            }
        }
        return SUCCESS;
    }

    public boolean isExist(UserProfile profile, short stuffId) {
        return isExist(profile, stuffId, true);
    }

    public boolean isExist(UserProfile profile, short stuffId, boolean orThrow) {
        Stuff stuff = getStuff(stuffId, orThrow);
        if(stuff == null) {
            return false;
        }
        return stuff.isTemporal() ? temporalStuffService.isExist(profile, stuffId) : permanentStuffService.isExist(profile, stuffId);
    }

    public boolean isExistPermanent(UserProfile profile, short stuffId) {
        return permanentStuffService.isExist(profile, stuffId);
    }

    public boolean isExistTemporal(UserProfile profile, short stuffId) {
        return temporalStuffService.isExist(profile, stuffId);
    }

    /**
     * одеть другую шапку
     *
     * @param profile профайл игрока
     * @param hatId   id шапки
     * @return true если шапка есть в рюкзаке и смена удалась или шапка "специальная", выдаваемая всем во время миссии
     */
    public SimpleResultEnum selectHat(UserProfile profile, int teamMemberId, short hatId) {
        if(profile.getHat(teamMemberId) == hatId) {
            return SUCCESS;
        }
        Stuff hat = getHat(hatId);
        if(hat == null) {
            return ERROR;
        }
        if(hat.isSpecial()) {
            return setStuff(profile, teamMemberId, hat);
        }
        if(hat.isTemporal()) {
            if(!temporalStuffService.isExist(profile, hatId)) {
                return ERROR;
            } else {
                return setStuff(profile, teamMemberId, hat);
            }
        } else {
            if(!permanentStuffService.isExist(profile, hatId)) {
                return ERROR;
            } else {
                return setStuff(profile, teamMemberId, hat);
            }
        }
    }

    /**
     * выбрать снаряжение
     *
     * @param profile профайл игрока
     * @param kitId   id снаряжения
     * @return true если шапка есть в рюкзаке и смена удалась или шапка "специальная", выдаваемая всем во время миссии
     */
    public SimpleResultEnum selectKit(UserProfile profile, int teamMemberId, short kitId) {
        if(profile.getKit(teamMemberId) == kitId) {
            return SUCCESS;
        }
        Stuff kit = getKit(kitId);
        if(kit == null) {
            return ERROR;
        }
        if(kit.isSpecial()) {
            return setStuff(profile, teamMemberId, kit);
        }
        if(kit.isTemporal()) {
            if(!temporalStuffService.isExist(profile, kitId)) {
                return ERROR;
            } else {
                return setStuff(profile, teamMemberId, kit);
            }
        } else {
            if(!permanentStuffService.isExist(profile, kitId)) {
                return ERROR;
            } else {
                return setStuff(profile, teamMemberId, kit);
            }
        }
    }

    public void visitStuff(UserProfile profile, Consumer<Stuff> consumer) {
        for(short stuffId : profile.getStuff()) {
            Stuff stuff = getStuff(stuffId);
            if(stuff != null) {
                consumer.accept(stuff);
            }
        }
        visitTemporalStuff(profile.getTemporalStuff(), consumer);
    }

    public void visitStuff(UserProfileStructure profileStructure, Consumer<Stuff> consumer) {
        for(short stuffId : profileStructure.stuff) {
            Stuff stuff = getStuff(stuffId);
            if(stuff != null) {
                consumer.accept(stuff);
            }
        }
        visitTemporalStuff(profileStructure.temporalStuff, consumer);
    }

    public void visitTemporalStuff(byte[] temporalStuff, Consumer<Stuff> consumer) {
        if(temporalStuff != null && temporalStuff.length >= TemporalStuffService.TEMP_STUF_SIZE) {
            for(int i = 0; i < temporalStuff.length; i += TemporalStuffService.TEMP_STUF_SIZE) {
                short stuffId = TemporalStuffService.readStuffId(temporalStuff, i);
                long stuffExpireDate = TemporalStuffService.getStuffExpireDate(temporalStuff, i);
                Stuff stuff = getStuff(stuffId);
                if(stuff != null && System.currentTimeMillis() < stuffExpireDate) {
                    consumer.accept(stuff);
                }
            }
        }
    }

    public void visitTemporalStuffConsumeExpireDate(byte[] temporalStuff, Consumer<Tuple2<Stuff,Date>> consumer) {
        if(temporalStuff != null && temporalStuff.length >= TemporalStuffService.TEMP_STUF_SIZE) {
            for(int i = 0; i < temporalStuff.length; i += TemporalStuffService.TEMP_STUF_SIZE) {
                short stuffId = TemporalStuffService.readStuffId(temporalStuff, i);
                long stuffExpireDate = TemporalStuffService.getStuffExpireDate(temporalStuff, i);
                Stuff stuff = getStuff(stuffId);
                if(stuff != null && System.currentTimeMillis() < stuffExpireDate) {
                    consumer.accept(Tuple.of(stuff, new Date(stuffExpireDate)));
                }
            }
        }
    }

    public int addBooster(UserProfile profile, final short boosterId) {
        return addBooster(profile, getStuff(boosterId));
    }

    public int addBooster(UserProfile profile, final Stuff boostStuff) {
       return addBooster(profile, boostStuff,  boostStuff.getExpireTimeInSeconds());
    }

    public int addBooster(UserProfile profile, final Stuff boostStuff, int boostExpireTimeInSeconds) {
        Tuple3<Integer, Stuff, Integer> i_stuff_expireDate = temporalStuffService.findStuff(profile, stuff -> stuff.isBoost() && boostStuff.getBoostFamily() == stuff.getBoostFamily());
        int expireTimeInSeconds;
        if(i_stuff_expireDate != null) {
            Integer i = i_stuff_expireDate._1;
            int fromDate = Math.max(AppUtils.currentTimeSeconds(), readExpireDate(profile.getTemporalStuff(), i));
            expireTimeInSeconds = fromDate + boostExpireTimeInSeconds;
            updateStuffFor(profile, boostStuff.getStuffId(), expireTimeInSeconds, i);
        } else {
            expireTimeInSeconds = AppUtils.currentTimeSeconds() + boostExpireTimeInSeconds;
            addStuffFor(profile, boostStuff.getStuffId(), expireTimeInSeconds);
        }
        return expireTimeInSeconds;
    }

    public int getBoostValue(UserProfile profile, final BoostFamily boostFamily) {
        final int[] value = {0};
        visitTemporalStuff(profile.getTemporalStuff(), stuff -> {
            if(stuff.isBoost() && stuff.getBoostFamily() == boostFamily) {
                value[0] += stuff.getBoostParam();
            }
        });
        return value[0];
    }

    public Date getBoostExpireDate(UserProfile profile, final BoostFamily boostFamily) {
        final Date[] value = {null};
        visitTemporalStuffConsumeExpireDate(profile.getTemporalStuff(), stuff_expireDate -> {
            Stuff stuff = stuff_expireDate._1;
            if(stuff.isBoost() && stuff.getBoostFamily() == boostFamily) {
                value[0] = stuff_expireDate._2;
            }
        });
        return value[0];
    }

    public int getBoostValue(UserProfileStructure profileStructure, final BoostFamily boostFamily) {
        final int[] value = {0};
        visitTemporalStuff(profileStructure.temporalStuff, stuff -> {
            if(stuff.isBoost() && stuff.getBoostFamily() == boostFamily) {
                value[0] += stuff.getBoostParam();
            }
        });
        return value[0];
    }

}
