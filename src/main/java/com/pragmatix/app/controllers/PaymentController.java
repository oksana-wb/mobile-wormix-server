package com.pragmatix.app.controllers;

import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.common.OrderBean;
import com.pragmatix.app.common.PaymentStatusEnum;
import com.pragmatix.app.common.PaymentType;
import com.pragmatix.app.messages.client.AppleVerifyReceipt;
import com.pragmatix.app.messages.client.GetDividend;
import com.pragmatix.app.messages.client.GetVipSubscription;
import com.pragmatix.app.messages.client.GoogleVerifyInappPurchase;
import com.pragmatix.app.messages.client.facebook.FbFulfillPayment;
import com.pragmatix.app.messages.client.facebook.FbRegisterMobilePayment;
import com.pragmatix.app.messages.client.facebook.FbRegisterPayment;
import com.pragmatix.app.messages.server.*;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.CheatersCheckerService;
import com.pragmatix.app.services.DepositService;
import com.pragmatix.app.services.StatisticService;
import com.pragmatix.app.services.social.android.GooglePurchaseService;
import com.pragmatix.app.services.social.facebook.FacebookPaymentProcessorImpl;
import com.pragmatix.app.services.social.ios.ApplePaymentService;
import com.pragmatix.common.utils.VarInt;
import com.pragmatix.common.utils.VarObject;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.service.VkontakteService;
import com.pragmatix.gameapp.social.service.facebook.FacebookError;
import com.pragmatix.gameapp.social.service.facebook.FacebookService;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;

/**
 * Контроллер который будет принимать с клиента
 * запросы на пополнение счета голосами
 */
@Controller
public class PaymentController {

    private final Logger log = SocialService.PAYMENT_LOGGER;

    @Resource
    private StatisticService statisticService;

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private Optional<DepositService> depositService;

    @Autowired(required = false)
    private VkontakteService vkontakteService;

    @Autowired(required = false)
    private FacebookPaymentProcessorImpl facebookPaymentProcessor;

    @Autowired(required = false)
    private FacebookService facebookService;

    @Autowired(required = false)
    private GooglePurchaseService googlePurchaseService;

    @Autowired(required = false)
    private ApplePaymentService applePaymentService;

    @OnMessage
    public VerifyPaymentResult onAppleVerifyReceipt(final AppleVerifyReceipt msg, final UserProfile userProfile) {
        log.info("verify request << {}", msg.toString());
        VerifyPaymentResult verifyPaymentResult = applePaymentService.verifyReceipt(msg, userProfile);
        log.info("send result >> {}", verifyPaymentResult);
        return verifyPaymentResult;
    }

    @OnMessage
    public VerifyPaymentResult onGoogleValidateInappPurchase(final GoogleVerifyInappPurchase msg, final UserProfile userProfile) {
        log.info("verify request << {}", msg.toString());

        VerifyPaymentResult verifyPaymentResult;
        if(!msg.isSubscription) {
            VarObject<OrderBean> varOrderBean = new VarObject<>();
            List<GenericAwardStructure> awards = new ArrayList<>();
            NeedMoneyResult.ResultEnum result = googlePurchaseService.validateInappPurchase(userProfile, msg.productId, msg.purchaseToken, varOrderBean, awards);
            if(varOrderBean.value != null) {
                verifyPaymentResult = new VerifyPaymentResult(result, varOrderBean.value.paymentType, msg.productId, varOrderBean.value.paymentAmount, msg.sessionKey, msg.purchaseToken);
                verifyPaymentResult.awards = awards;
            } else {
                verifyPaymentResult = new VerifyPaymentResult(result, PaymentType.UNDEFINED, msg.productId, 0, msg.sessionKey, msg.purchaseToken);
            }
        } else {
            VarObject<PaymentType> varSubscriptionType = new VarObject<>();
            VarInt expiryTimeInSecconds = new VarInt();
            NeedMoneyResult.ResultEnum result = googlePurchaseService.validateInappSubscription(userProfile, msg.productId, msg.purchaseToken, varSubscriptionType, expiryTimeInSecconds);
            if(varSubscriptionType.value != null) {
                verifyPaymentResult = new VerifyPaymentResult(result, varSubscriptionType.value, msg.productId, 0, msg.sessionKey, msg.purchaseToken);
            } else {
                verifyPaymentResult = new VerifyPaymentResult(result, PaymentType.UNDEFINED, msg.productId, 0, msg.sessionKey, msg.purchaseToken);
            }
            verifyPaymentResult.subscriptionExpiryTimeSecconds = expiryTimeInSecconds.value;
        }
        verifyPaymentResult.item = msg.productId;

        log.info("send result >> {}", verifyPaymentResult);
        return verifyPaymentResult;
    }

    @OnMessage
    public FbRegisterPaymentResult onFbRegisterPayment(final FbRegisterPayment msg, final UserProfile profile) {
        //https://developers.facebook.com/docs/howtos/payments/fulfillment/
        log.info("register payment << {}", msg.toString());

        if(cheatersCheckerService.checkPaymentDelay(profile)) {
            //отправляем на клиент инфу о том что произошла ошибка
            log.error("платежы выполняются слишком часто");
            return new FbRegisterPaymentResult(SimpleResultEnum.ERROR, msg, -1);
        }

        final OrderBean orderBean = facebookPaymentProcessor.getOrderBean(msg.itemId);
        if(orderBean == null) {
            log.error("не зарегистрированный код платежа [{}]", msg.itemId);
            return new FbRegisterPaymentResult(SimpleResultEnum.ERROR, msg, -1);
        }

        // сохраняем запрос
        try {
            final Integer paymentId = transactionTemplate.execute(new TransactionCallback<Integer>() {
                @Override
                public Integer doInTransaction(TransactionStatus status) {
                    int balanse = orderBean.paymentType == PaymentType.MONEY ? profile.getMoney() : profile.getRealMoney();
                    return statisticService.insertPaymentRequest(profile.getId(), orderBean.paymentType.type, orderBean.paymentCost, (short) PaymentStatusEnum.TOUCH.getType(), false, orderBean.paymentAmount, balanse, msg.itemId);
                }
            });

            FbRegisterPaymentResult result = new FbRegisterPaymentResult(SimpleResultEnum.SUCCESS, msg, paymentId);

            log.info("register payment result >> {}", result);

            return result;
        } catch (TransactionException e) {
            log.error(e.toString(), e);
            return new FbRegisterPaymentResult(SimpleResultEnum.ERROR, msg, -1);
        }
    }

    @OnMessage
    public FbRegisterPaymentResult onFbRegisterMobilePayment(final FbRegisterMobilePayment msg, final UserProfile profile) {
        //https://developers.facebook.com/docs/howtos/payments/fulfillment/
        log.info("register mobile payment << {}", msg.toString());

        if(cheatersCheckerService.checkPaymentDelay(profile)) {
            //отправляем на клиент инфу о том что произошла ошибка
            log.error("платежы выполняются слишком часто");
            return new FbRegisterPaymentResult(SimpleResultEnum.ERROR, msg, -1);
        }

        final OrderBean orderBean = new OrderBean(msg.moneyType == MoneyType.REAL_MONEY ? PaymentType.REAL_MONEY : PaymentType.MONEY, msg.amount, msg.votes);

        // сохраняем запрос
        try {
            final Integer paymentId = transactionTemplate.execute(new TransactionCallback<Integer>() {
                @Override
                public Integer doInTransaction(TransactionStatus status) {
                    int balanse = orderBean.paymentType == PaymentType.REAL_MONEY ? profile.getRealMoney() : profile.getMoney();
                    return statisticService.insertPaymentRequest(profile.getId(), orderBean.paymentType.type, orderBean.paymentCost, (short) PaymentStatusEnum.TOUCH.getType(), false, orderBean.paymentAmount, balanse, msg.pricepointId);
                }
            });

            FbRegisterPaymentResult result = new FbRegisterPaymentResult(SimpleResultEnum.SUCCESS, msg, paymentId);

            log.info("register payment result >> {}", result);

            return result;
        } catch (TransactionException e) {
            log.error(e.toString(), e);
            return new FbRegisterPaymentResult(SimpleResultEnum.ERROR, msg, -1);
        }
    }

    @OnMessage
    public void onFbFulfillPayment(final FbFulfillPayment msg, final UserProfile profile) {
        log.info("fulfill payment << {}", msg.toString());

        VarObject<JSONObject> jsonObjectVar = new VarObject<>();
        FacebookError fbError = facebookService.parseSignedRequest(msg.signedRequest, jsonObjectVar, log);
        log.info("signedRequest: {}", jsonObjectVar.value);
        if(fbError != FacebookError.OK) {
            log.error("Order fulfillment error [{}]", fbError);
            return;
        }
        log.info(jsonObjectVar.value.toString());

        int paymentId = jsonObjectVar.value.getInt("request_id");
        long transactionId = jsonObjectVar.value.getLong("payment_id");
        String status = jsonObjectVar.value.getString("status");

        boolean completed = true;
        if(paymentId != msg.paymentId) {
            log.error("идентификатор платежa [{}] не равен ожидаемому [{}]", paymentId, msg.paymentId);
            completed = false;
        }

        completed = completed && status.equals("completed");
        facebookPaymentProcessor.fulfillPayment(paymentId, transactionId, completed);
    }

    @OnMessage
    public GetDividendResult onGetDividend(GetDividend msg, UserProfile profile) {
        GetDividendResult result = new GetDividendResult(Sessions.getKey());
        boolean success = depositService.map(service -> service.payDividendTo(profile, new Date(), result)).orElse(false);
        result.result = success ? SimpleResultEnum.SUCCESS : SimpleResultEnum.ERROR;
        return result;
    }

    @OnMessage
    public VipSubscriptionResponse onGetVipSubscription(GetVipSubscription msg, UserProfile profile) {
        return Optional.ofNullable(vkontakteService)
                .flatMap(service -> service.getUserSubscription(profile.id, msg.vipSubscriptionId))
                .map(VipSubscriptionResponse::new)
                .orElse(new VipSubscriptionResponse(msg.vipSubscriptionId));
    }

}
