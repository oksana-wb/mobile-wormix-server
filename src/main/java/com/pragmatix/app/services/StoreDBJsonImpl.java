package com.pragmatix.app.services;

import com.google.gson.Gson;
import com.pragmatix.app.dao.StoreItemDao;
import com.pragmatix.app.domain.StoreItemEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.Null;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.07.13 20:18
 */
@Service
public class StoreDBJsonImpl implements Store {

    @Resource
    private StoreItemDao dao;

    @Resource
    private DaoService daoService;

    @Null
    @Override
    public <T> T load(String key, Class<T> clazz) {
        StoreItemEntity itemEntity = dao.get(key);
        if(itemEntity == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.fromJson(itemEntity.getValue(), clazz);
    }

    @Override
    public void save(String key, Object value) {
        Gson gson = new Gson();
        StoreItemEntity itemEntity = new StoreItemEntity(key, gson.toJson(value));
        daoService.doInTransactionWithoutResult(() -> dao.update(itemEntity));
    }

}
