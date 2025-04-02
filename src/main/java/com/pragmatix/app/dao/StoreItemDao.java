package com.pragmatix.app.dao;

import com.pragmatix.app.domain.StoreItemEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.07.13 20:15
 */
@Component
public class StoreItemDao extends AbstractDao<StoreItemEntity> {

    protected StoreItemDao() {
        super(StoreItemEntity.class);
    }
}
