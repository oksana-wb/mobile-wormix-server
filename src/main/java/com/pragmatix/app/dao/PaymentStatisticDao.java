package com.pragmatix.app.dao;

import com.pragmatix.app.domain.PaymentStatisticEntity;
import com.pragmatix.dao.AbstractDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author denis
 *         date 25.11.2009
 *         time: 19:30:25
 */
@Component
public class PaymentStatisticDao extends AbstractDao<PaymentStatisticEntity> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public PaymentStatisticDao() {
        super(PaymentStatisticEntity.class);
    }

    /**
     * обновить status платежа
     *
     * @param paymentId     id платежа
     * @param paymentStatus статус плптежа
     * @param completed
     * @return true если удалить обновить
     */
    public boolean updatePaymentRequest(Integer paymentId, int paymentStatus, boolean completed) {
        int count = getEm().createNamedQuery("updatePaymentStatistic").
                setParameter("paymentStatus", paymentStatus).
                setParameter("completed", completed).
                setParameter("paymentId", paymentId).
                setParameter("updateDate", new Date()).
                executeUpdate();
        return count > 0;
    }

    public boolean updateSuccessPaymentRequest(Integer paymentId, long transactionId, int balanse) {
        int count = getEm().createNamedQuery("updateSuccessPaymentStatistic").
                setParameter("updateDate", new Date()).
                setParameter("transactionId", String.valueOf(transactionId)).
                setParameter("balanse", balanse).
                setParameter("paymentId", paymentId).
                executeUpdate();
        return count > 0;
    }

    public boolean updateFailurePaymentRequest(Integer paymentId, long transactionId, int balanse) {
        int count = getEm().createNamedQuery("updateFailurePaymentStatistic").
                setParameter("updateDate", new Date()).
                setParameter("transactionId", String.valueOf(transactionId)).
                setParameter("balanse", balanse).
                setParameter("paymentId", paymentId).
                executeUpdate();
        return count > 0;
    }

    public PaymentStatisticEntity selectByTransactionId(long transactionId) {
        return selectByTransactionId(String.valueOf(transactionId));
    }

    public PaymentStatisticEntity selectByTransactionId(String transactionId){
        PaymentStatisticEntity result = null;
        try {
            result = (PaymentStatisticEntity) getEm().createNamedQuery("selectPaymentByTransactionId").
                    setParameter("transactionId", transactionId).
                    getSingleResult();
        } catch (NoResultException e) {
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return result;
    }

    public List<PaymentStatisticEntity> selectPaymentStatistics(long userProfileId) {
        List<PaymentStatisticEntity> resultList = new ArrayList<PaymentStatisticEntity>();
        try {
            resultList = (List<PaymentStatisticEntity>) getEm().createNamedQuery("selectAllPaymentStatictic").setParameter("profileId", userProfileId).getResultList();
        } catch (NoResultException e) {
        }
        return resultList;
    }

}
