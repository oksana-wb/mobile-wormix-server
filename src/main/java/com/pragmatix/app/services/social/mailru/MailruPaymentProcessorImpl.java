package com.pragmatix.app.services.social.mailru;

import com.pragmatix.app.common.OrderBean;
import com.pragmatix.app.common.PaymentType;
import com.pragmatix.app.dao.PaymentStatisticDao;
import com.pragmatix.app.domain.PaymentStatisticEntity;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.PaymentService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.service.mailru.MailruPaymentProcessingResult;
import com.pragmatix.gameapp.social.service.mailru.MailruPaymentProcessor;
import com.pragmatix.gameapp.social.service.mailru.MailruPaymentRecord;
import org.slf4j.Logger;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MailruPaymentProcessorImpl implements MailruPaymentProcessor {

    private final Logger log = SocialService.PAYMENT_LOGGER;

    //indexes: 0 - MoneyType 1 - moneyCount 2 - votes
    private Map<String, List<Integer>> priceByItem;

    @Resource
    private ProfileService profileService;

    @Resource
    private PaymentStatisticDao paymentStatisticDao;

    @Resource
    private PaymentService paymentService;

    public MailruPaymentProcessingResult processPayment(final MailruPaymentRecord paymentRecord) {
        final long transactionId = paymentRecord.transactionId;
        String item = "" + paymentRecord.serviceId;
        OrderBean orderBean = paymentService.getOrderBean(priceByItem, item);
        if(orderBean == null) {
            return new MailruPaymentProcessingResult(MailruPaymentProcessingResult.Status.Error, MailruPaymentProcessingResult.ErrorCode.ServiceNotFound);
        }
        if(paymentRecord.mailikiPrice != orderBean.getPaymentCostInt()) {
            log.error("Цена в платеже не соответствует покупке; item={}, paymentPrice={}; paymentCost={}", item, paymentRecord.mailikiPrice, orderBean.paymentCost);
            return new MailruPaymentProcessingResult(MailruPaymentProcessingResult.Status.Error, MailruPaymentProcessingResult.ErrorCode.IncorrectPrice);
        }

        final UserProfile profile = profileService.getUserProfile(paymentRecord.uid);
        if(profile == null) {
            log.error("пользователь не найден по id соц. сети '{}'", paymentRecord.uid);
            return new MailruPaymentProcessingResult(MailruPaymentProcessingResult.Status.Error, MailruPaymentProcessingResult.ErrorCode.UserNotFound);
        }

        PaymentStatisticEntity paymentStatisticEntity = paymentStatisticDao.selectByTransactionId(transactionId);
        if(paymentStatisticEntity != null) {
            if(paymentStatisticEntity.getPaymentStatus() == 0) {
                log.info("Повторный запрос orderStatusChange для order_id [{}]", transactionId);
                return MailruPaymentProcessingResult.SuccessResult;
            } else {
                log.error("Платеж [{}] найден, но имеет ошибочный статус [{}]", transactionId, paymentStatisticEntity.getPaymentStatus());
                return MailruPaymentProcessingResult.ErrorResult;
            }
        }

        int newPaymentId = -1;
        Date paymentDate = new Date();
        if(orderBean.paymentType == PaymentType.MONEY || orderBean.paymentType == PaymentType.REAL_MONEY) {
            newPaymentId = paymentService.applyPayment(profile,  orderBean.paymentType, orderBean.paymentAmount, orderBean.getPaymentCostInt(), "" + transactionId, paymentDate, item, true);
        } else if(orderBean.paymentType == PaymentType.CLAN_DONATE) {
            newPaymentId = paymentService.donateToClan(profile, orderBean.paymentAmount, orderBean.paymentAmountComeback, orderBean.getPaymentCostInt(), "" + transactionId, paymentDate, item);
        }else if(orderBean.paymentType == PaymentType.WIPE){
            newPaymentId = paymentService.wipePayment(profile, orderBean.paymentAmount, orderBean.getPaymentCostInt(), "" + transactionId, paymentDate, item);
        } else if(orderBean.paymentType == PaymentType.BUNDLE) {
            newPaymentId = paymentService.purchaseBundle(profile, "" + transactionId, paymentDate, item)._1;
        }

        if(newPaymentId == -1) {
            // возвращаяем ошибку, плетеж будет передоставлен позднее
            return new MailruPaymentProcessingResult(MailruPaymentProcessingResult.Status.ImpermanentError, MailruPaymentProcessingResult.ErrorCode.OtherError);
        } else if(newPaymentId == -2) {
            // возвращаяем ошибку
            return new MailruPaymentProcessingResult(MailruPaymentProcessingResult.Status.Error, MailruPaymentProcessingResult.ErrorCode.UserNotFound);
        }

        return MailruPaymentProcessingResult.SuccessResult;
    }

//====================== Getters and Setters =================================================================================================================================================

    public Map<String, List<Integer>> getPriceByItem() {
        return priceByItem;
    }

    public void setPriceByItem(Map<String, List<Integer>> priceByItem) {
        this.priceByItem = priceByItem;
    }
}
