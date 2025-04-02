package com.pragmatix.app.services;

import com.pragmatix.app.dao.CallbackFriendDao;
import com.pragmatix.app.dao.UserProfileDao;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.services.ProfileEventsService.ProfileEventEnum;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.services.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.09.12 13:10
 */
@Component
public class CallbackFriendService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final int firstPageSize = 10;
    private static final int nonFirstPageSize = 30;

    @Resource
    private UserRegistry userRegistry;

    @Resource
    private CallbackFriendDao callbackFriendDao;

    @Value("${debug.allFriendsIsAbddonded:false}")
    private boolean debugAllFriendsIsAbddonded = false;

    @Resource
    private SoftCache softCache;

    @Resource
    private UserProfileDao userProfileDao;

    @Resource
    private TaskService taskService;

    @Resource
    private ProfileEventsService profileEventsService;

    public List<Long> filterFriends(UserProfile profile) {
        List<Long> result = new ArrayList<Long>();
        for(int friendId : profile.getOrderedFriends()) {
            if(userRegistry.isProfileAbandonded((long) friendId) || debugAllFriendsIsAbddonded) {
                result.add((long) friendId);
            }
        }
        return result;
    }

    public List<Long> getPage(int page, List<Long> allAbandondedFriends) {
        if(page < 1) {
            page = 1;
        }

        int pageSize = firstPageSize;
        int firstIndex = 0;

        if(page > 1) {
            pageSize = nonFirstPageSize;
            firstIndex = firstPageSize + (page - 2) * nonFirstPageSize;
        }

        int allAbandondedFriendsCount = allAbandondedFriends.size();
        int lastIndex = Math.min(firstIndex + pageSize, allAbandondedFriendsCount);

        if(firstIndex >= allAbandondedFriendsCount) {
            return new ArrayList<Long>();
        }

        return allAbandondedFriends.subList(firstIndex, lastIndex);

    }

    /**
     * позвать друга вернуться в игру
     *
     * @param friendId кого позвать
     * @param profile  кто зовет
     * @return SimpleResultEnum.SUCCESS, если друг был позван, а SimpleResultEnum.ERROR - если этот профиль пока нельзя позвать
     */
    public SimpleResultEnum callbackFriend(final long friendId, final UserProfile profile) {
        if(debugAllFriendsIsAbddonded || userRegistry.isProfileAbandonded(friendId)) {
            if(profile.getLastCallbackedFriendId() != friendId) {
                if(callbackFriendDao.callbackFriend(friendId, profile.getId())) {
                    profile.setLastCallbackedFriendId(friendId);
                    return SimpleResultEnum.SUCCESS;
                }
            } else {
                log.warn("флуд: повторно зовут друга {}", friendId);
            }
        } else {
            log.warn("попытка позвать друга [{}] обратно, который уже вернулся в игру", friendId);
        }
        return SimpleResultEnum.ERROR;
    }

    /**
     * игрок перешел по ссылке с предложением вернуться
     *
     * @param profile  кто вернулся перейдя по ссылке
     * @param friendId чей id был указан в ссылке
     */
    public SimpleResultEnum rewardCallbacker(final UserProfile profile, final long friendId) {
        profile.setNeedRewardCallbackers(false);
        boolean callbackExists = callbackFriendDao.isCallbackExists(profile.getId(), friendId);
        if(!callbackExists) {
            return SimpleResultEnum.ERROR;
        }
        // помечаем, что после выдачи кому-либо награды с этого профиля, с него уже нельзя получить её снова до истечения срока - нельзя снова позвать (http://jira.pragmatix-corp.com/browse/WORMIX-4273)
        userRegistry.setAbandondedFlag(profile, false);
        Date lastBeingComebackedTime = new Date();
        // ищем только в кеше, из базы не грузим
        final UserProfile friendProfile = softCache.get(UserProfile.class, friendId, false);
        profileEventsService.fireProfileEventAsync(ProfileEventEnum.EXTRA, profile,
                Param.eventType, "rewardCallbacker",
                Param.friendId, friendId
        );
        if(friendProfile != null) {
            //  нашли в кеше, увеличиваем счетчик возвращенных друзей
            friendProfile.incComebackedFriends();

            if(friendProfile.isOnline()) {
                // если в онлайне, удаляем только предложения
                taskService.addTransactionTask(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        finishComeback(profile, lastBeingComebackedTime);
                    }
                });
            } else {
                // если в оффлайне, сетим счетчик в базу и удаляем предложения
                taskService.addTransactionTask(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        userProfileDao.setComebackedFriends(friendProfile.getComebackedFriends(), friendId);
                        finishComeback(profile, lastBeingComebackedTime);
                    }
                });
            }
        } else {
            // в кеше не нашли - увеличиваем в базе счетчик возвращенных друзей и удаляем предложения
            taskService.addTransactionTask(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    userProfileDao.incComebackedFriends(friendId);
                    finishComeback(profile, lastBeingComebackedTime);
                }
            });
        }
        return SimpleResultEnum.SUCCESS;
    }

    /**
     * Помечает в базе, что этот игрок уже вернулся по приглашению, очищая все приглашения и сохраняя время
     *
     * Метод должен вызываться внутри транзакции
     * @param profile профиль игрока, который вернулся
     * @param lastBeingComebackedTime время возвращения
     */
    private void finishComeback(UserProfile profile, Date lastBeingComebackedTime) {
        callbackFriendDao.deleteCallbacksForProfile(profile.getId());
        userProfileDao.setLastBeingComebackedTime(profile.getId(), lastBeingComebackedTime);
    }

}
