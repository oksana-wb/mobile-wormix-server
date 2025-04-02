package com.pragmatix.craft.domain;


import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.06.12 14:09
 */
public class ReagentsEntity {

    private final long profileId;

    private final int[] values = initValues();

    private volatile boolean dirty = false;

    private volatile boolean newly = true;

    public ReagentsEntity(long profileId) {
        this.profileId = profileId;
    }

    public static int[] initValues() {
        return new int[Reagent.values()[Reagent.values().length - 1].getIndex() + 1];
    }

    public Map<Reagent, Integer> getReagentValues() {
        return ReagentsEntity.getReagentValues(values);
    }

    public static Map<Reagent, Integer> getReagentValues(int[] values) {
        if(values == null) return Collections.emptyMap();
        Map<Reagent, Integer> result = new TreeMap<>();
        for(Reagent reagent : Reagent.values()) {
            if(values[reagent.getIndex()] != 0)
                result.put(reagent, values[reagent.getIndex()]);
        }
        return result;
    }

    public static Map<Reagent, Integer> getReagentValues(byte[] reagents) {
        Map<Reagent, Integer> result = new TreeMap<>();
        for(byte reagentId : reagents) {
            fillResult(result, reagentId);
        }
        return result;
    }

    private static void fillResult(Map<Reagent, Integer> result, byte reagentId) {
        if(reagentId < 0) return;
        Reagent reagent = Reagent.valueOf(reagentId);
        int count = 0;
        if(result.containsKey(reagent)) {
            count = result.get(reagent);
        }
        result.put(reagent, count + 1);
    }

    public static Map<Reagent, Integer> getReagentValues(List<Byte> reagents) {
        Map<Reagent, Integer> result = new TreeMap<>();
        for(byte reagentId : reagents) {
            fillResult(result, reagentId);
        }
        return result;
    }

    public int getReagentValue(Reagent reagent) {
        return values[reagent.getIndex()];
    }

    public void setReagentValue(Reagent reagent, Integer value) {
        values[reagent.getIndex()] = value != null ? Math.max(0, value) : 0;
    }

    public int getReagentValue(byte reagentIndex) {
        return values[reagentIndex];
    }

    public void setReagentValue(byte reagentIndex, int value) {
        dirty |= getReagentValue(reagentIndex) != value;
        values[reagentIndex] = Math.max(0, value);
    }

    public void addReagentValue(byte reagentIndex, int count) {
        values[reagentIndex] += count;
        dirty |= count != 0;
    }

    public void addReagentValue(Reagent reagent, int count) {
        values[reagent.getIndex()] += count;
        dirty |= count != 0;
    }

    public void clean() {
        for(int i = 0; i < values.length; i++) {
            values[i] = 0;
        }
        dirty = true;
    }

    //====================== Getters and Setters =================================================================================================================================================

    public long getProfileId() {
        return profileId;
    }

    public int[] getValues() {
        return values;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isNewly() {
        return newly;
    }

    public void setNewly(boolean newly) {
        this.newly = newly;
    }

    @Override
    public String toString() {
        Map<Reagent, Integer> reagentsValues = new LinkedHashMap<>();
        for(int i = 0; i < values.length; i++) {
            int value = values[i];
            if(value > 0)
                reagentsValues.put(Reagent.valueOf(i), value);
        }
        return "reagents: " + reagentsValues;
    }

}
