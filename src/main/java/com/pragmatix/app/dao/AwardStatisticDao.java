package com.pragmatix.app.dao;

import com.pragmatix.app.domain.AwardStatisticEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * User: denis
 * Date: 01.08.2010
 * Time: 21:44:51
 */
@Component
public class AwardStatisticDao extends AbstractDao<AwardStatisticEntity> {

    public AwardStatisticDao() {
        super(AwardStatisticEntity.class);
    }

}
