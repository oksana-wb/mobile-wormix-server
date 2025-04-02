package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import javax.persistence.*;
import javax.validation.constraints.Null;
import java.util.Arrays;

/**
 * Для хранения конфигурации рюкзака в БД
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.04.13 11:09
 */
public class BackpackConfEntity implements Identifiable<Long> {

    @Id
    private long profileId;
    /**
     * конфигурация рюкзака #1
     */
    @Basic
    private short[] config;
    /**
     * конфигурация рюкзака #2
     */
    @Basic
    private short[] config2;
    /**
     * конфигурация рюкзака #3
     */
    @Basic
    private short[] config3;
    /**
     * активная конфигурация рюкзака
     */
    @Column(nullable = false, columnDefinition = "smallint NOT NULL default 1")
    private byte activeConfig;
    /**
     * конфигурация раскладки
     */
    @Basic
    private short[] hotkeys;
    /**
     * сезонные результаты
     */
    @Column(insertable = false, updatable = false, columnDefinition = "bytea NOT NULL DEFAULT ''::bytea")
    private byte[] seasonsBestRank;

    @Transient
    private volatile boolean dirty = false;

    @Transient
    private volatile boolean newly = false;

    public BackpackConfEntity() {
    }

    public BackpackConfEntity(long profileId) {
        this.profileId = profileId;
        this.newly = true;
    }

    public BackpackConfEntity(long profileId, @Null short[] config1, @Null short[] config2, @Null short[] config3, @Null short[] hotkeys, byte activeConfig) {
        this.profileId = profileId;
        this.config = config1;
        this.config2 = config2;
        this.config3 = config3;
        this.hotkeys = hotkeys;
        this.activeConfig = activeConfig;
        this.newly = true;
    }

    //====================== Getters and Setters =================================================================================================================================================

    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }

    public long getProfileId() {
        return profileId;
    }

    public short[] getConfig() {
        return config;
    }

    public void setConfig(short[] backpackConf) {
        this.config = backpackConf;
    }

    public short[] getConfig2() {
        return config2;
    }

    public void setConfig2(short[] config2) {
        this.config2 = config2;
    }

    public short[] getConfig3() {
        return config3;
    }

    public void setConfig3(short[] config3) {
        this.config3 = config3;
    }

    public byte getActiveConfig() {
        return activeConfig;
    }

    public void setActiveConfig(byte activeConfig) {
        this.activeConfig = activeConfig;
    }

    public short[] getHotkeys() {
        return hotkeys;
    }

    public void setHotkeys(short[] hotkeys) {
        this.hotkeys = hotkeys;
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

    public byte[] getSeasonsBestRank() {
        return seasonsBestRank;
    }

    public void setSeasonsBestRank(byte[] seasonsBestRank) {
        this.seasonsBestRank = seasonsBestRank;
    }

    @Override
    public Long getId() {
        return profileId;
    }

    @Override
    public String toString() {
        return "BackpackConf{" +
                "hotkeys=" + Arrays.toString(hotkeys) +
                ", config1=" + Arrays.toString(config) +
                ", config2=" + Arrays.toString(config2) +
                ", config3=" + Arrays.toString(config3) +
                ", activeConfig=" + activeConfig +
                ", seasonsBestRank=" + Arrays.toString(seasonsBestRank) +
                '}';
    }

}
