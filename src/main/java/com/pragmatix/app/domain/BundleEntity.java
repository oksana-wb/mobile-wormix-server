package com.pragmatix.app.domain;

import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.common.Identifiable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

public class BundleEntity implements Identifiable<Long> {

    private Long id;

    private String code;

    private int sortOrder;

    private Date start;

    private Date finish;

    private int discount;

    private float votes;

    private Date createDate;

    private Date updateDate;

    public boolean disabled;

    private String skins = "";

    private String races = "";

    private String items = "";

    public BundleEntity() {
    }

    public BundleEntity(String code, int sortOrder, Date start, Date finish, int discount, float votes, String races, String skins, String items, boolean disabled) {
        this.code = code;
        this.sortOrder = sortOrder;
        this.start = start;
        this.finish = finish;
        this.discount = discount;
        this.votes = votes;
        this.races = races;
        this.skins = skins;
        this.items = items;
        this.disabled = disabled;
        this.createDate = new Date();
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
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Column(nullable = false, columnDefinition = "integer NOT NULL default 0")
    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int order) {
        this.sortOrder = order;
    }

    @Column(nullable = true)
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

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    public float getVotes() {
        return votes;
    }

    public void setVotes(float votes) {
        this.votes = votes;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getSkins() {
        return skins;
    }

    public void setSkins(String skins) {
        this.skins = skins;
    }

    public String getRaces() {
        return races;
    }

    public void setRaces(String races) {
        this.races = races;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        BundleEntity that = (BundleEntity) o;

        if(!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "BundleEntity{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", start=" + AppUtils.formatDate(start) +
                ", finish=" + AppUtils.formatDate(finish) +
                ", discount=" + discount +
                ", votes=" + votes +
                ", disabled=" + disabled +
                ", races=" + races +
                ", skins=" + skins +
                ", items='" + items + '\'' +
                ", createDate=" + AppUtils.formatDate(createDate) +
                ", updateDate=" + AppUtils.formatDate(updateDate) +
                '}';
    }
}
