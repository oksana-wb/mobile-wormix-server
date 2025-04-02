package com.pragmatix.app.services;

import com.pragmatix.app.common.BanType;
import com.pragmatix.app.domain.BanEntity;
import com.pragmatix.app.model.BanItem;
import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.gameapp.services.TaskService;
import com.pragmatix.gameapp.social.SocialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Сервис ля бана игроков
 * User: denis
 * Date: 28.10.2010
 * Time: 16:38:11
 */
@Service
public class BanService {

    public static final String SERVER_NAME = "server";

    /**
     * список игроков которые находяться в бане
     */
    private Map<Long, BanItem> banList = new ConcurrentHashMap<Long, BanItem>();

    @Resource
    private TaskService taskService;

    @Resource
    private DaoService daoService;

    @Resource
    private RatingService ratingService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Logger paymentLog = SocialService.PAYMENT_LOGGER;

    public void init(Map<Long, BanItem> banList) {
        this.banList.putAll(banList);
    }

    /**
     * забанить игрока
     *
     * @param profileId
     * @param type      - причина бана
     */
    public void addToBanList(Long profileId, final BanType type) {
        addToBanList(profileId, type, type.toString());
    }

    public void addToBanList(Long profileId, final BanType type, final String note) {
        addToBanList(profileId, type.getType(), type.getDurationInDays(), note);
    }

    public void addToBanList(Long profileId, int cheatReason, Integer durationInDays, String note) {
        addToBanList(profileId, cheatReason, durationInDays, note, SERVER_NAME, "");
    }

    public void addToBanList(Long profileId, int cheatReason, int durationInDays, String note, String admin, String attachments) {
        // если игрок уже находится в бане
        BanItem ban = get(profileId);
        if(ban != null) {
            log.warn("user already banned! {}", ban);
            return;
        }

        final Runnable insertTask = addToBanListReturnTask(profileId, cheatReason, durationInDays, note, admin, attachments);

        TransactionCallback transactionTask = status -> {
            insertTask.run();
            return null;
        };
        taskService.addTransactionTask(transactionTask);
    }


    public Runnable addToBanListReturnTask(Long profileId, int cheatReason, Integer durationInDays, String note, String admin, String attachments) {
        Long startDate = new Date().getTime();
        Long endDate;
        if(durationInDays == null || durationInDays == 0) {
            endDate = null;
        } else {
            endDate = startDate + (durationInDays > 0 ? TimeUnit.DAYS.toMillis(durationInDays) : 0);
        }
        final BanItem banItem = new BanItem(profileId, startDate, cheatReason, endDate, note, admin, attachments);
        banList.put(profileId, banItem);

        ratingService.onBan(profileId);

        return () -> {
            BanEntity banEntity = new BanEntity(banItem);
            daoService.getBanDao().insert(banEntity);
            banItem.setId(banEntity.getId());
        };
    }

    public void changeBanDuration(final BanItem ban, Long profileId, Integer durationInDays, String note, String admin) {
        final Runnable updateTask = changeBanDurationReturnTask(ban, profileId, durationInDays, note, admin);

        TransactionCallback transactionTask = status -> {
            updateTask.run();
            return null;
        };
        taskService.addTransactionTask(transactionTask);
    }

    public void updateBanEntity(final BanItem ban) {
        if(ban.getId() > 0) {
            TransactionCallback transactionTask = status -> {
                daoService.getBanDao().update(new BanEntity(ban));
                return null;
            };
            taskService.addTransactionTask(transactionTask);
        }
    }

    /**
     * метод разбанит игрока, и вернет задачу которую нужно будет выполнить в транзакции
     */
    public Runnable changeBanDurationReturnTask(final BanItem ban, Long profileId, Integer durationInDays, String note, String admin) {
        String action = "Бан снят";
        if(durationInDays <= 0) {
            // снимаем бан однозначно
            ban.setEndDate(System.currentTimeMillis());
            banList.remove(profileId);
        } else {
            // пробыл в бане
            long wasBanned = System.currentTimeMillis() - ban.getStartDate();
            // новый срок
            long newBanPeriod = TimeUnit.DAYS.toMillis(durationInDays);
            // осталость просидеть в бане
            long leftBan = newBanPeriod - wasBanned;
            if(leftBan <= 0) {
                // отсидел уже, так сказать
                // снимаем бан однозначно
                ban.setEndDate(System.currentTimeMillis());
                banList.remove(profileId);
            } else {
                // осталось немного доситеть
                ban.setEndDate(System.currentTimeMillis() + leftBan);
                action = "Бан изменен";
            }
        }

        // дописываем комментарий
        String newNote = String.format("%s%s (%s), причина - %s", (ban.getNote() == null ? "" : ban.getNote() + "; "), action, admin, note);
        ban.setNote(newNote);

        return new Runnable() {
            @Override
            public void run() {
                daoService.getBanDao().update(new BanEntity(ban));
            }
        };
    }

    public boolean isBanned(Long profileId) {
        return get(profileId) != null;
    }

    /**
     * содержиться ли id игрока в бане
     */
    public BanItem get(Long profileId) {
        if(profileId == null)
            return null;
        BanItem banItem = banList.get(profileId);
        if(banItem != null) {
            //если бан бесконечный или время действия не истекло
            if(banItem.getEndDate() == null || banItem.getEndDate() > System.currentTimeMillis()) {
                return banItem;
                //иначе удаляем из банлиста
            } else {
                banList.remove(profileId);
            }
        }
        return null;
    }

    /**
     * удалить игрока из бан листа
     *
     * @param profileId
     */
    public void remove(final Long profileId) {
        banList.remove(profileId);
        TransactionCallback transactionTask = new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                daoService.getBanDao().deleteFromBanList(profileId);
                return null;
            }
        };
        taskService.addTransactionTask(transactionTask);
    }

    public void banForever(Long profileId) {
        BanItem ban = get(profileId);
        if(ban != null) {
            if(ban.getEndDate() != null) {
                // сначала выполняем разбан, потом баним
                changeBanDuration(ban, profileId, 0, "перебан", "server");
                addToBanList(profileId, BanType.BAN_FOR_CANCEL_PURCHASE);
                log.info("{} <- уже имеет бан, баним теперь навсегда", profileId);
            } else {
                paymentLog.info("{} <- уже имеет вечный бан", profileId);
            }
        } else {
            addToBanList(profileId, BanType.BAN_FOR_CANCEL_PURCHASE);
            paymentLog.info("{} <- баним навсегда", profileId);
        }
    }

    public Map<Long, BanItem> getBanList() {
        return banList;
    }
}
