package com.pragmatix.app.services;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.messages.server.SearchTheHouseResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.gameapp.services.TaskService;
import com.pragmatix.gameapp.sessions.Sessions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.pragmatix.app.messages.server.SearchTheHouseResult.ResultEnum.*;

/**
 * Класс хранит id профайлов игроков для
 * которых уже достиг максимум по обыску домика
 */
@Service
public class SearchTheHouseService {

    public static final byte MAX_SEARCH_KEYS_BY_DAY = 5;

    private static final Set<Integer> SCORING_LEVELS = new HashSet<>(Arrays.asList(5, 10, 15, 20, 25, 30));

    private static final int MIN_SCORING_LEVEL = 4;

    // 60 дней
    protected static final long ABANDONED_TIME = TimeUnit.DAYS.toMillis(60);

    @Resource
    private DaoService daoService;

    @Resource
    private TaskService taskService;

    @Resource
    private StatisticService statisticService;


    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Resource
    private CraftService craftService;

    @Resource
    private ProfileBonusService profileBonusService;

    /**
     * Вернет количесво обысков друзей, доступных сегодня игроку
     *
     * @param profile профиль
     * @return MAX_SEARCH_KEYS_BY_DAY если зашел сегодня в первый раз
     */
    public byte getAvailableSearchKeys(UserProfile profile) {
        return dailyRegistry.getSearchKeys(profile.getId());
    }

    public SearchTheHouseResult searchTheHouse(UserProfile profile, final UserProfile friendProfile) {
        Date friendLastSearchTime = friendProfile.getLastSearchTime();
        SearchTheHouseResult.ResultEnum searchResult = getSearchResult(friendProfile, profile.getId());

        SearchTheHouseResult awardResult;

        byte searchKeys = dailyRegistry.getSearchKeys(profile.getId());

        //если друг давно не заходил ключик не тратится
        if(searchResult != ABANDONED) {
            searchKeys = (byte) (searchKeys - 1);
            dailyRegistry.setSearchKeys(profile.getId(), searchKeys);
        }

        if(searchResult == EMPTY || searchResult == ABANDONED) {
            awardResult = new SearchTheHouseResult(searchResult, 0, searchKeys);
        } else if(searchResult == RUBY_LIMIT_EXEED) {
            // читер нашел рубин, но мы его ему не даём
            // и также оставляем возможность обыскать обычному игроку
            awardResult = getNoRubyBonus(profile);
        } else {
            //сохроняем инфу о том, что данного друга мы уже обыскали
            friendProfile.setLastSearchTime(new Date());

            if(searchResult == REAL_MONEY) {
                //сохроняем в БД инфу о том, что домик обыскали если игрока нету в онлайн
                if(Sessions.get(friendProfile) == null) {
                    daoService.doInTransactionWithoutResult(() -> daoService.getUserProfileDao().setLastSearchTime(friendProfile.getId(), friendProfile.getLastSearchTime()));
                }
                int realMoney = 1;
                awardResult = new SearchTheHouseResult(REAL_MONEY, realMoney, searchKeys);
            } else if(searchResult == MONEY) {
                awardResult = getNoRubyBonus(profile);
            } else {
                // теоритически, такого быть не должно
                awardResult = new SearchTheHouseResult(ERROR, 0, searchKeys);
            }
        }

        GenericAward genericAward = null;
        if(awardResult.result == REAL_MONEY) {
            genericAward = GenericAward.builder().addRealMoney(1).build();
        } else if(awardResult.result == MONEY) {
            genericAward = GenericAward.builder().addMoney(awardResult.value).build();
        } else if(awardResult.result == REAGENT && awardResult.value >= 0) {
            genericAward = GenericAward.builder().addReagent((byte) awardResult.value, 1).build();
        }
        if(genericAward != null) {
            profileBonusService.awardProfile(genericAward, profile, AwardTypeEnum.SEARCH_THE_HOUSE,
                    "friend", friendProfile.getId(),
                    "friendLevel", friendProfile.getLevel(),
                    "friendLastSearchTime", AppUtils.formatDate(friendLastSearchTime)
            );
        }
        return awardResult;
    }

    public SearchTheHouseResult getNoRubyBonus(UserProfile profile) {
        //пытаемся выдать реагент
        byte reagentId = craftService.getSingleReagentForFriendSearch(profile.getLevel());
        if(reagentId >= 0) {
            //выпал реагент
            return new SearchTheHouseResult(REAGENT, reagentId, dailyRegistry.getSearchKeys(profile.getId()));
        } else {
            //выдаём деньги
            //вычисляем количество денег которое выдать
            int money = ThreadLocalRandom.current().nextInt(7, 20);
            return new SearchTheHouseResult(MONEY, money, dailyRegistry.getSearchKeys(profile.getId()));
        }
    }

    /**
     * предпологаемый результат обыска домика игрока
     * если = 0, то в домике пусто
     * если = 1, то давать рубины (2 рубина)
     * если = 2, то давать фузы (сервер скажет сколько, при успешном обыске)
     * если = 4, то в домике пусто, давно не заходил на сервис
     *
     * @param friendProfile профили игрока котогого обыскивают
     * @param profileId
     * @return searchResult
     */
    public SearchTheHouseResult.ResultEnum getSearchResult(UserProfile friendProfile, Long profileId) {
        // не достиг минимального уровня
        if(friendProfile.getLevel() < MIN_SCORING_LEVEL) {
            return EMPTY;
        }

        Date today = new Date();
        Date lastSearchTime = friendProfile.getLastSearchTime();
        Date lastLoginTime = friendProfile.getLastLoginTime();
        // давно не заходил на сервис - пусто
        if(lastLoginTime != null && lastLoginTime.getTime() < today.getTime() - ABANDONED_TIME) {
            return ABANDONED;
        }
        // никто не обыскивал ни разу, на зачетном уровне
        if(isRubinCandidat(friendProfile)) {
            // рубины только если тот кто обыскивает не замечен в накрутке рубинов
            return !cheatersCheckerService.isExcludedFromRubyAward(profileId) ? REAL_MONEY : RUBY_LIMIT_EXEED;
        } else if(lastSearchTime != null) {
            // уже обыскивали
            Calendar todayCal = Calendar.getInstance();
            Calendar lastSearchTimeCal = Calendar.getInstance();
            lastSearchTimeCal.setTime(lastSearchTime);
            // обыскивали но не сегодня - выдаем фузы
            if(todayCal.get(Calendar.DAY_OF_MONTH) != lastSearchTimeCal.get(Calendar.DAY_OF_MONTH)) {
                return MONEY;
            } else {
                return EMPTY;
            }
        } else {
            return MONEY;
        }
    }

    // вызывается при достижении игроком нового уровня
    public void fireLevelUp(UserProfile profile) {
        // достиг зачетного уровня
        if(SCORING_LEVELS.contains(profile.getLevel())) {
            profile.setLastSearchTime(null);
        }
    }

    // вернет истину если за обыск игрока можно заработать рубин
    public boolean isRubinCandidat(UserProfile profile) {
        return SCORING_LEVELS.contains(profile.getLevel()) && profile.getLastSearchTime() == null;
    }

}
