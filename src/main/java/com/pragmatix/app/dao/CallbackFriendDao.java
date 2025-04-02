package com.pragmatix.app.dao;

import com.pragmatix.gameapp.services.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.09.12 17:37
 */
@Component
public class CallbackFriendDao {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TaskService taskService;

    // сохранит предложение вернуться в базу
    public boolean  callbackFriend(final long friendId, final Long profileId) {
        return taskService.addTransactionTask(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                try {
                    jdbcTemplate.update("insert into wormswar.callback_friend (friend_id, profile_id) values (?, ?)", friendId, profileId);
                } catch (DuplicateKeyException e) {
                    log.info(e.toString(), e);
                } catch (DataIntegrityViolationException e) {
                    log.info(e.toString(), e);
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            }
        });
    }

    // проверяет, что предложение имело место
    public boolean isCallbackExists(final Long friendId, final long profileId) {
        try {
            return jdbcTemplate.queryForObject("select 1 from wormswar.callback_friend where friend_id = ? and profile_id = ?", Integer.class, friendId, profileId) == 1;
        } catch (EmptyResultDataAccessException e) {
            return false;
        } catch (DataAccessException e) {
            log.error(e.toString(), e);
            return false;
        }
    }

    // вернет список (не более limit) кто звал игрока обратно
    public List<Long> selectCallers(final Long profileId, int limit) {
        try {
            return jdbcTemplate.queryForList("select profile_id from wormswar.callback_friend where friend_id = ? limit ?", Long.class, profileId, limit);
        } catch (DataAccessException e) {
            log.error(e.toString(), e);
            return null;
        }
    }

    // удаляет все приглашения игрока
    public void deleteCallbacksForProfile(final Long profileId) {
        taskService.addTransactionTask(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                try {
                    jdbcTemplate.update("delete from wormswar.callback_friend  where friend_id = ? ", profileId);
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            }
        });
    }
}
