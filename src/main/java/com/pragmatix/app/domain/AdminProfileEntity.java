package com.pragmatix.app.domain;

import com.pragmatix.admin.model.AdminProfile;
import com.pragmatix.gameapp.common.Identifiable;

import java.io.Serializable;
import java.util.Date;

/**
 * Порфайл админа
 * <p>
 * User: denis
 * Date: 09.03.2011
 * Time: 15:48:43
 */
public class AdminProfileEntity implements Identifiable<Long>, Serializable {

    /**
     * id записи
     */
    private Long id;

    /**
     * логин админа
     */
    private String login;

    /**
     * логин админа
     */
    private String password;

    /**
     * имя
     */
    private String name;

    /**
     * права которые доступны админу
     */
    private byte role;

    /**
     * дата создания
     */
    private Date createDate;

    /**
     * кем был создан
     */
    private String createdBy;

    public AdminProfileEntity() {
    }

    public AdminProfileEntity(AdminProfile profile) {
        this.login = profile.getLogin();
        this.password = profile.getPassword();
        this.role = profile.getRole().getType();
        this.name = profile.getName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public byte getRole() {
        return role;
    }

    public void setRole(byte role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AdminProfileEntity{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                ", role=" + role +
                ", createDate=" + createDate +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }

}
