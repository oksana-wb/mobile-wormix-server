package com.pragmatix.app.domain;

import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.gameapp.common.Identifiable;

import javax.persistence.*;
import java.time.LocalDateTime;

public class BossBattleExtraRewardEntity implements Identifiable<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime start;

    private LocalDateTime finish;    
    
    @Column(updatable = false)
    private short missionId;

    @Column(columnDefinition = "smallint NOT NULL DEFAULT 1")
    private short levelFrom = 1;

    @Column(columnDefinition = "smallint NOT NULL DEFAULT 30")
    private short levelTo = 30;

    @Column(columnDefinition = "boolean NOT NULL DEFAULT false")
    private boolean archive;

    @Column(columnDefinition = "integer NOT NULL DEFAULT 100")
    private int chance = 100;

    @Column(columnDefinition = "integer NOT NULL DEFAULT 0")
    private int realMoney = 0;

    @Column(columnDefinition = "integer NOT NULL DEFAULT 0")
    private int money = 0;

    @Column(columnDefinition = "integer NOT NULL DEFAULT 0")
    private int reaction = 0;

    @Column(columnDefinition = "character varying NOT NULL DEFAULT ''")
    private String reagents = "";

    @Column(columnDefinition = "character varying NOT NULL DEFAULT ''")    
    private String weapons = "";

    @Column(columnDefinition = "character varying NOT NULL DEFAULT ''")    
    private String stuff = "";

    @Transient
    public GenericAward reward;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getFinish() {
        return finish;
    }

    public void setFinish(LocalDateTime finish) {
        this.finish = finish;
    }

    public short getMissionId() {
        return missionId;
    }

    public void setMissionId(short missionId) {
        this.missionId = missionId;
    }

    public short getLevelFrom() {
        return levelFrom;
    }

    public void setLevelFrom(short levelFrom) {
        this.levelFrom = levelFrom;
    }

    public short getLevelTo() {
        return levelTo;
    }

    public void setLevelTo(short levelTo) {
        this.levelTo = levelTo;
    }

    public boolean isArchive() {
        return archive;
    }

    public void setArchive(boolean archive) {
        this.archive = archive;
    }

    public int getChance() {
        return chance;
    }

    public void setChance(int chance) {
        this.chance = chance;
    }

    public int getRealMoney() {
        return realMoney;
    }

    public void setRealMoney(int realMoney) {
        this.realMoney = realMoney;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = money;
    }

    public int getReaction() {
        return reaction;
    }

    public void setReaction(int reaction) {
        this.reaction = reaction;
    }

    public String getReagents() {
        return reagents;
    }

    public void setReagents(String reagents) {
        this.reagents = reagents;
    }

    public String getWeapons() {
        return weapons;
    }

    public void setWeapons(String weapons) {
        this.weapons = weapons;
    }

    public String getStuff() {
        return stuff;
    }

    public void setStuff(String stuff) {
        this.stuff = stuff;
    }
}
