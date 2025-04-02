package com.pragmatix.craft.dao;

import com.pragmatix.craft.domain.Reagent;
import com.pragmatix.craft.domain.ReagentsEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 07.07.12 14:02
 */
@Service
public class ReagentsMapper {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public final String tableName = "wormswar.reagents";

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    private String updateQuery;

    private String insertQuery;

    @PostConstruct
    public void init() {
        updateQuery = "UPDATE " + tableName + " SET ";
        Reagent[] values = Reagent.values();
        for(int i = 0; i < values.length; i++) {
            Reagent reagent = values[i];
            updateQuery += (i > 0 ? ", " : "") + reagent.name() + "=? ";
        }
        updateQuery += " where profile_id = ?";

        insertQuery = "INSERT INTO " + tableName + " (";
        for(Reagent reagent : Reagent.values()) {
            insertQuery += reagent.name() + ", ";
        }
        insertQuery += " profile_id) values (" + StringUtils.repeat("?, ", Reagent.values().length) + "?)";
    }

    public ReagentsEntity select(long profileId) {
        try {
            return jdbcTemplate.queryForObject("SELECT * FROM " + tableName + " WHERE profile_id = ?", (res, i) -> {
                ReagentsEntity result = new ReagentsEntity(res.getInt("profile_id"));
                for(Reagent reagent : Reagent.values()) {
                    result.setReagentValue(reagent, res.getInt(reagent.name()));
                }
                result.setDirty(false);
                result.setNewly(false);
                return result;
            }, profileId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (DataAccessException e) {
            log.error(e.toString(), e);
            return null;
        }
    }

    public int update(ReagentsEntity entity) {
        return persist(entity, updateQuery);
    }

    public int insert(ReagentsEntity entity) {
        return persist(entity, insertQuery);
    }

    private int persist(ReagentsEntity entity, String updateQuery) {
        try {
            List<Object> params = new ArrayList<>();
            for(Reagent achievementName : Reagent.values()) {
                params.add(entity.getReagentValue(achievementName));
            }
            params.add(entity.getProfileId());

            return transactionTemplate.execute(transactionStatus -> jdbcTemplate.update(updateQuery, params.toArray()));
        } catch (Exception e) {
            log.error(e.toString());
            return 0;
        }
    }


    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }
}
