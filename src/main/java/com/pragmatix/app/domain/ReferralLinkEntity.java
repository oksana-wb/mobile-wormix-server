package com.pragmatix.app.domain;

import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.gameapp.common.Identifiable;

import javax.persistence.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 16.04.2014 11:20
 */
public class ReferralLinkEntity implements Identifiable<Long> {

    private Long id;

    private String token;

    private Date start;

    private Date finish;

    private int limit = 0;

    public AtomicInteger visitors = new AtomicInteger(0);

    private int ruby = 0;

    private int fuzy = 0;

    private int battles = 0;

    private int reaction = 0;

    private String reagents = "";

    private String weapons = "";

    private int experience = 0;

    private int bossToken = 0;

    private int wagerToken = 0;

    private GenericAward genericAward;

    public ReferralLinkEntity() {
    }

    public ReferralLinkEntity(String token, Date start, Date finish, int limit, int ruby, int fuzy, int battles, int reaction, String reagents, String weapons,
                              int experience, int bossToken, int wagerToken) {
        this.token = token;
        this.start = start;
        this.finish = finish;
        this.limit = limit;
        this.ruby = ruby;
        this.fuzy = fuzy;
        this.battles = battles;
        this.reaction = reaction;
        this.reagents = reagents;
        this.weapons = weapons;
        this.experience = experience;
        this.bossToken = bossToken;
        this.wagerToken = wagerToken;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(nullable = false)
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Column(nullable = false)
    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    @Column(nullable = true)
    public Date getFinish() {
        return finish;
    }

    public void setFinish(Date finish) {
        this.finish = finish;
    }

    @Column(name = "\"limit\"")
    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getRuby() {
        return ruby;
    }

    public void setRuby(int ruby) {
        this.ruby = ruby;
    }

    public int getFuzy() {
        return fuzy;
    }

    public void setFuzy(int fuzy) {
        this.fuzy = fuzy;
    }

    public int getBattles() {
        return battles;
    }

    public void setBattles(int battles) {
        this.battles = battles;
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

    public int getVisitors() {
        return visitors.get();
    }

    public void setVisitors(int visitors) {
        this.visitors = new AtomicInteger(visitors);
    }

    @Column(columnDefinition = "INTEGER NOT NULL DEFAULT 0")
    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    @Column(columnDefinition = "INTEGER NOT NULL DEFAULT 0")
    public int getBossToken() {
        return bossToken;
    }

    public void setBossToken(int bossToken) {
        this.bossToken = bossToken;
    }

    @Column(columnDefinition = "INTEGER NOT NULL DEFAULT 0")
    public int getWagerToken() {
        return wagerToken;
    }

    public void setWagerToken(int wagerToken) {
        this.wagerToken = wagerToken;
    }

    @Transient
    public GenericAward getGenericAward() {
        return genericAward;
    }

    public void setGenericAward(GenericAward genericAward) {
        this.genericAward = genericAward;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        ReferralLinkEntity that = (ReferralLinkEntity) o;

        if(!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


    @Override
    public String toString() {
        return "ReferralLinkEntity{" +
                "id=" + id +
                ", token='" + token + '\'' +
                ", start=" + start +
                ", finish=" + finish +
                ", limit=" + limit +
                ", visitors=" + visitors +
                ", ruby=" + ruby +
                ", fuzy=" + fuzy +
                ", battles=" + battles +
                ", reaction=" + reaction +
                ", reagents='" + reagents + '\'' +
                ", weapons='" + weapons + '\'' +
                ", experience=" + experience +
                ", bossToken=" + bossToken +
                ", wagerToken=" + wagerToken +
                '}';
    }
}
