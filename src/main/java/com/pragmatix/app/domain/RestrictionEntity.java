package com.pragmatix.app.domain;

import com.pragmatix.app.model.RestrictionItem;
import com.pragmatix.gameapp.common.Identifiable;

import java.util.Date;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 01.06.2016 18:36
 *         <p>
 * @see com.pragmatix.app.model.RestrictionItem
 */
public class RestrictionEntity implements Identifiable<Integer> {

    /**
     * id записи
     */
    private Integer id;

    /**
     * id профиля
     */
    private Long profileId;

    /**
     * какие именно возможности у игрока заблокированы
     */
    private short blocks;

    /**
     * дата начала действия ограничения
     */
    private Date startDate;

    /**
     * окончание действия ограничения
     */
    private Date endDate;

    /**
     * причина: один из BanType или другой, дополнительный код
     * @see com.pragmatix.app.common.BanType
     */
    private int reason;

    /**
     * История изменений в json
     */
    private String history;


    public RestrictionEntity() {
    }

    public RestrictionEntity(RestrictionItem item) {
        if (item.getId() > 0) {
            setId(item.getId());
        }
        setProfileId(item.getProfileId());
        setStartDate(new Date(item.getStartDate()));
        if (item.getEndDate() != null) {
            setEndDate(new Date(item.getEndDate()));
        }
        setReason(item.getReason());
        setBlocks(item.getBlocks());
        setHistory(item.getHistoryAsJson());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public short getBlocks() {
        return blocks;
    }

    public void setBlocks(short blocks) {
        this.blocks = blocks;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {
        this.history = history;
    }

    @Override
    public String toString() {
        return "RestrictionEntity{" +
                "id=" + id +
                ", profileId=" + profileId +
                ", blocks=" + Integer.toHexString(blocks) +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", reason=" + reason +
                ", history='" + history + '\'' +
                '}';
    }
}
