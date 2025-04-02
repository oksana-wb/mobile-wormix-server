package com.pragmatix.app.services;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.messages.client.ActivatePromoKey;
import com.pragmatix.app.messages.server.ActivatePromoKeyError;
import com.pragmatix.app.messages.server.AwardGranted;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.server.Server;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.08.2016 10:36
 */
public class PromoService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public enum PromoAction {
        RAID
    }

    public final JdbcTemplate promoJdbcTemplate;

    public final TransactionTemplate promoTransactionTemplate;

    @Resource
    private ProfileBonusService profileBonusService;

    @Resource
    private ProfileService profileService;

    private Map<PromoAction, GenericAward> promoAwards;

    // погашенные промо ключи
    private Set<String> activatedKeys;

    private String tableName = "wormix.registered_keys";

    private boolean initialized = false;

    public PromoService(DataSource promoDataSource) {
        this.promoJdbcTemplate = new JdbcTemplate(promoDataSource);
        this.promoTransactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(promoDataSource));
    }

    @PostConstruct
    public void init() {
        activatedKeys = new ConcurrentHashSet<>();
        Server.sysLog.info("Fill activated promo keys ...");
        promoJdbcTemplate.query("SELECT key FROM " + tableName + " WHERE activate_date IS NOT NULL", res -> {
            activatedKeys.add(res.getString("key"));
        });
        initialized = true;
        Server.sysLog.info("Done. Activated keys: {}", activatedKeys.size());
    }

    public Optional<Tuple2<PromoAction, List<GenericAwardStructure>>> activatePromoKey(String promoKey, UserProfile profile) {
        if(!initialized) return Optional.empty();
        try {
            if(activatedKeys.contains(promoKey)) {
                log.warn("Промо код [{}] уже активирован ранее.", promoKey);
                return Optional.empty();
            } else {
                Short action = null;
                try {
                    action = promoJdbcTemplate.queryForObject("SELECT action FROM " + tableName + " WHERE key = ? AND activate_date IS NULL", Short.class, promoKey);
                } catch (EmptyResultDataAccessException e) {
                }
                if(action == null) {
                    log.warn("Промо код [{}] не найден или уже активирован ранее.", promoKey);
                    return Optional.empty();
                }
                // "гасим" промо код
                boolean success = promoTransactionTemplate.execute(transactionStatus -> {
                            int profileId = profile.getId().intValue();
                            short socialNet = profileService.getDefaultSocialId().getShortType();
                            return promoJdbcTemplate.update("UPDATE " + tableName + " SET activate_date = ?, profile_id = ?, social_net = ? " +
                                    "WHERE key = ? AND activate_date IS NULL", new Date(), profileId, socialNet, promoKey);
                        }
                ) == 1;
                if(success) {
                    // выдаем награду
                    PromoAction promoAction = PromoAction.values()[action.intValue()];
                    GenericAward genericAward = promoAwards.get(promoAction);
                    List<GenericAwardStructure> awards = profileBonusService.awardProfile(genericAward, profile, AwardTypeEnum.ACTION,
                            "promoAction", promoAction.name(),
                            "promoKey", promoKey
                    );
                    profileService.updateSync(profile);

                    activatedKeys.add(promoKey);
                    return Optional.of(Tuple.of(promoAction, awards));
                } else {
                    log.warn("Промо код [{}] активирован ранее.", promoKey);
                    return Optional.empty();
                }
            }
        } catch (DataAccessException e) {
            log.error(e.toString(), e);
            return Optional.empty();
        }
    }

    public void deactivatePromoKeys(UserProfile profile) {
        short socialNet = profileService.getDefaultSocialId().getShortType();
        List<String> keys = Collections.emptyList();
        try {
            keys = promoJdbcTemplate.queryForList("SELECT key FROM " + tableName + " WHERE profile_id = ? and social_net = ?", String.class, profile.getId(), socialNet);
        } catch (EmptyResultDataAccessException e) {
        }

        if(!keys.isEmpty()){
            log.info("Деактивируем промо коды для игрока [{}]: {}", profile, keys);
            promoTransactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    promoJdbcTemplate.update("update " + tableName + " set activate_date = null, profile_id = null, social_net = null" +
                            "  where profile_id = ? and social_net = ?", profile.getId(), socialNet);
                }
            });
            for(String key : keys) {
                activatedKeys.remove(key);
            }
        }
    }

    public void setPromoAwards(Map<PromoAction, GenericAward> promoAwards) {
        this.promoAwards = new EnumMap(promoAwards);
    }

    @Controller
    public static class PromoController {

        private final Optional<PromoService> promoService;

        public PromoController(Optional<PromoService> promoService) {
            this.promoService = promoService;
        }

        @OnMessage
        public Object onActivatePromoKey(ActivatePromoKey msg, UserProfile profile) {
            return promoService.flatMap(service -> service.activatePromoKey(msg.key, profile)).map(action_awards -> {
                String attach = action_awards._1.name() + "," + msg.key;
                return (Object) new AwardGranted(AwardTypeEnum.ACTION, action_awards._2, attach, Sessions.getKey(profile));
            }).orElse(new ActivatePromoKeyError(msg.key));
        }
    }

}
