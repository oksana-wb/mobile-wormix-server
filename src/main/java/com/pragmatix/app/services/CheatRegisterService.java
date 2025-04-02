package com.pragmatix.app.services;

import com.pragmatix.app.dao.CheaterStatisticDao;
import com.pragmatix.app.domain.CheaterStatisticEntity;
import com.pragmatix.app.domain.CheaterStatisticItem;
import com.pragmatix.gameapp.common.TypeableEnum;
import com.pragmatix.gameapp.services.DailyTaskAvailable;
import com.pragmatix.gameapp.services.TaskService;
import com.pragmatix.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис сбора статистики по действиям читеров
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.05.12 17:19
 */
@Service
public class CheatRegisterService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private volatile Map<Long, Set<CheaterStatisticItem>> cheatStatMap = new ConcurrentHashMap<Long, Set<CheaterStatisticItem>>();

    @Resource
    private DaoService daoService;

    @Resource
    private TaskService taskService;

    @Resource
    private TransactionTemplate transactionTemplate;

    private boolean initialized = false;

    public void init(){
        initialized = true;
    }

    public CheaterStatisticItem cheaterDetected(Long profileId, TypeableEnum actionType, String actionParam, String note) {
        CheaterStatisticItem item = getCheatFor(profileId, actionType, actionParam);
        if(item != null) {
            item.incCount();
        } else {
            item = registrateCheat(profileId, actionType, actionParam, note);
        }
        return item;
    }

    public CheaterStatisticItem getCheatFor(Long profileId, TypeableEnum actionType, String actionParam) {
        Set<CheaterStatisticItem> items = cheatStatMap.get(profileId);
        if(items == null) {
            return null;
        }
        for(CheaterStatisticItem item : items) {
            if(item.getActionType().getType() == actionType.getType() && item.getActionParam().equals(actionParam)) {
                return item;
            }
        }
        return null;
    }

    private CheaterStatisticItem registrateCheat(final Long profileId, final TypeableEnum actionType, final String actionParam, final String note) {
        final int count = 1;
        final CheaterStatisticItem item = new CheaterStatisticItem(actionType, actionParam, count, note);
        Set<CheaterStatisticItem> items = cheatStatMap.get(profileId);
        if(items == null) {
            items = new HashSet<CheaterStatisticItem>();
            cheatStatMap.put(profileId, items);
        }
        items.add(item);
        taskService.addTransactionTask(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                CheaterStatisticEntity entity = new CheaterStatisticEntity(profileId, (short) actionType.getType(), actionParam, count, note);
                daoService.getCheaterStatisticDao().insert(entity);
                item.setId(entity.getId());
            }
        });
        return item;
    }

    /**
     * сбрасываем накопленную статистику по читерам в базу
     * задача которую нужно выполнять ежедневно
     */
    public void persistStatictic() {
        if(cheatStatMap.size() > 0) {
            Server.sysLog.info("persist collected cheater's statictic [count: {}] ...", cheatStatMap.size());

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    CheaterStatisticDao dao = daoService.getCheaterStatisticDao();
                    for(Set<CheaterStatisticItem> items : cheatStatMap.values()) {
                        for(CheaterStatisticItem item : items) {
                            if(item.getId() > 0) {
                                dao.updateCheaterStatistic(item.getId(), item.getCount(), item.getNote());
                            } else {
                                log.error("failure update CheaterStatistic id not set in {}", item);
                            }
                        }
                    }
                }
            });
            Server.sysLog.info("done.");
        }
    }

    /**
     * сбрасываем накопленную статистику по читерам в базу
     */
    public void runDailyTask() {
        if(initialized) {
            final Map<Long, Set<CheaterStatisticItem>> cheatStatMap = CheatRegisterService.this.cheatStatMap;
            CheatRegisterService.this.cheatStatMap = new ConcurrentHashMap<Long, Set<CheaterStatisticItem>>();

            taskService.addTransactionTask(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    Server.sysLog.info("persist collected cheater's statictic [count: {}] ...", cheatStatMap.size());

                    CheaterStatisticDao dao = daoService.getCheaterStatisticDao();
                    for(Set<CheaterStatisticItem> items : cheatStatMap.values()) {
                        for(CheaterStatisticItem item : items) {
                            if(item.getId() > 0) {
                                dao.updateCheaterStatistic(item.getId(), item.getCount(), item.getNote());
                            } else {
                                log.error("failure update CheaterStatistic id not set in {}", item);
                            }
                        }
                    }
                }
            });
        }
    }

}
