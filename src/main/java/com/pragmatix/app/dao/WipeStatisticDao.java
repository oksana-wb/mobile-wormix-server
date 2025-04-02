package com.pragmatix.app.dao;

import com.pragmatix.app.domain.WipeStatisticEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

/**
 * User: denis
 * Date: 01.08.2010
 * Time: 21:44:51
 */
@Component
public class WipeStatisticDao extends AbstractDao<WipeStatisticEntity> {

    public WipeStatisticDao() {
        super(WipeStatisticEntity.class);
    }

}
