package com.pragmatix.app.services;

import com.pragmatix.app.dao.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.concurrent.Callable;

/**
 * в данный класс сетится спрингом все Dao модули приложения
 * User: denis
 * Date: 15.11.2009
 * Time: 4:03:03
 */
@Service
public class DaoService {

    @Resource
    private BackpackItemDao backpackItemDao;

    @Resource
    private UserProfileDao userProfileDao;

    @Resource
    private WormGroupsDao wormGroupsDao;

    @Resource
    private PaymentStatisticDao paymentStatisticDao;

    @Resource
    private ShopStatisticDao shopStatisticDao;

    @Resource
    private AwardStatisticDao awardStatisticDao;

    @Resource
    private WipeStatisticDao wipeStatisticDao;

    @Resource
    private BanDao banDao;

    @Resource
    private RestrictionDao restrictionDao;

    @Resource
    private AdminProfileDao adminProfileDao;

    @Resource
    private AppParamsDao appParamsDao;

    @Resource
    private AuditAdminActionDao auditAdminActionDao;

    @Resource
    private SocialIdDao socialIdDao;

    @Resource
    private CheaterStatisticDao cheaterStatisticDao;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Value("${dataSource.port:5432}")
    public int dataSourcePort;

    @Value("${dataSource.db:wormswar}")
    public String dataSourceDb;

    @Value("${pgDumpPath:}")
    public String pgDumpPath;

    @Resource
    private DepositDao depositDao;

    public BackpackItemDao getBackpackItemDao() {
        return backpackItemDao;
    }

    public void setBackpackItemDao(BackpackItemDao backpackItemDao) {
        this.backpackItemDao = backpackItemDao;
    }

    public UserProfileDao getUserProfileDao() {
        return userProfileDao;
    }

    public void setUserProfileDao(UserProfileDao userProfileDao) {
        this.userProfileDao = userProfileDao;
    }

    public WormGroupsDao getWormGroupDao() {
        return wormGroupsDao;
    }

    public void setWormGroupDao(WormGroupsDao wormGroupDao) {
        this.wormGroupsDao = wormGroupDao;
    }

    public PaymentStatisticDao getPaymentStatisticDao() {
        return paymentStatisticDao;
    }

    public void setPaymentStatisticDao(PaymentStatisticDao paymentStatisticDao) {
        this.paymentStatisticDao = paymentStatisticDao;
    }

    public ShopStatisticDao getShopStatisticDao() {
        return shopStatisticDao;
    }

    public void setShopStatisticDao(ShopStatisticDao shopStatisticDao) {
        this.shopStatisticDao = shopStatisticDao;
    }

    public AwardStatisticDao getAwardStatisticDao() {
        return awardStatisticDao;
    }

    public void setAwardStatisticDao(AwardStatisticDao awardStatisticDao) {
        this.awardStatisticDao = awardStatisticDao;
    }

    public BanDao getBanDao() {
        return banDao;
    }

    public void setBanDao(BanDao banDao) {
        this.banDao = banDao;
    }

    public AdminProfileDao getAdminProfileDao() {
        return adminProfileDao;
    }

    public void setAdminProfileDao(AdminProfileDao adminProfileDao) {
        this.adminProfileDao = adminProfileDao;
    }

    public AppParamsDao getAppParamsDao() {
        return appParamsDao;
    }

    public void setAppParamsDao(AppParamsDao appParamsDao) {
        this.appParamsDao = appParamsDao;
    }

    public WipeStatisticDao getWipeStatisticDao() {
        return wipeStatisticDao;
    }

    public AuditAdminActionDao getAuditAdminActionDao() {
        return auditAdminActionDao;
    }

    public SocialIdDao getSocialIdDao() {
        return socialIdDao;
    }

    public CheaterStatisticDao getCheaterStatisticDao() {
        return cheaterStatisticDao;
    }

    public void doInTransactionWithoutResult(Runnable task) throws TransactionException {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                task.run();
            }
        });
    }

    public <T> T doInTransaction(Callable<T> callable) throws TransactionException {
        return transactionTemplate.execute(transactionStatus -> {
            try {
                return callable.call();
            } catch (Exception e) {
               throw new IllegalTransactionStateException(e.toString(), e);
            }
        });
    }

    public void update(String sql, Object... args) throws DataAccessException {
        doInTransactionWithoutResult(() -> jdbcTemplate.update(sql, args));
    }

    public DepositDao getDepositDao() {
        return depositDao;
    }

    public RestrictionDao getRestrictionDao() {
        return restrictionDao;
    }
}

