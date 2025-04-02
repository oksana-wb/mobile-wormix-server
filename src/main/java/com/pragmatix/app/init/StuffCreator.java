package com.pragmatix.app.init;

import com.pragmatix.app.model.Stuff;
import com.pragmatix.server.Server;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс для создания списка предметов которые доступны игрку
 * <b>необходим при первом старте сервера на пустой БД</b>
 * <p/>
 * User: denis
 * Date: 19.11.2009
 * Time: 21:50:56
 */
public class StuffCreator {

    private Map<Short, Stuff> stuffMap = new ConcurrentHashMap<>();

    private short[] vipStuff = ArrayUtils.EMPTY_SHORT_ARRAY;

    public Collection<Stuff> getStuffs() {
        return stuffMap.values();
    }

    public void setStuffEntities(List<Stuff> stuffEntities) {
        for(Stuff weaponEntity : stuffEntities) {
            stuffMap.put(weaponEntity.getStuffId(), weaponEntity);
        }
    }

    public Stuff getStuff(short staffId){
        return stuffMap.get(staffId);
    }

    public short[] getVipStuff() {
        return vipStuff;
    }

    public void setVipStuff(short[] vipStuff) {
        this.vipStuff = vipStuff;
    }

}
