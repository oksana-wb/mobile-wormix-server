package com.pragmatix.app.services;

import com.pragmatix.app.dao.DepositDao;
import com.pragmatix.app.domain.DepositEntity;
import com.pragmatix.app.messages.structures.DepositStructure;
import com.pragmatix.app.model.AnyMoneyAddition;
import com.pragmatix.app.model.DepositBean;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.server.Server;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.constraints.Null;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 19.04.2016 9:31
 *         <p>
 *         Сервис создания, обработки и выплаты депозитов в рубинах (http://jira.pragmatix-corp.com/browse/WM-4992)
 */
public class DepositService {

    private final Logger log = SocialService.PAYMENT_LOGGER;

    @Resource
    private DepositDao depositDao;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private ProfileEventsService profileEventsService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ProfileService profileService;

    private Map<String, DepositBean> depositConf;

    private int MIN_DIVIDEND_LENGTH = 1;

    @Value("#{depositsConf}")
    public void setDepositConf(List<DepositBean> depositConf) {
        Map<String, DepositBean> depositConfMap = new HashMap<>();
        for(DepositBean depositBean : depositConf) {
            depositConfMap.put(depositBean.getId(), depositBean);
        }
        this.depositConf = depositConfMap;
    }

    @PostConstruct
    public void init() {
        for(DepositBean depositBean : depositConf.values()) {
            int[] dividendsByDays = dividendsFromStr(depositBean.getDividendsByDaysStr());
            depositBean.setDividendsByDays(dividendsByDays);

            // Validate:
            if(dividendsByDays == null || dividendsByDays.length < MIN_DIVIDEND_LENGTH || depositBean.getTotalValue() <= 0) {
                Server.sysLog.error("Failed to initialize deposit bean {}: incorrect dividends by days: '{}'", depositBean, depositBean.getDividendsByDaysStr());
                throw new IllegalArgumentException("Wrong depositBean.dividendsByDaysStr: '" + depositBean.getDividendsByDaysStr() + "' for deposit " + depositBean.getId());
            }
            if(depositBean.getMoneyType() == null) {
                Server.sysLog.error("Failed to initialize deposit bean {}: money type not set up", depositBean);
                throw new IllegalArgumentException("Wrong depositBean.moneyType: " + depositBean.getMoneyType() + " for deposit " + depositBean.getId());
            }
        }
    }

    /**
     * Получить конфигурацию депозита по id типа
     *
     * @param id id из price-beans.xml
     * @return конфигурацию соответствующего бина (или null если передан некорректный id)
     */
    @Null
    public DepositBean getDepositBean(String id) {
        return depositConf.get(id);
    }

    /**
     * Получить список открытых данным игроком депозитов, которые ещё не были полностью выплачены
     * <p>
     * При первом вызове считывает из базы данных, впоследствии берёт из {@link UserProfile#deposits}
     *
     * @param profile профиль
     * @return массив из записей
     */
    @NotNull
    public DepositEntity[] getDepositsFor(UserProfile profile) {
        if(profile.getDeposits() == null) {
            DepositEntity[] deposits = depositDao.getAllOpenForProfile(profile.getId().intValue());
            profile.setDeposits(deposits);
            return deposits;
        } else {
            return profile.getDeposits();
        }
    }

    /**
     * Достаёт из базы список _всех_ депозитов игрока, в том числе прошлых и погашенных
     * <p>
     * В отличие от {@link #getDepositsFor(UserProfile)}, не кэширует результат в профиле
     *
     * @param profile профиль
     * @return массив из записей
     */
    @NotNull
    public DepositEntity[] getAllDepositsFor(UserProfile profile) {
        return depositDao.getAllForProfile(profile.getId().intValue());
    }

    /**
     * Получить список структур для отправки на клиент по открытым данным игроком депозитам, которые ещё не были полностью выплачены
     *
     * @param profile профиль игрока
     * @param now     момент времени "сейчас", для которого происходит расчёт (чтобы был неизменным за время расчёта)
     * @return массив структур для отправки на клиент
     */
    @NotNull
    public DepositStructure[] getDepositStructuresFor(UserProfile profile, Date now) {
        DepositEntity[] deposits = getDepositsFor(profile);
        final boolean todayPaid = dailyRegistry.isDividendPaid(profile.getId());

        ArrayList<DepositStructure> depositStructures = new ArrayList<>(deposits.length);
        for(DepositEntity deposit : deposits) {
            final int[] dividendsByDays = dividendsFromStr(deposit.getDividendsByDays());
            DepositStructure depositStructure = new DepositStructure(
                    deposit.getMoneyType(),
                    dividendsByDays,
                    deposit.getStartDate(),
                    deposit.getProgress());

            if(todayPaid || isToday(deposit.getLastPayDate(), now)) {
                depositStructure.todayDividend = 0;
            } else {
                depositStructure.todayDividend = dividendsByDays[deposit.getProgress()];
            }
            depositStructures.add(depositStructure);
        }
        return depositStructures.toArray(new DepositStructure[depositStructures.size()]);
    }

    /**
     * Открывает новый депозит у игрока
     *
     * @param depositBean  вид депозита
     * @param newDepositId id, который будет ему присвоен (совпадает с id платежа)
     * @param startDate    дата открытия
     * @param profile      профиль игрока
     * @return созданный вклад
     */
    public DepositEntity openDeposit(DepositBean depositBean, int newDepositId, Date startDate, UserProfile profile) {

        DepositEntity depositEntity = new DepositEntity(newDepositId, profile.getId(), depositBean.getMoneyType(), dividendsToStr(depositBean.getDividendsByDays()), startDate);

        // сразу выдаём первую часть
        int firstPart = depositBean.getImmediateDividend();
        switch (depositBean.getMoneyType()) {
            case REAL_MONEY:
                profile.setRealMoney(profile.getRealMoney() + firstPart);
                break;
            case MONEY:
                profile.setMoney(profile.getMoney() + firstPart);
                break;
        }
        depositEntity.setProgress(1);
        depositEntity.setLastPayDate(startDate);

        depositDao.insert(depositEntity);

        profile.addDeposit(depositEntity);
        log.info("Открыт вклад {} для [{}] id=[{}]. Первая часть - [{} {}] - выплачена ему.", depositBean, profile.getId(), depositEntity.getId(), firstPart, depositBean.getMoneyType().name());
        return depositEntity;
    }

    /**
     * Выплачивает игроку проценты за сегодня по всем открытым вкладам
     *
     * @param profile профиль игрока
     * @param now     момент времени "сейчас", для которого происходит расчёт (чтобы был неизменным за время расчёта)
     * @param result  out-параметр, в который будет сохранены значения полученных фузов и рубинов
     * @return true если проценты были начислены, иначе false (если нет вкладов или уже забирал сегодня)
     */
    public boolean payDividendTo(UserProfile profile, Date now, AnyMoneyAddition result) {
        if(dailyRegistry.isDividendPaid(profile.getId())) {
            // уже забирал проценты сегодня
            return false;
        } else {
            DepositEntity[] deposits = getDepositsFor(profile);
            int totalMoney = 0;
            int totalRealMoney = 0;
            // суммируем проценты от всех вкладов
            for(final DepositEntity deposit : deposits) {
                if(isToday(deposit.getLastPayDate(), now)) {
                    // конкретно с этого депозита проценты за сегодня уже были получен (например, он как раз сегодня был открыт)
                    continue;
                }
                int progress = deposit.getProgress();
                final MoneyType moneyType = MoneyType.valueOf(deposit.getMoneyType());
                final int[] dividendsByDays = dividendsFromStr(deposit.getDividendsByDays());
                final int partsCount = dividendsByDays.length;

                int moneyPart = 0;
                int realMoneyPart = 0;

                // если ещё не всё выплачено
                if(progress < partsCount) {
                    int part = dividendsByDays[progress];
                    // плюсуем проценты от этого вклада к общей сумме
                    switch (moneyType) {
                        case REAL_MONEY:
                            realMoneyPart = part;
                            totalRealMoney += part;
                            break;
                        case MONEY:
                            moneyPart = part;
                            totalMoney += part;
                            break;
                    }
                    progress++;
                    deposit.setProgress(progress);
                    deposit.setLastPayDate(now);
                    log.info("Вклад [{}]: progress->{}, lastPayDate->{}, выплачено [{} {}]", deposit.getId(), progress, AppUtils.formatDate(now), part, moneyType.name());
                }
                // если _теперь_ всё выплачено по этому вкладу (или уже и было выплачено, но почему-то не закрыто - изменение конфигов?)
                if(progress >= partsCount) {
                    // закрываем вклад
                    deposit.setPaidOff(true);
                    profile.removeDeposit(deposit);
                }
                try {
                    // сохраняем прогресс и статус paidOff в базе
                    transactionTemplate.execute(new TransactionCallback<Boolean>() {
                        @Override
                        public Boolean doInTransaction(TransactionStatus status) {
                            return depositDao.updateProgress(deposit);
                        }
                    });

                    profile.setRealMoney(profile.getRealMoney() + realMoneyPart);
                    profile.setMoney(profile.getMoney() + moneyPart);
                } catch (Exception e) {
                    log.error(e.toString(), e);

                    return false;
                }
            }

            profileService.updateSync(profile);

            // отмечаем, что на сегодня уже забрал всё
            dailyRegistry.setDividendPaid(profile.getId());

            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PAYMENT, profile,
                    Param.eventType, "dividend",
                    Param.money, totalMoney,
                    Param.realMoney, totalRealMoney
            );

            result.setMoney(totalMoney);
            result.setRealMoney(totalRealMoney);
            return totalMoney > 0 || totalRealMoney > 0;
        }
    }

    /**
     * Очищает состояние депозитов для данного игрока при обнулении профиля
     * <p>
     * Поскольку записи о депозитах не удаляются, при этом действии они все просто помечаются как закрытые (даже если не выплачены до конца)
     *
     * @param profile профиль, для которого происходит обнуление
     */
    public void clearDeposits(final UserProfile profile) {
        DepositEntity[] deposits = getDepositsFor(profile);
        for(DepositEntity deposit : deposits) {
            deposit.setPaidOff(true);
            deposit.setLastPayDate(new Date());
            depositDao.updateProgress(deposit);
        }
        profile.setDeposits(new DepositEntity[0]);
    }

    /**
     * Парсит строку, используемую в конфигах и в базе: "20 20 20 20 25" -> [20, 20, 20, 20, 25]
     */
    public int[] dividendsFromStr(String dividendsByDaysStr) {
        int[] dividendsByDays = null;
        if(dividendsByDaysStr != null) {
            String[] parts = dividendsByDaysStr.trim().split(" ");
            dividendsByDays = new int[parts.length];
            for(int i = 0; i < dividendsByDays.length; i++) {
                try {
                    dividendsByDays[i] = Integer.valueOf(parts[i]);
                } catch (NumberFormatException e) {
                    log.error("Рассчёт процентов по вкладу: Неверное число [{}] в строке dividendsByDaysStr='{}'", parts[i], dividendsByDaysStr);
                    dividendsByDays[i] = 0;
                }
            }
        }
        return dividendsByDays;
    }

    /**
     * Форматирует строку, используемую в конфигах и в базе: [20, 20, 20, 20, 25] -> "20 20 20 20 25"
     */
    public String dividendsToStr(int[] dividendsByDays) {
        return StringUtils.join(ArrayUtils.toObject(dividendsByDays), " ");
    }

    private boolean isToday(Date datetime, Date now) {
        if(datetime == null) {
            return false;
        }
        Calendar instant = Calendar.getInstance();
        instant.setTime(datetime);
        Calendar today = Calendar.getInstance();
        today.setTime(now);
        return instant.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                instant.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

}
