package com.pragmatix.steam.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Vladimir
 * Date: 15.12.11 13:37
 */

public abstract strictfp class JpaDAOBase<ID, ENTITY extends Entity<ID>> {

    public EntityManager entityManager;

    public Logger logger = LoggerFactory.getLogger(getClass());

    public abstract Class<ENTITY> getEntityClass();

    public String getEntityClassName() {
        return getEntityClass().getName();
    }

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Map<ID, ENTITY> loadEntitiesByIds(ID[] ids) {
        QueryBuilder queryBuilder = new QueryBuilder();

        queryBuilder.append("SELECT e FROM ").append(getEntityClassName()).append(" e\nWHERE e.id IN (");

        int i = 0;

        for (ID id : ids) {
            if (i++ > 0) {
                queryBuilder.append(", ");
            }
            queryBuilder.appendParameter("id_" + i, id);
        }

        queryBuilder.append(")\nORDER BY e.id");

        Map<ID, ENTITY> res = new HashMap<>();

        List<ENTITY> entities = loadEntities(queryBuilder, 0, 0);

        for (ENTITY entity : entities) {
            res.put(entity.getId(), entity);
        }

        return res;
    }

    public ENTITY findEntityById(ID id) {
        return entityManager.find(getEntityClass(), id);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public ENTITY insertEntity(ENTITY entity) {
        entityManager.persist(entity);

        return entity;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public ENTITY updateEntity(ENTITY entity) {
        if (!entityManager.contains(entity)) {
            entity = entityManager.merge(entity);
        }

        return entity;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean delete(ID id) {
        Query query = entityManager.createQuery(
                "DELETE FROM " + getEntityClassName() + " e\nWHERE e.id=:id");
        query.setParameter("id", id);

        return query.executeUpdate() > 0;
    }
    public ENTITY findEntityNQ(String queryName, String[] paramNames, Object[] paramValues) {
        List<ENTITY> entities = loadEntitiesNQ(queryName, paramNames, paramValues, 0, 1);

        return entities.size() > 0 ? entities.get(0) : null;
    }

    public List<ENTITY> loadEntitiesNQ(String queryName, String[] paramNames, Object[] paramValues, int offset, int limit) {
        return loadEntitiesNQ(getEntityClass(), queryName, paramNames, paramValues, offset, limit);
    }

    public <T extends Entity> List<T> loadEntitiesNQ(Class<T> tClass, String queryName, String[] paramNames, Object[] paramValues, int offset, int limit) {

        javax.persistence.TypedQuery<T> query = entityManager.createNamedQuery(queryName, tClass);

        if (paramNames != null) {
            int i = 0;

            for (String paramName : paramNames) {
                query.setParameter(paramName, paramValues[i++]);
            }
        }

        if (offset > 0) {
            query.setFirstResult(offset);
        }

        if (limit > 0) {
            query.setMaxResults(limit);
        }

        return query.getResultList();
    }

    public List<Object> loadRows(String querySql, String[] paramNames, Object[] paramValues, int offset, int limit) {

        Query query = entityManager.createQuery(querySql);

        if (paramNames != null) {
            int i = 0;

            for (String paramName : paramNames) {
                query.setParameter(paramName, paramValues[i++]);
            }
        }

        if (offset > 0) {
            query.setFirstResult(offset);
        }

        if (limit > 0) {
            query.setMaxResults(limit);
        }

        return query.getResultList();
    }

    public List<Object[]> loadRowsNQ(String queryName, String[] paramNames, Object[] paramValues, int offset, int limit) {

        Query query = entityManager.createNamedQuery(queryName);

        if (paramNames != null) {
            int i = 0;

            for (String paramName : paramNames) {
                query.setParameter(paramName, paramValues[i++]);
            }
        }

        if (offset > 0) {
            query.setFirstResult(offset);
        }

        if (limit > 0) {
            query.setMaxResults(limit);
        }

        return query.getResultList();
    }

    public int executeUpdateNQ(String queryName, String[] paramNames, Object[] paramValues) {
        Query query = entityManager.createNamedQuery(queryName);

        if (paramNames != null) {
            int i = 0;

            for (String paramName : paramNames) {
                query.setParameter(paramName, paramValues[i++]);
            }
        }

        return query.executeUpdate();
    }

    public int executeUpdate(String sql, String[] paramNames, Object[] paramValues) {
        Query query = entityManager.createQuery(sql);

        if (paramNames != null) {
            int i = 0;

            for (String paramName : paramNames) {
                query.setParameter(paramName, paramValues[i++]);
            }
        }

        return query.executeUpdate();
    }

    public ENTITY findEntity(QueryBuilder queryBuilder) {
        List<ENTITY> entities = loadEntities(queryBuilder, 0, 1);

        return entities.size() > 0 ? entities.get(0) : null;
    }

    public List<ENTITY> loadEntities(QueryBuilder queryBuilder, int offset, int limit) {
        return loadEntities(queryBuilder.getSql().toString(), queryBuilder.getParamNames(), queryBuilder.getParamValues(),
                offset, limit);
    }

    public List<ENTITY> loadEntities(String sql, List<String> paramNames, List<Object> paramValues, int offset, int limit) {
        javax.persistence.TypedQuery<ENTITY> query = entityManager.createQuery(sql, getEntityClass());

        if (paramNames != null) {
            int i = 0;

            for (String paramName : paramNames) {
                query.setParameter(paramName, paramValues.get(i++));
            }
        }

        if (offset > 0) {
            query.setFirstResult(offset);
        }

        if (limit > 0) {
            query.setMaxResults(limit);
        }

        return query.getResultList();
    }

    public List<ENTITY> loadEntities(String sql, String[] paramNames, Object[] paramValues, int offset, int limit) {
        javax.persistence.TypedQuery<ENTITY> query = entityManager.createQuery(sql, getEntityClass());

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                query.setParameter(paramNames[i], paramValues[i]);
            }
        }

        if (offset > 0) {
            query.setFirstResult(offset);
        }

        if (limit > 0) {
            query.setMaxResults(limit);
        }

        return query.getResultList();
    }
}
