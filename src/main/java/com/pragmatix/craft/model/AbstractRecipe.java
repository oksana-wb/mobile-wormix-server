package com.pragmatix.craft.model;

import com.pragmatix.app.settings.ItemRequirements;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 09.07.13 17:57
 */
public abstract class AbstractRecipe extends ItemRequirements {

    protected short id;
    protected String name;
    protected Map<Byte, Integer> reagents;

    public short getId() {
        return id;
    }

    public void setId(short id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<Byte, Integer> getReagentsMap() {
        return reagents;
    }

    public void setReagents(String reagentsStr) {
        reagents = getReagentsMap(reagentsStr);
    }

    protected Map<Byte, Integer> getReagentsMap(String reagentsStr) {
        Map<Byte, Integer> reagents = new HashMap<>();
        // "24:2 25:1"
        if(!reagentsStr.isEmpty()) {
            String[] ss = reagentsStr.split(" ");
            for(String s : ss) {
                String[] item = s.split(":");
                reagents.put(Byte.parseByte(item[0]), Integer.parseInt(item[1]));
            }
        }
        return reagents;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        Recipe recipe = (Recipe) o;

        if(id != recipe.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) id;
    }
}
