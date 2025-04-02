package com.pragmatix.steam;

import com.pragmatix.app.common.OrderBean;
import com.pragmatix.app.common.PaymentType;
import com.pragmatix.app.messages.server.NeedMoneyResult;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.DaoService;
import com.pragmatix.app.services.PaymentService;
import com.pragmatix.app.services.social.SocialUserIdMapService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.steam.dao.PurchaseTx;
import com.pragmatix.steam.dao.PurchaseTxJpaDAO;
import com.pragmatix.steam.domain.SteamPriceSegment;
import com.pragmatix.steam.domain.SteamProduct;
import com.pragmatix.steam.messages.*;
import com.pragmatix.steam.web.request.InitTxnRequest;
import com.pragmatix.steam.web.request.OrderItemStructure;
import com.pragmatix.steam.web.responses.FinalizeTxnResponse;
import com.pragmatix.steam.web.responses.InitTxnResponse;
import com.pragmatix.steam.web.responses.SteamErrors;
import com.pragmatix.steam.web.responses.SteamWebResponse;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
public class SteamPurchaseService {

    private final Logger log = SocialService.PAYMENT_LOGGER;

    @Resource
    protected PurchaseTxJpaDAO purchaseTxDAO;

    @Resource
    protected SteamPurchaseBackend backend;

    @Resource
    protected SteamProductRepository productRepository;

    @Resource
    protected DaoService daoService;

    @Resource
    protected PaymentService paymentService;

    @Resource
    private SocialUserIdMapService socialUserIdMap;

    public void onLogin(UserProfile userProfile) {
        if(userProfile.getSocialId() == SocialServiceEnum.steam.getType()) {
            String socialUserId = getSocialUserId(userProfile);
            if(socialUserId.length() > 9 && (isEmpty(userProfile.getCountryCode()) || isEmpty(userProfile.getCurrencyCode()))) {
                SteamWebResponse.ErrorBlock error = backend.updatePurchaseInfo(socialUserId, userProfile);
                if(error != null) {
                    log.warn("Error [{}] when updating purchase info for user {}", error, socialUserId);
                }
            }
        }
    }

    public String getSocialUserId(UserProfile userProfile) {
        return socialUserIdMap.mapToStringId(userProfile.getId(), SocialServiceEnum.steam);
    }

    public InitPurchaseTxResponse initPurchaseTx(UserProfile userProfile, InitPurchaseTxRequest request) {

        SteamProduct steamProduct = productRepository.getProduct(request.productCode);

        String[] languageCode = new String[]{isoLangCode(userProfile.getLocale().name())};

        InitPurchaseTxResponse response = new InitPurchaseTxResponse();

        String socialUserId = getSocialUserId(userProfile);

        PurchaseTx tx = new PurchaseTx();
        tx.initDate = new Date();
        tx.userProfileId = userProfile.getId().intValue();
        tx.socialId = userProfile.getSocialId();
        tx.socialUserId = socialUserId;
        tx.stage = PurchaseTx.STAGE_INITIAL;
        tx.stageDate = tx.initDate;
        tx.productCode = steamProduct.code;
        tx.productCategory = steamProduct.category;
        tx.purchaseQuantity = steamProduct.count;

        try {
            SteamWebResponse.ErrorBlock error = backend.updatePurchaseInfo(socialUserId, userProfile);

            if(error != null) {
                log.warn("ERR_STEAM! unlockForSteamCurrency {}={}", "GetUserInfo", error);
            } else {
                response.countryCode = userProfile.getCountryCode();
                response.currencyCode = userProfile.getCurrencyCode();

                SteamPriceSegment priceSegment = productRepository.getProductPrices(userProfile.getCountryCode(), userProfile.getCurrencyCode());
                int productCost = priceSegment.getProductCost(steamProduct);

                String description = chooseDescription(steamProduct, languageCode, userProfile);
                OrderItemStructure item = new OrderItemStructure(steamProduct.id, 1, productCost, description);

                tx.countryCode = userProfile.getCountryCode();
                tx.currencyCode = priceSegment.currency.code;
                tx.purchaseCost = item.amount;

                daoService.doInTransactionWithoutResult(() -> purchaseTxDAO.insert(tx));

                response.purchaseOrderId = tx.getId();

                InitTxnRequest txnRequest = new InitTxnRequest();
                txnRequest.steamid = socialUserId;
                txnRequest.orderid = tx.getId();
                txnRequest.language = languageCode[0];
                txnRequest.currency = priceSegment.currency.code;
                txnRequest.lineitems = new ArrayList<>();
                txnRequest.lineitems.add(item);

                InitTxnResponse txnResponse = backend.initTxn(txnRequest);

                if(!txnResponse.isSuccess()) {
                    log.warn("ERR_STEAM! initPurchaseTx: {}", txnResponse.error);
                    if(txnResponse.error != null && txnResponse.error.errorcode != 0) {
                        tx.errorCode = txnResponse.error.errorcode;
                        tx.errorMessage = txnResponse.error.errordesc;

                        response.errorCode = txnResponse.error.errorcode;
                        response.errorMessage = txnResponse.error.errordesc;
                    } else {
                        // не должно такого быть
                        tx.stage = PurchaseTx.STAGE_REJECTED;
                        tx.stageDate = new Date();
                    }
                } else {
                    String purchaseTxId = txnResponse.transid;

                    tx.foreignTxId = purchaseTxId;
                    tx.stage = PurchaseTx.STAGE_PENDING;
                    tx.stageDate = new Date();

                    response.purchaseTxId = purchaseTxId;
                    response.socialParams = txnResponse.socialParams;
                }

                daoService.doInTransactionWithoutResult(() -> purchaseTxDAO.update(tx));
            }
        } catch (Throwable e) {
            log.error("Exception while initializing Steam transaction " + tx + " by request " + request, e);
        }
        return response;
    }

    private String chooseDescription(SteamProduct steamProduct, String[] languageCode, UserProfile profile) {
        String defaultLangCode = getDefaultLangCode();
        if(languageCode[0] == null || languageCode[0].isEmpty()) {
            languageCode[0] = isoLangCode(profile.getLocale().name());
        }
        String res = steamProduct.getDescription(languageCode[0]);
        if(res == null) {
            res = steamProduct.getDescription(defaultLangCode);
            if(res == null) {
                throw new IllegalArgumentException(String.format("Product description missing for language '%s'/'%s'", languageCode[0], defaultLangCode));
            }
            languageCode[0] = defaultLangCode;
        }
        return res;
    }

    protected String isoLangCode(String languageCode) {
        if(languageCode == null || languageCode.length() < 2) {
            return getDefaultLangCode();
        } else if(languageCode.length() > 2) {
            return languageCode.substring(0, 2);
        }
        return languageCode;
    }

    protected String getDefaultLangCode() {
        return productRepository.defaultLangCode;
    }

    public CommitPurchaseTxResponse commitPurchaseTx(UserProfile userProfile, CommitPurchaseTxRequest request) {
        final List<GenericAwardStructure> awards = new ArrayList<>();
        Consumer<Tuple2<UserProfile, PurchaseTx>> applyPayment = (profile_tx) -> {

            UserProfile profile = profile_tx._1;
            PurchaseTx tx = profile_tx._2;

            String transactionId = tx.foreignTxId;
            String productId = tx.productCode;
            Date transactionDate = tx.submitDate;

            OrderBean orderBean = new OrderBean(tx.productCategory.paymentType, tx.purchaseQuantity, tx.purchaseCost);

            int expiryTimeInSecconds = 0;
            if(orderBean.paymentType == PaymentType.MONEY || orderBean.paymentType == PaymentType.REAL_MONEY) {
                paymentService.applyPayment(profile, orderBean.paymentType, orderBean.paymentAmount, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId, false);
            } else if(orderBean.paymentType == PaymentType.RENAME) {
                paymentService.rename(profile, orderBean.paymentAmount, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId);
            } else if(orderBean.paymentType == PaymentType.DEPOSIT) {
                paymentService.openDeposit(profile, orderBean.paymentAmount, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId);
            } else if(orderBean.paymentType == PaymentType.VIP) {
                int vipFrom = Math.max(profile.getVipExpiryTime(), AppUtils.currentTimeSeconds());
                expiryTimeInSecconds = vipFrom + (int) TimeUnit.DAYS.toSeconds(orderBean.paymentAmount);

                paymentService.confirmMobileVip(profile, transactionId, productId, transactionDate.getTime(), expiryTimeInSecconds);
            } else if(orderBean.paymentType == PaymentType.CLAN_DONATE) {
                SteamProduct steamProduct = productRepository.getProduct(tx.productCode);
                paymentService.donateToClan(profile, orderBean.paymentAmount, steamProduct.paymentAmountComeback, orderBean.getPaymentCostInt(), transactionId, transactionDate, productId);
            } else if(orderBean.paymentType == PaymentType.BUNDLE) {
                awards.addAll(paymentService.purchaseBundle(profile, transactionId, transactionDate, productId)._2);
            }

            paymentService.onSuccessPayment(profile);
        };

        CommitPurchaseTxResponse commitPurchaseTxResponse = commitPurchaseTx(userProfile, request, applyPayment);
        commitPurchaseTxResponse.awards = awards;
        return commitPurchaseTxResponse;
    }

    public CommitPurchaseTxResponse commitPurchaseTx(UserProfile userProfile, CommitPurchaseTxRequest request, Consumer<Tuple2<UserProfile, PurchaseTx>> commitPurchaseTx) {
        String methodName = "commitPurchaseTx";
        CommitPurchaseTxResponse response = new CommitPurchaseTxResponse(request);
        int orderId = request.purchaseOrderId;
        boolean userPaidMoney = false;

        try {
            PurchaseTx tx = getTxByOrderId(orderId);
            if(tx == null) {
                log.warn("ERR_INVALID_ARGUMENT! methodName={}, orderId={}", methodName, orderId);
            } else {
                if(tx.foreignTxId == null || tx.foreignTxId.isEmpty()) {
                    tx.foreignTxId = request.purchaseTxId;
                }

                response.purchaseOrderId = tx.getId();
                response.purchaseTxId = tx.foreignTxId;

                if(tx.userProfileId != userProfile.getId() || !tx.stage.equals(PurchaseTx.STAGE_PENDING) || tx.errorCode != 0) {
                    log.warn("ERR_INVALID_STATE! methodName={}, purchaseOrderId={}, stage={}, errorCode={}", methodName, orderId, tx.stage, tx.errorCode);
                } else {
                    tx.stage = PurchaseTx.STAGE_AUTHORIZED;
                    tx.submitDate = tx.stageDate = new Date();

                    daoService.doInTransactionWithoutResult(() -> purchaseTxDAO.update(tx));

                    FinalizeTxnResponse txnResponse = backend.finalizeTxn(tx);

                    if(!txnResponse.isSuccess() && !backend.isDebugPaymentMode()) {
                        log.warn("ERR_STEAM! methodName={}, FinalizeTxnRequest={}", methodName, txnResponse.error);
                        if(txnResponse.error != null && txnResponse.error.errorcode != 0) {
                            tx.errorCode = txnResponse.error.errorcode;
                            tx.errorMessage = txnResponse.error.errordesc;

                            if(txnResponse.error.errorcode == SteamErrors.USER_NOT_APPROVED ||
                                    txnResponse.error.errorcode == SteamErrors.USER_DENIED) {
                                tx.stage = PurchaseTx.STAGE_CANCELED;
                                tx.stageDate = new Date();
                            }
                        } else {
                            // не должно такого быть
                            tx.stage = PurchaseTx.STAGE_REJECTED;
                            tx.stageDate = new Date();
                        }
                        daoService.doInTransactionWithoutResult(() -> purchaseTxDAO.update(tx));
                    } else {
                        userPaidMoney = true;
                        tx.stage = PurchaseTx.STAGE_SUCCESS;
                        tx.stageDate = new Date();
                        daoService.doInTransactionWithoutResult(() -> purchaseTxDAO.update(tx));

                        commitPurchaseTx.accept(Tuple.of(userProfile, tx));

                        response.result = NeedMoneyResult.ResultEnum.SUCCESS;
                        response.item = tx.productCode;
                        response.paymentType = tx.productCategory.paymentType;
                        response.count = tx.purchaseQuantity;
                    }
                }
            }
        } catch (Throwable e) {
            String prefix = userPaidMoney ? "!USER PAID MONEY! " : "";
            log.error(prefix + "Exception while committing Steam transaction " + request.purchaseTxId + " by request " + request, e);
            return null;
        }

        return response;
    }

    public ProductsInfoResponse getProductsInfo(UserProfile userProfile) {
        ProductsInfoResponse response = new ProductsInfoResponse();

        response.countryCode = userProfile.getCountryCode();
        response.currencyCode = userProfile.getCurrencyCode();

        SteamPriceSegment priceSegment = productRepository.getProductPrices(userProfile.getCountryCode(), userProfile.getCurrencyCode());

        String[] languageCode = new String[]{isoLangCode(userProfile.getLocale().name())};

        response.products = productRepository.products.values().stream().map(steamProduct -> {
            int productCost = priceSegment.getProductCost(steamProduct);
            String description = chooseDescription(steamProduct, languageCode, userProfile);
            return new SteamProductInfo(steamProduct.code, steamProduct.category.name(), productCost, description);
        }).collect(Collectors.toList());

        return response;
    }

    public PurchaseTx getTxByOrderId(int orderId) {
        return purchaseTxDAO.findById(orderId);
    }

}
