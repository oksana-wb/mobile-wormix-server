package com.pragmatix.app.messages.structures;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Structure;

/**
 * Когда нужно премировать игрока
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 01.10.12 10:21
 */
@Structure
public class GenericAwardStructure {
    /**
     * что за награда
     */
    public AwardKindEnum awardKind;
    /**
     * количество
     */
    public int count;
    /**
     * id элемента в случаях: оружие, шапка, реагент
     */
    public int itemId = -1;

    @Ignore
    public int boostFactor;

    public GenericAwardStructure() {
    }

    public GenericAwardStructure(AwardKindEnum awardKind, int count, int itemId) {
        this.awardKind = awardKind;
        this.count = count;
        this.itemId = itemId;
    }

    public GenericAwardStructure(AwardKindEnum awardKind, int count) {
        this.awardKind = awardKind;
        this.count = count;
    }

    public GenericAwardStructure setBoostFactor(int boostFactor){
        this.boostFactor = boostFactor;
        return this;
    }

    @Override
    public String toString() {
        return "{" + awardKind.name() +
                (itemId > 0 || (itemId == 0 && awardKind == AwardKindEnum.REAGENT) ? "(" + itemId + ")" : "") +
                ": " + (awardKind == AwardKindEnum.TEMPORARY_STUFF ? AppUtils.formatDateInSeconds(count) : count) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        GenericAwardStructure that = (GenericAwardStructure) o;

        if(itemId != that.itemId) return false;
        if(awardKind != that.awardKind) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = awardKind.hashCode();
        result = 31 * result + itemId;
        return result;
    }

    public void setAwardKind(AwardKindEnum awardKind) {
        this.awardKind = awardKind;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

}
