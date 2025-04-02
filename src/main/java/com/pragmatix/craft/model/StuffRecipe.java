package com.pragmatix.craft.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 09.07.13 17:45
 */
public class StuffRecipe extends AbstractRecipe {

    /**
     * должен пройти босса
     */
    private short missionId;
    /**
     * базовые предметы
     */
    private Set<Short> baseStuffsSet;
    /**
     * варианты итогового предмета
     */
    private Set<Short> resultsStuffsSet = new HashSet<>();
    /**
     * вероятности получения той или иной модификации итогового предмета
     */
    private int[][] resultStuffMassMap;

    private String resultStuffMass;

    private int recraftMoney;

    private int recraftRealMoney;

    private Set<Short> recraftBaseStuffsSet = new HashSet<>();;

    private Map<Byte, Integer> recraftReagents;

    public void setResultStuffsMass(String resultStuffMass) {
        this.resultStuffMass = resultStuffMass;
    }

    public void setBaseStuffs(String stuffsSetStr) {
        baseStuffsSet = getBaseStuffsSet(stuffsSetStr);
    }

    public void setRecraftBaseStuffs(String baseStuffsSet) {
        recraftBaseStuffsSet = getBaseStuffsSet(baseStuffsSet);
    }

    public Set<Short> getBaseStuffsSet(String stuffsSetStr) {
        Set<Short> baseStuffsSet = new HashSet<>();
        stuffsSetStr = stuffsSetStr.trim();
        if(stuffsSetStr.isEmpty()) return baseStuffsSet;
        String[] ss = stuffsSetStr.split(" ");
        for(String s : ss) {
            baseStuffsSet.add(Short.valueOf(s));
        }
        return baseStuffsSet;
    }

    public void setRecraftReagents(String reagentsStr) {
        recraftReagents = getReagentsMap(reagentsStr);
    }

    public Map<Byte, Integer> getRecraftReagentsMap() {
        return recraftReagents;
    }

    public int[][] getResultStuffMassMap() {
        return resultStuffMassMap;
    }

    public short getMissionId() {
        return missionId;
    }

    public void setMissionId(short missionId) {
        this.missionId = missionId;
    }

    public void setResultStuffMassMap(int[][] resultStuffMassMap) {
        this.resultStuffMassMap = resultStuffMassMap;
    }

    public String getResultStuffMass() {
        return resultStuffMass;
    }

    public void setResultStuffMass(String resultStuffMass) {
        this.resultStuffMass = resultStuffMass;
    }

    public Set<Short> getResultsStuffsSet() {
        return resultsStuffsSet;
    }

    public Set<Short> getBaseStuffsSet() {
        return baseStuffsSet;
    }

    public Set<Short> getRecraftBaseStuffsSet() {
        return recraftBaseStuffsSet;
    }

    public int getRecraftMoney() {
        return recraftMoney;
    }

    public void setRecraftMoney(int recraftMoney) {
        this.recraftMoney = recraftMoney;
    }

    public int getRecraftRealMoney() {
        return recraftRealMoney;
    }

    public void setRecraftRealMoney(int recraftRealMoney) {
        this.recraftRealMoney = recraftRealMoney;
    }

    @Override
    public String toString() {
        return "StuffRecipe{" +
                "id=" + id +
                '}';
    }

}
