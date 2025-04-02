package com.pragmatix.steam.dao;

import com.pragmatix.gameapp.common.Identifiable;

/**
 * Author: Vladimir
 * Date: 31.10.13 16:45
 */
public abstract class DomainObject<ID> implements Identifiable<ID>, Cloneable {
    /**
     * идентификатор объекта
     */
    private ID id;

    @Override
    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        if (!id.equals(this.id)) {
            if (this.id != null) {
                throw new IllegalStateException("Id is already set " + this);
            }
            this.id = id;
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || getClass().equals(obj.getClass()) && (this.id.equals(((DomainObject) obj).id));
    }
}
