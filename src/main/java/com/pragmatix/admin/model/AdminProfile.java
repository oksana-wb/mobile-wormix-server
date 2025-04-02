package com.pragmatix.admin.model;

import com.pragmatix.admin.common.RoleType;
import com.pragmatix.app.domain.AdminProfileEntity;
import com.pragmatix.sessions.IUser;

import java.util.Date;

/**
 * профайл  админа
 * User: denis
 * Date: 28.01.2011
 * Time: 13:54:21
 */
public class AdminProfile implements IUser {

   /**
     * логин админа
     */
    private String login;

    /**
     * логин админа
     */
    private String password;

    /**
     * права которые доступны админу
     */
    private RoleType role;

    private byte socialId;
    
    /**
     * имя
     */
    private String name;    

    public AdminProfile(AdminProfileEntity entity) {
        this.login = entity.getLogin();
        this.password = entity.getPassword();
        this.role = RoleType.getRole(entity.getRole());
        this.name = entity.getName();
    }

    public AdminProfile(String login, String password, RoleType role, String name) {
        this.login = login;
        this.password = password;
        this.role = role;
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "admin:" + login;
    }

    @Override
    public Object getId() {
        return login;
    }

    @Override
    public byte getSocialId() {
        return socialId;
    }

    public void setSocialId(byte socialId) {
        this.socialId = socialId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
