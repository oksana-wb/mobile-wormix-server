package com.pragmatix.steam.dao;

import javax.persistence.*;
import java.util.Date;

/**
 * Author: Vladimir
 * Date: 10.03.2017 17:33
 */
@javax.persistence.Entity
@Table(schema = "wormswar",name = "purchase_tx")
@NamedQueries({
        @NamedQuery(name = PurchaseTxEntity.NQ_FIND_BY_FOREIGN_TX_ID, query = "SELECT e FROM PurchaseTxEntity e" +
                "\nWHERE e.foreignTxId=:foreignTxId")
})
public class PurchaseTxEntity extends Entity<Integer> {
    public static final String NQ_FIND_BY_FOREIGN_TX_ID = "PurchaseTx.findByForeignTxId";

    private Date initDate;

    private String foreignTxId;

    private int userProfileId;

    private int socialId;

    private String socialUserId;

    public String countryCode;

    private String currencyCode;

    private String productCategory;

    private String productCode;

    private int purchaseQuantity;

    private int purchaseCost;

    private Date submitDate;

    private String stage;

    private Date stageDate;

    private int errorCode;

    private String errorMessage;

    /**
     * @return идентификатор записи
     */
    @Override
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    public Integer getId() {
        return id;
    }

    @Column(name = "tx_init_date", nullable = true)
    public Date getInitDate() {
        return initDate;
    }

    public void setInitDate(Date initDate) {
        this.initDate = initDate;
    }

    @Column(name = "foreign_tx_id")
    public String getForeignTxId() {
        return foreignTxId;
    }

    public void setForeignTxId(String foreignTxId) {
        this.foreignTxId = foreignTxId;
    }

    @Column(name = "user_profile_id", nullable = false)
    public int getUserProfileId() {
        return userProfileId;
    }

    public void setUserProfileId(int userProfileId) {
        this.userProfileId = userProfileId;
    }

    @Column(name = "social_id", nullable = false)
    public int getSocialId() {
        return socialId;
    }

    public void setSocialId(int socialId) {
        this.socialId = socialId;
    }

    @Column(name = "social_user_id", nullable = false)
    public String getSocialUserId() {
        return socialUserId;
    }

    public void setSocialUserId(String socialUserId) {
        this.socialUserId = socialUserId;
    }

    @Column(name = "country", nullable = false)
    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Column(name = "currency", nullable = false)
    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @Column(name = "product_category", nullable = false)
    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    @Column(name = "product_code", nullable = false)
    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    @Column(name = "purchase_quantity", nullable = false)
    public int getPurchaseQuantity() {
        return purchaseQuantity;
    }

    public void setPurchaseQuantity(int purchaseAmount) {
        this.purchaseQuantity = purchaseAmount;
    }

    @Column(name = "purchase_cost", nullable = false)
    public int getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(int purchaseCost) {
        this.purchaseCost = purchaseCost;
    }

    @Column(name = "tx_submit_date")
    public Date getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(Date submit_date) {
        this.submitDate = submit_date;
    }

    @Column(name = "tx_stage", nullable = false)
    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    @Column(name = "tx_stage_date", nullable = false)
    public Date getStageDate() {
        return stageDate;
    }

    public void setStageDate(Date stageDate) {
        this.stageDate = stageDate;
    }

    @Column(name = "error_code", nullable = false)
    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    @Column(name = "error_message")
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
