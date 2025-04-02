package com.pragmatix.steam.dao;

import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Vladimir
 * Date: 15.12.11 13:37
 */

public abstract class JpaDAO<ID, PRINCIPAL extends DomainObject<ID>, ENTITY extends Entity<ID>> extends JpaDAOBase<ID, ENTITY> {

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public PRINCIPAL findById(ID id) {
        ENTITY entity = findEntityById(id);

        return entity != null ? createPrincipal(entity) : null;
    }

    public Map<ID, PRINCIPAL> loadByIds(ID[] ids) {
        Map<ID, PRINCIPAL> res = new HashMap<>();
        for (Map.Entry<ID, ENTITY> entry : loadEntitiesByIds(ids).entrySet()) {
            res.put(entry.getKey(), createPrincipal(entry.getValue()));
        }
        return res;
    }

    public List<PRINCIPAL> load(String sql, String[] paramNames, Object[] paramValues, int offset, int limit) {
        return toPrincipalList(loadEntities(sql, paramNames, paramValues, offset, limit));
    }

    protected PRINCIPAL findNQ(String queryName, String[] paramNames, Object[] paramValues) {
        List<PRINCIPAL> principals = loadNQ(queryName, paramNames, paramValues, 0, 1);

        return principals.size() > 0 ? principals.get(0) : null;
    }

    protected List<PRINCIPAL> loadNQ(String queryName, String[] paramNames, Object[] paramValues, int offset, int limit) {
        return toPrincipalList(loadEntitiesNQ(queryName, paramNames, paramValues, offset, limit));
    }

    protected List<PRINCIPAL> toPrincipalList(List<ENTITY> entities) {
        List<PRINCIPAL> res = new ArrayList<>();

        for (ENTITY entity : entities) {
            res.add(createPrincipal(entity));
        }

        return res;
    }

    public PRINCIPAL insert(PRINCIPAL principal) {
        ENTITY entity = insertEntity(createEntity(principal));

        principal.setId(entity.getId());

        return principal;
    }

    public PRINCIPAL update(PRINCIPAL principal) {
        ENTITY entity = createEntity(principal);
        updateEntity(entity);
        return principal;
    }

    protected abstract PRINCIPAL createPrincipal(ENTITY entity);

    protected abstract ENTITY createEntity(PRINCIPAL principal);
}
