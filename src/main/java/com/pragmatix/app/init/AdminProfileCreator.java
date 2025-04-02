package com.pragmatix.app.init;

import com.pragmatix.admin.common.RoleType;
import com.pragmatix.app.dao.AdminProfileDao;
import com.pragmatix.app.domain.AdminProfileEntity;
import com.pragmatix.app.services.DaoService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Класс для создания списка админов приложения
 * <b>необходим при первом старте сервера на пустой БД</b>
 *
 * User: denis
 * Date: 09.03.2011
 * Time: 16:37:30
 */
@Component
public class AdminProfileCreator {

    @Resource
    private DaoService daoService;

    /**
     * Метод для создания списка админов приложения и сохранения его в БД
     * @return список созданных уровней
     */
    public List<AdminProfileEntity> createAdmins() {
        List<AdminProfileEntity> entities = new ArrayList<AdminProfileEntity>();
        entities.add(createAdmin("Оксана","6548482", "Qw3rt6uey", RoleType.SUPER_ADMIN_ROLE.getType(), "6548482"));
        return entities;
    }

    /**
     * Создает новый профайл админа в БД
     *
     * @param login логин
     * @param password пароль
     * @param role роль
     * @param creator кем создан
     * @return профайл админа
     */
    public AdminProfileEntity createAdmin(String name, String login, String password, byte role, String creator) {
        AdminProfileEntity entity = new AdminProfileEntity();
        entity.setLogin(login);
        entity.setPassword(password);
        entity.setName(name);
        entity.setRole(role);
        entity.setCreateDate(new Date());
        entity.setCreatedBy(creator);
        AdminProfileDao adminProfileDao = daoService.getAdminProfileDao();
        // удаляем прежную запись
        adminProfileDao.deleteByLogin(login) ;
        // вставляем новую
        adminProfileDao.insert(entity) ;
        return entity;
    }
}
