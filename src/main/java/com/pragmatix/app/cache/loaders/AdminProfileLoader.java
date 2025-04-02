package com.pragmatix.app.cache.loaders;

import com.pragmatix.admin.model.AdminProfile;
import com.pragmatix.gameapp.cache.loaders.ILoader;
import org.springframework.stereotype.Component;

/**
 * Класс для загрузки из БД AdminProfile
 * пустая реализация, необходимо чтобы не кидало исключение
 *
 * Created by IntelliJ IDEA.
 * User: denver
 * Date: 11.03.2010
 * Time: 23:57:49
 */
@Component
public class AdminProfileLoader implements ILoader<AdminProfile, String> {

    @Override
    public AdminProfile load(String key) {
        return null;
    }

    @Override
    public Class<AdminProfile> getLoadedClass() {
        return AdminProfile.class;
    }
}