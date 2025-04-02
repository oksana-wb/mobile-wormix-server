package com.pragmatix.arena.mercenaries;

import com.pragmatix.common.utils.VarObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.pragmatix.pvp.model.BattleParticipant.State.draw;

public class MercenariesDao {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    private String table = "wormswar.mercenaries";

    public MercenariesEntity find(int profileId) {
        Map<String, Object> params = new HashMap<>();
        params.put("profile_id", profileId);
        final VarObject<MercenariesEntity> result = new VarObject<>();
        namedParameterJdbcTemplate.query("select * from " + table + " where profile_id = :profile_id", params, new RowCallbackHandler() {
            public void processRow(ResultSet res) throws SQLException {
                Date date = res.getDate("start_series");
                result.value = new MercenariesEntity(
                        res.getInt("profile_id"),
                        res.getBoolean("open"),
                        res.getInt("num"),
                        res.getByte("win"),
                        res.getByte("defeat"),
                        res.getByte("draw"),
                        res.getInt("total_win"),
                        res.getInt("total_defeat"),
                        res.getInt("total_draw"),
                        new byte[]{res.getByte("mercenary_01"), res.getByte("mercenary_02"), res.getByte("mercenary_03")},
                        false,
                        false,
                        date != null ? (int) (date.getTime() / 1000L) : 0
                );
            }
        });
        return result.value;
    }

    public void persist(MercenariesEntity entity) {
        persist(entity, false);
    }

    private void execUpdate(MercenariesEntity entity, final String query, boolean inTransaction) {
        final Map<String, Object> params = new HashMap<>();
        params.put("profile_id", entity.profileId);
        params.put("open", entity.open);
        params.put("num", entity.num);
        params.put("start_series", entity.startSeries > 0 ? new Date(entity.startSeries * 1000L) : null);
        params.put("win", entity.win);
        params.put("defeat", entity.defeat);
        params.put("draw", entity.draw);
        params.put("total_win", entity.total_win);
        params.put("total_defeat", entity.total_defeat);
        params.put("total_draw", entity.total_draw);
        params.put("mercenary_01", entity.team[0]);
        params.put("mercenary_02", entity.team[1]);
        params.put("mercenary_03", entity.team[2]);

        if(inTransaction)
            namedParameterJdbcTemplate.update(query, params);
        else
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    namedParameterJdbcTemplate.update(query, params);
                }
            });
    }

    public void persist(MercenariesEntity entity, boolean inTransaction) {
        if(entity.newly) {
            if(entity.num > 0) {
                String query = "insert into " + table + "  (profile_id, open, num, win, defeat, draw, total_win, total_defeat, total_draw, mercenary_01, mercenary_02, mercenary_03)" +
                        " values (:profile_id, :open, :num, :win, :defeat, :draw, :total_win, :total_defeat, :total_draw, :mercenary_01, :mercenary_02, :mercenary_03)";
                execUpdate(entity, query, inTransaction);
                entity.dirty = false;
                entity.newly = false;
            }
        } else if(entity.dirty) {
            String query = "update " + table + " set open = :open, num = :num, start_series = :start_series" +
                    ", win = :win, defeat = :defeat, draw = :draw, total_win = :total_win, total_defeat = :total_defeat, total_draw = :total_draw" +
                    ", mercenary_01 = :mercenary_01, mercenary_02 = :mercenary_02, mercenary_03 = :mercenary_03 " +
                    "where profile_id = :profile_id";
            execUpdate(entity, query, inTransaction);
            entity.dirty = false;
        }
    }

    public void delete(int profileId, boolean inTransaction) {
        final Map<String, Object> params = new HashMap<>();
        params.put("profile_id", profileId);
        final Runnable update = () -> namedParameterJdbcTemplate.update("delete from " + table + " where profile_id = :profile_id", params);
        if(inTransaction)
            update.run();
        else
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    update.run();
                }
            });
    }

}
