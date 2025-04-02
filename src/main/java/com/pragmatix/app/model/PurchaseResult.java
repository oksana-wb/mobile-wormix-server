package com.pragmatix.app.model;

import com.pragmatix.app.common.CostStructure;
import com.pragmatix.app.common.ShopResultEnum;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 01.09.2016 16:17
 */
public class PurchaseResult {

    public static final PurchaseResult ERROR = new PurchaseResult(ShopResultEnum.ERROR, Collections.emptyList());

    public static final PurchaseResult MIN_REQUIREMENTS_ERROR = new PurchaseResult(ShopResultEnum.MIN_REQUIREMENTS_ERROR, Collections.emptyList());

    public static final PurchaseResult NOT_FOR_SALE = new PurchaseResult(ShopResultEnum.NOT_FOR_SALE, Collections.emptyList());

    public final ShopResultEnum result;

    // сколько и в какой валюте было списано за покупку
    public final List<CostStructure> cost;

    public boolean isSuccess(){
        return result == ShopResultEnum.SUCCESS;
    }

    public PurchaseResult(ShopResultEnum result, List<CostStructure> cost) {
        this.result = result;
        this.cost = cost;
    }

}
