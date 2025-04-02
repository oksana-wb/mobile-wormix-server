package com.pragmatix.app.model;

import com.pragmatix.app.domain.BanEntity;

/**
 * Author: Oksana Shevchenko
 * Date: 19.12.2010
 * Time: 7:02:02
 */
public class BanItem {

    /**
     * id в базе
     */
    private long id;

    /**
     * id пофайла
     */
    private Long profileId;

    /**
     * время попадания в бан
     */
    private long startDate;

    /**
     * тип бана
     */
    private int banReason;

    /**
     * время действия бана
     */
    private Long endDate;

    /**
     * краткая информация о бане от админа
     */
    private String note;

    /**
     * логин админа который отправил игрока в бан
     */
    private String admin;

    /**
     * ссылка на доказательство (изображение)
     */
    private String attachments;

    public BanItem(Long profileId, long startDate, int banReason, Long endDate, String note, String admin, String attachments) {
        this.profileId = profileId;
        this.startDate = startDate;
        this.banReason = banReason;
        this.endDate = endDate;
        this.note = note;
        this.admin = admin;
        this.attachments = attachments;
    }

    public BanItem(BanEntity e) {
        this.id = e.getId();
        this.profileId = e.getProfileId();
        this.startDate = e.getDate().getTime();
        this.banReason = e.getType();
        if(e.getEndDate() != null) {
            this.endDate = e.getEndDate().getTime();
        }
        this.note = e.getNote();
        this.admin = e.getAdmin();
        this.attachments = e.getAttachments();
    }

    public Long getEndDate() {
        return endDate;
    }

    public void setEndDate(Long endDate) {
        this.endDate = endDate;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public int getBanReason() {
        return banReason;
    }

    public void setBanReason(int banReason) {
        this.banReason = banReason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        BanItem banItem = (BanItem) o;

        if(id != banItem.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        return result;
    }


    @Override
    public String toString() {
        return "BanItem{" +
                "id=" + id +
                ", profileId=" + profileId +
                ", startDate=" + startDate +
                ", banReason=" + banReason +
                ", endDate=" + endDate +
                ", note='" + note + '\'' +
                ", admin='" + admin + '\'' +
                ", attachments='" + attachments + '\'' +
                '}';
    }
}
