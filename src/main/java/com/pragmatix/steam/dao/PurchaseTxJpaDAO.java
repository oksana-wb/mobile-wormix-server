package com.pragmatix.steam.dao;


import com.pragmatix.steam.domain.SteamProduct;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Author: Vladimir
 * Date: 10.03.2017 17:59
 */
@Component
public class PurchaseTxJpaDAO extends JpaDAO<Integer, PurchaseTx, PurchaseTxEntity> {

    @Override
    public Class<PurchaseTxEntity> getEntityClass() {
        return PurchaseTxEntity.class;
    }

    @Override
    protected PurchaseTx createPrincipal(PurchaseTxEntity entity) {
        PurchaseTx res = new PurchaseTx();
        res.setId(entity.getId());
        res.userProfileId = entity.getUserProfileId();
        res.socialId = entity.getSocialId();
        res.socialUserId = entity.getSocialUserId();
        res.initDate = entity.getInitDate();
        res.foreignTxId = entity.getForeignTxId();
        res.countryCode = entity.getCountryCode();
        res.currencyCode = entity.getCurrencyCode();
        res.productCategory = SteamProduct.SteamProductCategory.valueOf(entity.getProductCategory());
        res.productCode = entity.getProductCode();
        res.purchaseQuantity = entity.getPurchaseQuantity();
        res.purchaseCost = entity.getPurchaseCost();
        res.submitDate = entity.getSubmitDate();
        res.stage = entity.getStage();
        res.stageDate = entity.getStageDate();
        res.errorCode = entity.getErrorCode();
        res.errorMessage = entity.getErrorMessage();
        return res;
    }

    @Override
    protected PurchaseTxEntity createEntity(PurchaseTx principal) {
        PurchaseTxEntity res = new PurchaseTxEntity();
        res.setId(principal.getId());
        res.setUserProfileId(principal.userProfileId);
        res.setSocialId(principal.socialId);
        res.setSocialUserId(principal.socialUserId);
        res.setInitDate(principal.initDate);
        res.setForeignTxId(principal.foreignTxId);
        res.setCountryCode(principal.countryCode);
        res.setCurrencyCode(principal.currencyCode);
        res.setProductCategory(principal.productCategory.name());
        res.setProductCode(principal.productCode);
        res.setPurchaseQuantity(principal.purchaseQuantity);
        res.setPurchaseCost(principal.purchaseCost);
        res.setSubmitDate(principal.submitDate);
        res.setStage(principal.stage);
        res.setStageDate(principal.stageDate);
        res.setErrorCode(principal.errorCode);
        res.setErrorMessage(principal.errorMessage);
        return res;
    }

    public PurchaseTx findByForeignTxId(String foreignTxId) {
        PurchaseTxEntity entity = findEntityNQ(PurchaseTxEntity.NQ_FIND_BY_FOREIGN_TX_ID,
                new String[] {"foreignTxId"}, new Object[] {foreignTxId});

        return entity != null ? createPrincipal(entity) : null;
    }
}
