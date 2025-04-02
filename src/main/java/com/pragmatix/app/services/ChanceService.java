package com.pragmatix.app.services;

import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.events.IProfileEvent;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.app.settings.RandomAward;

import javax.validation.constraints.Null;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Класс для работы с вероятностными событиями
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 */
public class ChanceService {

    /**
     * список событий с их вероятностями
     */
    private List<ChanceStructure> structureList = new CopyOnWriteArrayList<ChanceStructure>();
    /**
     * служебная переменная
     */
    private int i = 0;
    /**
     * статистика по событиям
     */
    private Map<Object, Integer> events = new LinkedHashMap<>();
    /**
     * собирать ли статиснику по событиям
     */
    private boolean collectStatistic = false;

    public ChanceService(boolean collectStatistic) {
        this.collectStatistic = collectStatistic;
    }

    public ChanceService() {
    }

    /**
     * @param award       награда с указанной вероятностью
     */
    public void addRandomAward(RandomAward award) {
        int probability = award.getProbability();
        if(i + probability > 100) {
            throw new IllegalArgumentException("sum of probabilities > 100% !");
        }
        structureList.add(new ChanceStructure(i, i + probability, award));
        i += probability;
        if(collectStatistic) {
            events.put(award, 0);
        }
    }

    /**
     * метод вернет награду согласно заложенных вероятностей
     */
    @Null
    public GenericAward getAward() {
        int rnd = (int) (Math.random() * 100);
        for(ChanceStructure chanceStructure : structureList) {
            if(rnd >= chanceStructure.min && rnd < chanceStructure.max) {
                GenericAward result = chanceStructure.award;
                if(collectStatistic) {
                    int runCount = events.get(result);
                    events.put(result, runCount + 1);
                }
                return result;
            }
        }
        return null;
    }

    class ChanceStructure {
        int min;
        int max;
        GenericAward award;

        ChanceStructure(int min, int max, GenericAward award) {
            this.min = min;
            this.max = max;
            this.award = award;
        }

        @Override
        public String toString() {
            return "ChanceStructure{" +
                    "min=" + min +
                    ", max=" + max +
                    ", award=" + award +
                    '}';
        }
    }

}
