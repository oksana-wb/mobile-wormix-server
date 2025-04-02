package com.pragmatix.app.dao;

import com.pragmatix.app.domain.ShopStatisticEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * @author denis
 *         Date: 13.01.2010
 *         Time: 21:48:44
 */
@Component
public class ShopStatisticDao extends AbstractDao<ShopStatisticEntity> {

    public ShopStatisticDao() {
        super(ShopStatisticEntity.class);
    }

}
