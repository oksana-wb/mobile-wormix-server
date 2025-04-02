package com.pragmatix.app.services;

import com.pragmatix.app.messages.server.GetFriendsForMissionResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.UserProfileByOnlineAndLevelComparator;
import com.pragmatix.app.settings.SimpleBattleSettings;
import com.pragmatix.gameapp.cache.SoftCache;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс для создания кеша списка друзей у профайла
 * <p/>
 * User: denis
 * Date: 16.04.2010
 * Time: 1:02:18
 */
@Service
public class FriendsListService {

    private static final Logger log = LoggerFactory.getLogger(FriendsListService.class);

    @Resource
    private SoftCache softCache;

    @Resource
    private ProfileService profileService;

    @Resource
    private UserRegistryI userRegistry;

    @Resource
    private BattleService battleService;

    private static final int firstPageSize = 9;

    private static final int nonFirstPageSize = 27;

    @Value("${friendsList.maxOnlineFriends:1000}")
    private int maxOnlineFriends = 1000;

    @Value("${friendsList.maxFriends:297}")
    private int maxFriends = 297;

    @Value("#{battleAwardSettings.awardSettingsMap}")
    private Map<Short, SimpleBattleSettings> awardSettingsMap;

    /**
     * инициализирует профайл игрока айдишниками друзей
     * и загружает первую страницу списка дрзей
     *
     * @param profile профайл игрока
     * @param ids     список айдишников игроков
     * @return первая страница списка друзей
     */
    public FirstPageBean getFirstPage(UserProfile profile, List<Long> ids) {
        //ограничеваем список для поиска онлайн друзей
        List<Long> idsList = ids.size() > maxOnlineFriends ? ids.subList(0, maxOnlineFriends) : ids;

        // грузим тех кто online и помещаем их в начало
        Set<UserProfile> friendsOnline = new TreeSet<>(new UserProfileByOnlineAndLevelComparator());
        List<Long> notOrderedFriends = new ArrayList<>();
        loadOnlineFromCache(idsList, friendsOnline, notOrderedFriends);

        List<Integer> friendsList = friendsOnline.stream().map(userProfile -> userProfile.getId().intValue()).collect(Collectors.toList());
        short onlineFriendsCount = (short) friendsList.size();

        // сортируем оставшихся друзей используя глобальную мапу уровней
        Set<FriendUser> orderedFriends = new TreeSet<>();
        for(Long friendId : notOrderedFriends) {
            orderedFriends.add(new FriendUser(friendId, (byte) userRegistry.getProfileLevel(friendId)));
        }

        //оставляем только часть если список друзей ограничен
        int index = 0;
        for(FriendUser friendUser : orderedFriends) {
            //оставляем только часть если список друзей ограничен
            if(index < maxFriends) {
                index++;
                friendsList.add((int) friendUser.id);
            } else {
                break;
            }
        }

        profile.setOrderedFriends(friendsList.stream().mapToInt(i -> i).toArray());

        return new FirstPageBean(getPage(profile, 1), onlineFriendsCount, (short) profile.getOrderedFriends().length);
    }

    private static class FriendUser implements Comparable<FriendUser> {
        long id;
        byte level;

        private FriendUser(long id, byte level) {
            this.id = id;
            this.level = level;
        }

        @Override
        public int compareTo(FriendUser o) {
            if(this.level > o.level) {
                return -1;
            } else if(this.level < o.level) {
                return 1;
            } else if(this.id > o.id) {
                return -1;
            } else if(this.id < o.id) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public static class FirstPageBean {
        private Collection<UserProfile> firstPage;
        private short onlineFriends;
        private short friends;

        public FirstPageBean(Collection<UserProfile> firstPage, short onlineFriends, short friends) {
            this.firstPage = firstPage;
            this.onlineFriends = onlineFriends;
            this.friends = friends;
        }

        public Collection<UserProfile> getFirstPage() {
            return firstPage;
        }

        public short getOnlineFriends() {
            return onlineFriends;
        }

        public short getFriends() {
            return friends;
        }
    }

    /**
     * вернуть нужную страницу друзей. начинаются с 1
     * страницы должны браться по очереди
     *
     * @param userProfile профиль
     * @param page        номер страницы начинаются с 1
     * @return список друзей на странице
     */
    public Collection<UserProfile> getPage(UserProfile userProfile, int page) {
        if(page < 1) {
            page = 1;
        }

        int pageSize = firstPageSize;
        int firstIndex = 0;

        if(page > 1) {
            pageSize = nonFirstPageSize;
            firstIndex = firstPageSize + (page - 2) * nonFirstPageSize;
        }

        List<UserProfile> result = new ArrayList<>(pageSize);

        int allFriendsCount = userProfile.getOrderedFriends().length;
        int lastIndex = Math.min(firstIndex + pageSize, allFriendsCount);

        if(firstIndex >= allFriendsCount) {
            // возвращаем пустой набор
            return result;
        }

        try {
            int[] orderedFriendOnPageArr = ArrayUtils.subarray(userProfile.getOrderedFriends(), firstIndex, lastIndex);
            List<Long> orderedFriendOnPage = new ArrayList<>(orderedFriendOnPageArr.length);
            for(int profileId : orderedFriendOnPageArr) {
                orderedFriendOnPage.add((long) profileId);
            }
            Map<Long, UserProfile> friendProfileMap = profileService.loadProfilesInMap(orderedFriendOnPage, true);
            // добавляем профайлы в результат согласно отсортированному списку id друзей
            for(Long friendId : orderedFriendOnPage) {
                UserProfile profile = friendProfileMap.get(friendId);
                if(profile == null) {
                    if(log.isDebugEnabled()) {
                        log.debug("Can't load profile by id {}", friendId);
                    }
                    profile = new UserProfile(friendId);
                    userProfile.setProfileStringId(profileService.getProfileStringId(userProfile.getId()));
                }
                result.add(profile);
            }
        } catch (Exception e) {
            log.error("FriendsListService.getPage page = " + page, e);
        }

        return result;
    }

    /**
     * Загружаем из кеша тех кто онлайн
     *
     * @param ids               список id (ограниченный пороговым значением)
     * @param online            кладем сюда тех кто онлайн
     * @param notOrderedFriends сюда тех кокго будем сортировать и грузить
     */
    private void loadOnlineFromCache(List<Long> ids, Collection<UserProfile> online, List<Long> notOrderedFriends) {
        for(Long id : ids) {
            UserProfile profile = softCache.get(UserProfile.class, id, false);
            if(profile != null && profile.isOnline()) {
                online.add(profile);
            } else {
                notOrderedFriends.add(id);
            }
        }
    }

    /**
     * вернет список профилей и соотв. статусов которым можно отправить (в случае статуса SUCCESS) приглашение на командный бой против босса
     */
    public Pair<List<UserProfile>, List<GetFriendsForMissionResult.FriendState>> getFriendsForMission(UserProfile profile, short newMissionId) {
        List<UserProfile> friends = new ArrayList<>();
        List<GetFriendsForMissionResult.FriendState> states = new ArrayList<>();

        SimpleBattleSettings battleSettings = awardSettingsMap.get(newMissionId);
        if(battleSettings == null) {
            log.error("AwardSettings not found for missionId={} in 2xE battle", newMissionId);
            return new ImmutablePair<>(friends, states);
        }

        for(int friendId : profile.getOrderedFriends()) {
            UserProfile friendProfile = profileService.getUserProfile((long)friendId, false);
            if(friendProfile == null || !friendProfile.isOnline()) {
                continue;
            }

            GetFriendsForMissionResult.FriendState friendState = GetFriendsForMissionResult.FriendState.SUCCESS;

            // начисляем нужное количество битв если пришло время
            battleService.checkBattleCount(friendProfile);

            //проверяем можно ли отправить игроку приглашение в командный бой или нет
            if(friendProfile.getBattlesCount() == 0) {
                friendState = GetFriendsForMissionResult.FriendState.EXCEED_BATTLES;
            } else if(!battleService.validateNewMission(friendProfile, newMissionId, battleSettings)) {
                friendState = GetFriendsForMissionResult.FriendState.MISSION_LOCKED;
            }

            friends.add(friendProfile);
            states.add(friendState);
        }

        return new ImmutablePair<>(friends, states);
    }

    /**
     * вернет список профилей и соотв. статусов которым можно отправить (в случае статуса SUCCESS) приглашение на командный бой против босса
     */
    public Pair<List<UserProfile>, List<GetFriendsForMissionResult.FriendState>> getFriendsForSuperBossMission(UserProfile profile, short[] missionIds) {
        List<UserProfile> friends = new ArrayList<>();
        List<GetFriendsForMissionResult.FriendState> states = new ArrayList<>();

        short missionId1 = missionIds[0];
        SimpleBattleSettings battleSettings1 = awardSettingsMap.get(missionId1);
        if(battleSettings1 == null) {
            log.error("AwardSettings not found for missionId={} in 2xE heroic battle", missionId1);
            return new ImmutablePair<>(friends, states);
        }

        short missionId2 = missionIds[1];
        SimpleBattleSettings battleSettings2 = awardSettingsMap.get(missionId2);
        if(battleSettings2 == null) {
            log.error("AwardSettings not found for missionId={} in 2xE heroic battle", missionId2);
            return new ImmutablePair<>(friends, states);
        }

        for(int friendId : profile.getOrderedFriends()) {
            UserProfile friendProfile = profileService.getUserProfile((long)friendId, false);
            if(friendProfile == null || !friendProfile.isOnline()) {
                continue;
            }

            GetFriendsForMissionResult.FriendState friendState = GetFriendsForMissionResult.FriendState.SUCCESS;

            // начисляем нужное количество битв если пришло время
            battleService.checkBattleCount(friendProfile);

            //проверяем можно ли отправить игроку приглашение в командный бой или нет
            if(friendProfile.getBattlesCount() == 0) {
                friendState = GetFriendsForMissionResult.FriendState.EXCEED_BATTLES;
            } else if(!battleService.validateSuperBossMission(friendProfile, missionId1, battleSettings1) || !battleService.validateSuperBossMission(friendProfile, missionId2, battleSettings2)) {
                friendState = GetFriendsForMissionResult.FriendState.MISSION_LOCKED;
            }

            friends.add(friendProfile);
            states.add(friendState);

        }

        return new ImmutablePair<>(friends, states);
    }

}
