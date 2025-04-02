package com.pragmatix.app.settings;

import com.pragmatix.app.common.ItemCheck;
import com.pragmatix.craft.domain.Reagent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 04.05.11 12:57
 */
public class ItemRequirements implements IItemRequirements {

    public int needMoney = ItemCheck.EMPTY_PRICE;

    public int needRealMoney = ItemCheck.EMPTY_PRICE;

    public int needLevel = 1;

    public int needBattles;

    public Map<Reagent, Integer> needReagents = Collections.emptyMap();

    public ItemRequirements() {
    }

    public ItemRequirements(int needMoney, int needRealMoney, int needLevel) {
        this.needMoney = needMoney;
        this.needRealMoney = needRealMoney;
        this.needLevel = needLevel;
    }

    public ItemRequirements(int needMoney, int needRealMoney, int needLevel, int needBattles, Map<Reagent, Integer> needReagents) {
        this.needMoney = needMoney;
        this.needRealMoney = needRealMoney;
        this.needLevel = needLevel;
        this.needBattles = needBattles;
        this.needReagents = needReagents;
    }

    public ItemRequirements cloneSetReagents(Map<Reagent, Integer> needReagents) {
        return new ItemRequirements(needMoney, needRealMoney, needLevel, needBattles, new HashMap<>(needReagents));
    }

    @Override
    public int needMoney() {
        return needMoney;
    }

    @Override
    public int needRealMoney() {
        return needRealMoney;
    }

    @Override
    public int needLevel() {
        return needLevel;
    }

    @Override
    public int needBattles() {
        return needBattles;
    }

    @Override
    public Map<Reagent, Integer> needReagents() {
        return needReagents;
    }

    public void setNeedMoney(int needMoney) {
        this.needMoney = needMoney;
    }

    public void setNeedRealMoney(int needRealMoney) {
        this.needRealMoney = needRealMoney;
    }

    public void setNeedLevel(int needLevel) {
        this.needLevel = needLevel;
    }

    public void setNeedBattles(int needBattles) {
        this.needBattles = needBattles;
    }

    public void setNeedReagents(List<String> needReagents) {
        this.needReagents = needReagents.stream()
                .map(s -> s.split(":"))
                .collect(Collectors.toMap(ss -> Reagent.valueOf(Integer.parseInt(ss[0])), ss -> Integer.parseInt(ss[1])));
    }

    public boolean isEmpty(){
        return
                (needMoney == ItemCheck.EMPTY_PRICE || needMoney == 0)
                && (needRealMoney == ItemCheck.EMPTY_PRICE || needRealMoney == 0)
                && (needBattles == 0)
                && (needReagents.isEmpty())
                ;
    }
}
