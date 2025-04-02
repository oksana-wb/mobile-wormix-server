package com.pragmatix.craft.model;

import com.pragmatix.app.settings.ItemRequirements;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.06.12 11:15
 */
public class Recipe extends AbstractRecipe {

    private Recipe baseRecipe;
    private short baseRecipeId;
    private int weaponId;
    private boolean starting = true;

    public Recipe getBaseRecipe() {
        return baseRecipe;
    }

    public void setBaseRecipe(Recipe baseRecipe) {
        this.baseRecipe = baseRecipe;
    }

    public int getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(int weaponId) {
        this.weaponId = weaponId;
    }

    public boolean isStarting() {
        return starting;
    }

    public void setStarting(boolean starting) {
        this.starting = starting;
    }

    public short getBaseRecipeId() {
        return baseRecipeId;
    }

    public void setBaseRecipeId(short baseRecipeId) {
        this.baseRecipeId = baseRecipeId;
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "id=" + id +
                '}';
    }

}
