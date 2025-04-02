package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Id;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.07.13 20:10
 */
public class StoreItemEntity implements Identifiable<String> {

    @Id
    private String key;

    @Basic
    @Column(length = 1024*10)
    private String value;

    public StoreItemEntity(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public StoreItemEntity() {
    }

    @Override
    public String getId() {
        return key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
