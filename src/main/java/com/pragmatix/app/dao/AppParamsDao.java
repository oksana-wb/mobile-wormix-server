package com.pragmatix.app.dao;

import com.pragmatix.app.domain.AppParamsEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

/**
 * Dao класс для сохронения и загрузки глобальных настроек приложения
 *
 * Created: 26.04.11 18:36
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
@Component
public class AppParamsDao extends AbstractDao<AppParamsEntity> {

    protected AppParamsDao() {
        super(AppParamsEntity.class);
    }

    public AppParamsEntity selectAppParams() {
        return selectAppParams(1);
    }

    public AppParamsEntity selectAppParams(int id) {
        return getEm().find(AppParamsEntity.class, id);
    }
}
