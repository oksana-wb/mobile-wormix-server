package com.pragmatix.app.services.social.odnoklassniki;

import com.pragmatix.app.common.OrderBean;
import com.pragmatix.app.common.PaymentType;
import com.pragmatix.app.messages.structures.BundleStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BundleService;
import com.pragmatix.app.services.PaymentService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.payment.PaymentProcessingResult;
import com.pragmatix.gameapp.social.payment.PaymentProcessor;
import com.pragmatix.gameapp.social.payment.PaymentRecord;
import org.slf4j.Logger;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Author: Vladimir
 * Date: 03.08.12 9:34
 */
public class OdnoklassnikiPaymentProcessor implements PaymentProcessor {

    protected Logger logger = SocialService.PAYMENT_LOGGER;

    @Resource
    private ProfileService profileService;

    @Resource
    private PaymentService paymentService;

    private Map<String, List<Integer>> priceByItem;

    public PaymentProcessingResult processPayment(final PaymentRecord payment) {
        logger.info("Начинаем обработку платежа 'Одноклассников': " + payment);

        UserProfile profile = profileService.getUserProfile(payment.getUserId());
        if(profile == null) {
            logger.warn("Плательщик 'Одноклассников' не зарегистрирован в приложении: " + payment.getUserId());
            return PaymentProcessingResult.INVALID_USER;
        }

        String item = payment.getProductCode();
        OrderBean orderBean = paymentService.getOrderBean(priceByItem, item);
        if(orderBean == null) {
            return PaymentProcessingResult.INVALID_PARAMETER;
        }

        PaymentType paymentType = orderBean.paymentType;
        int amount = orderBean.paymentAmount;
        int votes = orderBean.getPaymentCostInt();
        if(votes != payment.getAmount()) {
            logger.error(String.format("[userId=%s] Указанное количество 'ok' [%s] не соответствует стоимости [%s] покупки [%s %s]",
                    profile.getProfileStringId(), payment.getAmount(), votes, paymentType, amount));
            votes = payment.getAmount();
            amount = 0;
        }

        long transactionId = 0;
        try {
            transactionId = Long.parseLong(payment.getTransactionId());
        } catch (NumberFormatException e) {
        }
        int newPaymentId = -1;
        Date paymentDate = payment.getTransactionTime();
        if(paymentType == PaymentType.MONEY || paymentType == PaymentType.REAL_MONEY) {
            newPaymentId = paymentService.applyPayment(profile, paymentType, amount, votes, "" + transactionId, paymentDate, item, true);
        } else if(paymentType == PaymentType.CLAN_DONATE) {
            newPaymentId = paymentService.donateToClan(profile, amount, orderBean.paymentAmountComeback, votes, "" + transactionId, paymentDate, item);
        } else if(paymentType == PaymentType.WIPE) {
            newPaymentId = paymentService.wipePayment(profile, amount, votes, "" + transactionId, paymentDate, item);
        } else if(paymentType == PaymentType.BUNDLE) {
            newPaymentId = paymentService.purchaseBundle(profile, "" + transactionId, paymentDate, item)._1;
        }
        PaymentProcessingResult paymentProcessingResult = newPaymentId > 0 ? PaymentProcessingResult.OK : PaymentProcessingResult.UNKNOWN;

        logger.info("Завершили обработку платежа 'Одноклассников' #" + payment.getTransactionId() + " " + paymentProcessingResult);

        return paymentProcessingResult;
    }

//====================== Getters and Setters =================================================================================================================================================


    public Map<String, List<Integer>> getPriceByItem() {
        return priceByItem;
    }

    public void setPriceByItem(Map<String, List<Integer>> priceByItem) {
        this.priceByItem = priceByItem;
    }
}
