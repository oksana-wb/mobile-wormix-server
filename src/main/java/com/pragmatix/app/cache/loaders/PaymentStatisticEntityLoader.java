package com.pragmatix.app.cache.loaders;

import com.pragmatix.app.dao.PaymentStatisticDao;
import com.pragmatix.app.domain.PaymentStatisticEntity;
import com.pragmatix.gameapp.cache.loaders.ILoader;
import com.pragmatix.pvp.model.PvpUser;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.04.2016 14:22
 */
@Component
public class PaymentStatisticEntityLoader  implements ILoader<PaymentStatisticEntity, String> {

    @Resource
    private PaymentStatisticDao paymentStatisticDao;

    @Override
    public PaymentStatisticEntity load(String key) {
        return paymentStatisticDao.selectByTransactionId(key);
    }

    @Override
    public Class<PaymentStatisticEntity> getLoadedClass() {
        return PaymentStatisticEntity.class;
    }

}
