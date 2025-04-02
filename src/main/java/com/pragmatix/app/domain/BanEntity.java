package com.pragmatix.app.domain;

import com.pragmatix.app.model.BanItem;
import com.pragmatix.gameapp.common.Identifiable;

import javax.persistence.Transient;
import java.util.Date;

/**
 * User: denis
 * Date: 07.11.2010
 * Time: 17:57:09
 */
public class BanEntity implements Identifiable<Long> {

    /**
     * id записи
     */
    private Long id;

    /**
     * id пофайла
     */
    private Long profileId;

    /**
     * время
     */
    private Date date;

    /**
     * время действия бана
     */
    private Date endDate;

    /**
     * тип бана
     */
    private int type;

    protected static final int MAX_NOTE_SIZE = 255;

    /**
     * краткая информация о бане от админа
     */
    private String note;

    /**
     * логин админа который отправил игрока в бан
     */
    private String admin;
    /**
     * профайл пользователя который забанен
     */
    private UserProfileEntity userProfile;

    private String attachments;

    public BanEntity() {
    }


    public BanEntity(BanItem banItem) {
        if(banItem.getId() > 0) {
            setId(banItem.getId());
        }
        setProfileId(banItem.getProfileId());
        setDate(new Date(banItem.getStartDate()));
        setType(banItem.getBanReason());
        setNote(banItem.getNote());
        if(banItem.getEndDate() != null) {
            setEndDate(new Date(banItem.getEndDate()));
        }
        setAdmin(banItem.getAdmin());
        setAttachments(banItem.getAttachments());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getNote() {
        return note;
    }

    @Transient
    public Date getStartDate() {
        return date;
    }

    public void setStartDate(Date date) {
        this.date = date;
    }

    public void setNote(String note) {
        if(note == null) {
            note = "";
        } else if(note.length() > MAX_NOTE_SIZE) {
            note = note.substring(0, MAX_NOTE_SIZE - 3) + "...";
        }
        this.note = note;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getAdmin() {
        return admin;
    }

    @Transient
    public int getBanReason() {
        return type;
    }

    public void setBanReason(int type) {
        this.type = type;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public UserProfileEntity getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfileEntity userProfile) {
        this.userProfile = userProfile;
    }

    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    @Override
    public String toString() {
        return "BanEntity{" +
                "profileId=" + profileId +
                ", date=" + date +
                ", endDate=" + endDate +
                ", type=" + type +
                ", note='" + note + '\'' +
                ", admin='" + admin + '\'' +
                ", attachments='" + attachments + '\'' +
                '}';
    }
}
