package com.pragmatix.app.init;

import com.pragmatix.app.model.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Null;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * класс для создания сетки уровней
 */
public class LevelCreator {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private int maxLevel;

    private final Map<Integer, Level> levels = new LinkedHashMap<>();

    private int maxAvailablePoints = 60;

    public void setLevels(Collection<Level> levels) {
        for(Level level : levels) {
            this.levels.put(level.getLevel(), level);
        }
        maxLevel = Collections.max(this.levels.keySet());
    }

    @Null
    public Level getLevel(int level) {
        return levels.get(level);
    }

    public Level getLevelNonNull(int level) {
        Level resultLevel = levels.get(level);
        if(resultLevel == null) {
            log.error("level [{}] not found", level);
            resultLevel = levels.get(getMinLevel());
        }
        return resultLevel;
    }

    public int getMinLevel() {
        return 1;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Map<Integer, Level> getLevels() {
        return levels;
    }

    public int getMaxAvailablePoints(int level) {
        return maxAvailablePoints;
    }

    public void setMaxAvailablePoints(int maxAvailablePoints) {
        this.maxAvailablePoints = maxAvailablePoints;
    }

}
