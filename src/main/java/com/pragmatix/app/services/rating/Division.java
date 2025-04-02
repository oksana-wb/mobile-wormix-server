package com.pragmatix.app.services.rating;

import com.pragmatix.clan.structures.ClanMemberStructure;
import com.pragmatix.app.messages.structures.RatingProfileStructure;
import com.pragmatix.app.model.UserProfile;
import io.vavr.Tuple2;
import io.vavr.Tuple3;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 18.05.11 17:39
 */
public class Division implements Serializable {

    private static final long serialVersionUID = 5671732181958457215L;

    private int division;
    /**
     * топ 30 игроков
     */
    private final Map<Long, RatingProfileStructure> topPlayers = new ConcurrentHashMap<>();

    /**
     * топ 30 игроков упорядоченых по рейтингу в порядке убывания
     */
    private transient volatile NavigableSet<RatingProfileStructure> profileNavigableSet;

    public final int maxTop;

    private final Comparator<RatingProfileStructure> ratingComparator;

    private final BiFunction<UserProfile, RatingProfileStructure, Boolean> needAddFunc;

    Division(int division, int maxTop) {
        this.division = division;
        this.maxTop = maxTop;
        this.ratingComparator =  new UserProfileByRatingComparator();
        this.needAddFunc = (profile, lastProfile) -> lastProfile.rating < profile.getRating();
    }

    Division(int division, int maxTop, Comparator<RatingProfileStructure> ratingComparator, BiFunction<UserProfile, RatingProfileStructure, Boolean> needAddFunc) {
        this.division = division;
        this.maxTop = maxTop;
        this.ratingComparator = ratingComparator;
        this.needAddFunc = needAddFunc;
    }

    /**
     * перестраивает топ игроков в одну операцию
     * необходимо если у игрока в топе сменился рейтинг либо в рейтинг попал новый игрок
     */
    public void reinit() {
        NavigableSet<RatingProfileStructure> navigableSet = new ConcurrentSkipListSet<>(ratingComparator);
        navigableSet.addAll(topPlayers.values());
        profileNavigableSet = navigableSet;
    }

    public void init(Collection<RatingProfileStructure> topPlayers) {
        this.topPlayers.clear();
        for(RatingProfileStructure topPlayer : topPlayers) {
           this.topPlayers.put(topPlayer.id, topPlayer);
        }
        reinit();
    }

    public void addUser(UserProfile profile, Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank_skin) {
        topPlayers.put(profile.getId(), new RatingProfileStructure(profile, clanMember_rank_skin));
    }

    public void addUser(RatingProfileStructure ratingProfileStructure) {
        topPlayers.put(ratingProfileStructure.id, ratingProfileStructure);
    }

    public void removeUser(UserProfile profile) {
        removeUser(profile.getId());
    }

    public void removeUser(Long profileId) {
        RatingProfileStructure wasHere = topPlayers.remove(profileId);
        if (wasHere != null) {
            reinit();
        }
    }

    public boolean checkAndAddUser(UserProfile profile, Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank_skin) {
        if(topPlayers.size() < maxTop) {
            // топ рейтинга заполен не полностью
            addUser(profile, clanMember_rank_skin);
            reinit();
            return true;
        }else{
            RatingProfileStructure lastProfile = profileNavigableSet.last();
            // проверяем нужно ли добавлять
            if(needAddFunc.apply(profile, lastProfile)) {
                // усли топ заполнен полностью, удаляем последний элемент (элменты, если по каким то причинам топ заполнен с перебором)
                while (topPlayers.size() > maxTop - 1) {
                    topPlayers.remove(lastProfile.id);
                    reinit();
                    lastProfile = profileNavigableSet.last();
                }
                // добавляем
                addUser(profile, clanMember_rank_skin);
                reinit();
                return true;
            } else {
                // не достаточен рейтинг для в включения в топ
                return false;
            }
        }
    }

    /**
     * Метод используется при заполнении топ дивизиона во время старта сервера
     *
     * @param ratingProfileStructure Кандитат на добавление в топ
     * @return добавлен ли в топ
     */
    public boolean checkAndAddUser(RatingProfileStructure ratingProfileStructure) {
        if(topPlayers.size() == 0) {
            // топ рейтинга для дивизиона пуст
            addUser(ratingProfileStructure);
            reinit();
            return true;
        } else if(topPlayers.size() < maxTop) {
            // топ рейтинга заполен не полностью
            addUser(ratingProfileStructure);
            profileNavigableSet.add(ratingProfileStructure);
            return true;
        } else {
            // топ заполнен полностью
            RatingProfileStructure lastProfile = profileNavigableSet.last();
            // проверяем нужно ли добавлять
            if(lastProfile.rating < ratingProfileStructure.rating) {
                // удаляем последний элемент
                topPlayers.remove(lastProfile.id);
                profileNavigableSet.remove(lastProfile);
                // добавляем
                addUser(ratingProfileStructure);
                profileNavigableSet.add(ratingProfileStructure);
                return true;
            } else {
                // не достаточен рейтинг для в вкючения в топ
                return false;
            }
        }
    }

    /**
     * вернет список рейтинга для игрока
     *
     * @return список рейтинга для игрока
     */
    public List<RatingProfileStructure> getRatingList() {
        return new ArrayList<>(profileNavigableSet);
    }

    public void onChangeRating(UserProfile profile, boolean maybeAdd, Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank_skin) {
        if(!contains(profile.getId())) {
            if(maybeAdd) {
                checkAndAddUser(profile, clanMember_rank_skin);
            }
        } else {
            updateRating(profile, clanMember_rank_skin);
        }
    }

    public boolean contains(Long profileId) {
        return getTopPlayers().get(profileId) != null;
    }

    private RatingProfileStructure getRatingProfileStructure(UserProfile profile) {
        return getTopPlayers().get(profile.getId());
    }

    private void updateRating(UserProfile profile, Callable<Tuple3<ClanMemberStructure, Byte, Byte>> clanMember_rank_skin) {
        RatingProfileStructure ratingProfileStructure = getRatingProfileStructure(profile);
        if(ratingProfileStructure != null) {
            ratingProfileStructure.init(profile, clanMember_rank_skin);
            reinit();
        }
    }

    public void consume(UserProfile profile, BiConsumer<RatingProfileStructure, UserProfile> consumer){
        RatingProfileStructure ratingProfileStructure = topPlayers.get(profile.getId());
        if(ratingProfileStructure != null) {
            consumer.accept(ratingProfileStructure, profile);
        }
    }

    @Override
    public String toString() {
        return "Division{" +
                "division=" + division +
                '}';
    }

    //====================== Getters and Setters =================================================================================================================================================

    public Map<Long, RatingProfileStructure> getTopPlayers() {
        return topPlayers;
    }

    public int getDivision() {
        return division;
    }

    public NavigableSet<RatingProfileStructure> getProfileNavigableSet() {
        return profileNavigableSet;
    }
}
