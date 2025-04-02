package com.pragmatix.app.cache.loaders;

import com.pragmatix.gameapp.cache.loaders.ILoader;
import com.pragmatix.pvp.model.PvpUser;
import org.springframework.stereotype.Component;

/**
 * Класс для загрузки из БД PvpUser
 * пустая реализация, необходимо чтобы не кидало исключение
 *
 * Created by IntelliJ IDEA.
 * User: denver
 * Date: 10.03.2010
 * Time: 22:09:30
 */
@Component
public class PvpUserLoader implements ILoader<PvpUser, Long> {

    @Override
    public PvpUser load(Long key) {
        return null;
    }

    @Override
    public Class<PvpUser> getLoadedClass() {
        return PvpUser.class;
    }
}
