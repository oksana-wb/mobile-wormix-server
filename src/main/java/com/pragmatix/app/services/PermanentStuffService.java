package com.pragmatix.app.services;

import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class PermanentStuffService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * добаить новый предмет(шляпу, амулет) в список предметов игрока
     *
     * @param item id предмета
     */
    public void addStuffFor(UserProfile userProfile, short item) {
        userProfile.setStuff(ArrayUtils.add(userProfile.getStuff(), item));
    }

    /**
     * удалить предмет(шляпу, амулет) из списка предметов игрока
     *
     * @param item id предмета
     */
    public void removeStuffFor(UserProfile userProfile, short item) {
        userProfile.setStuff(ArrayUtils.removeElement(userProfile.getStuff(), item));
    }

    /**
     * покупка нового предмета (шапки или снаряжения)
     *
     * @param profile профайл игрока купивший шапку
     * @param stuff   предмета
     */
    public boolean addStuff(UserProfile profile, Stuff stuff) {
        if(stuff.isTemporal()) {
            log.error("предмет не может быть временным {}", stuff);
            return false;
        }

        if(isExist(profile, stuff.getStuffId())) {
          return false;
        }
        addStuffFor(profile, stuff.getStuffId());
        return true;
    }

    /**
     * проверяем есть ли такой предмет уже в рюкзаке
     *
     * @param profile профайл игрока
     * @param itemId  id предмета
     * @return true если есть
     */
    public boolean isExist(UserProfile profile, short itemId) {
        return ArrayUtils.contains(profile.getStuff(), itemId);
    }

}
