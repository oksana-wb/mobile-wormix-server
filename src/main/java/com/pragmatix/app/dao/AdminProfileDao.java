package com.pragmatix.app.dao;

import com.pragmatix.app.domain.AdminProfileEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

/**
 * Dao класс для сохронения и загрузки AdminProfileEntity
 * User: denis
 * Date: 15.11.2009
 * Time: 3:54:47
 */
@Component
public class AdminProfileDao extends AbstractDao<AdminProfileEntity> {

    public AdminProfileDao() {
        super(AdminProfileEntity.class);
    }

    /**
     * удалить профайл админа из БД
     * @param login логин админа которого будем удалять
     * @return true если удалить удалось
     */
    public boolean deleteByLogin(String login) {
        int count = getEm().createNamedQuery("deleteAdminProfileByLogin").
                setParameter("login", login).executeUpdate();
        return count > 0;
    }

}