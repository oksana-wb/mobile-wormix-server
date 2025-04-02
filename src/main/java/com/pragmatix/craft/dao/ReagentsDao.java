package com.pragmatix.craft.dao;

import com.pragmatix.craft.domain.ReagentsEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 07.07.12 14:03
 */
@Service
public class ReagentsDao {

    private ReagentsMapper mapper;

    private final int[] emptyReagents = ReagentsEntity.initValues();

    public ReagentsDao(ReagentsMapper mapper) {
        this.mapper = mapper;
    }

    public ReagentsEntity select(long profileId) {
        return mapper.select(profileId);
    }

    public boolean persist(ReagentsEntity entity) {
        if(entity == null) {
            return false;
        }
        if(entity.isNewly()) {
            // не сохраняем в базу реагенты если они все нулевые
            if(!Arrays.equals(entity.getValues(), emptyReagents)) {
                boolean result = mapper.insert(entity) > 0;
                entity.setNewly(false);
                return result;
            } else {
                return false;
            }
        } else if(entity.isDirty()) {
            boolean result = mapper.update(entity) > 0;
            entity.setDirty(false);
            return result;
        } else {
            return false;
        }
    }

}
