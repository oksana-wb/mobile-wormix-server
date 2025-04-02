package com.pragmatix.steam.dao;

import com.pragmatix.steam.domain.SteamProduct;

import java.util.Date;

/**
 * Author: Vladimir
 * Date: 10.03.2017 17:33
 */
public class PurchaseTx extends DomainObject<Integer> {
    /**
     * ожидает отправки в стим
     */
    public static final String STAGE_INITIAL = "initial";
    /**
     * ожидает подтверждения пользователя
     */
    public static final String STAGE_PENDING = "pending";
    /**
     * одобрена пользователем и отправлена в стим, ожидаем списание денег с клиента
     */
    public static final String STAGE_AUTHORIZED = "authorized";
    /**
     * отменена пользователем
     */
    public static final String STAGE_CANCELED = "canceled";
    /**
     * успешно завершена, средства списаны
     */
    public static final String STAGE_SUCCESS = "success";
    /**
     * отклонена стимом
     */
    public static String STAGE_REJECTED = "rejected";
    /** время инициации покупки игроком */
    public Date initDate;

    /** id транзакции, присвоенный платёжной системой */
    public String foreignTxId;

    /** профиль покупателя */
    public int userProfileId;

    /** идентификатор соц. сети покупателя */
    public int socialId;

    /** имя пользователя покупателя в соцсети */
    public String socialUserId;

    /** 2-символьный код страны покупателя в платежной системе (ISO 3166-1-alpha-2) */
    public String countryCode;

    /** 3-символьный код валюты покупки (ISO 4217) */
    public String currencyCode;
    /**
     * категория покупаемого товара
     */
    public SteamProduct.SteamProductCategory productCategory;
    /**
     * код покупаемого товара
     */
    public String productCode;

    /** количество единиц покупаемого товара */
    public int purchaseQuantity;

    /** полная стоимость покупки */
    public int purchaseCost;

    /** время одобрения покупки пользователем */
    public Date submitDate;

    /** состояние (этап) обработки транзакции */
    public String stage;

    /** время последнего изменения состояния транзакции {@link PurchaseTx#stage} */
    public Date stageDate;

    /** код ошибки (либо 0 в случае успеха) */
    public int errorCode;

    /** сообщение об ошибке (либо null в случае успеха) */
    public String errorMessage;

    @Override
    public String toString() {
        return "PurchaseTx{" +
                "id=" + getId() +
                ", initDate=" + initDate +
                ", foreignTxId='" + foreignTxId + '\'' +
                ", userProfileId=" + userProfileId +
                ", socialId=" + socialId +
                ", socialUserId='" + socialUserId + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", currencyCode='" + currencyCode + '\'' +
                ", productCategory='" + productCategory + '\'' +
                ", productCode='" + productCode + '\'' +
                ", purchaseQuantity=" + purchaseQuantity +
                ", purchaseCost=" + purchaseCost +
                ", submitDate=" + submitDate +
                ", stage='" + stage + '\'' +
                ", stageDate=" + stageDate +
                (errorCode != 0 ?
                        ", errorCode=" + errorCode +
                        ", errorMessage='" + errorMessage + '\'': "") +
                '}';
    }
}
