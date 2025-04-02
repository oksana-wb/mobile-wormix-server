package com.pragmatix.steam.dao;

import com.pragmatix.gameapp.common.Identifiable;

import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * Author: Vladimir
 * Date: 15.12.11 8:46
 */
@MappedSuperclass
public abstract class Entity<ID> implements Identifiable<ID>, Serializable {

    protected ID id;

    public void setId(ID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            return id != null && id.equals(((Entity) o).getId());
        }
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + "]";
    }

}
