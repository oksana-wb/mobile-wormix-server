package com.pragmatix.app.settings;

import com.pragmatix.craft.services.CraftService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.12.2015 17:11
 */
public class GenericAwardFactory extends GenericAwardProducer implements GenericAwardContainer {

    private String name;
    /**
     * вероятности получения той или иной награды
     */
    private int[][] resultAwardMassMap;

    private String resultAwardMass;

    private Map<Integer, GenericAward> awardsMap = new HashMap<>();

    private boolean addExperience = false;

    public void init() {
        String[] awardsMass = resultAwardMass.split(" ");
        int allAwardsMass = 0;
        resultAwardMassMap = new int[awardsMass.length + 1][2];
        for(int i = 0; i < awardsMass.length; i++) {
            String s = awardsMass[i].split(":")[0];
            Integer awardId = Integer.valueOf(s);
            if(!awardsMap.containsKey(awardId))
                throw new IllegalStateException("GenericAwardFactory(" + name + "): awardsMap not contains item with id " + awardId);

            int reagentMass = Integer.parseInt(awardsMass[i].split(":")[1]);
            allAwardsMass += reagentMass;
            resultAwardMassMap[i + 1][0] = awardId;
            resultAwardMassMap[i + 1][1] = reagentMass;
        }
        // по нулевому индексу сумму всех весов
        resultAwardMassMap[0][1] = allAwardsMass;
    }

    @Override
    public GenericAward getGenericAward() {
        int resultItemId = CraftService.rollDice(resultAwardMassMap, getRandomAwardSeed());
        return awardsMap.get(resultItemId);
    }

    public int[][] getResultAwardMassMap() {
        return resultAwardMassMap;
    }

    public void setResultAwardMassMap(int[][] resultAwardMassMap) {
        this.resultAwardMassMap = resultAwardMassMap;
    }

    public String getResultAwardMass() {
        return resultAwardMass;
    }

    public void setResultAwardMass(String resultAwardMass) {
        this.resultAwardMass = resultAwardMass;
    }

    public Map<Integer, GenericAward> getAwardsMap() {
        return awardsMap;
    }

    public void setAwards(List<GenericAward> awards) {
        this.awardsMap = awards.stream()
                // начисляем опыт по схеме если надо
                .peek(i -> i.setAddExperience(addExperience))
                .collect(Collectors.toMap(GenericAwardProducer::getKey, i -> i));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Collection<GenericAward> getGenericAwards() {
        return awardsMap.values();
    }

    public boolean isAddExperience() {
        return addExperience;
    }

    public void setAddExperience(boolean addExperience) {
        this.addExperience = addExperience;
    }
}
