package com.pragmatix.app.services;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.dao.ReferralLinkDao;
import com.pragmatix.app.domain.ReferralLinkEntity;
import com.pragmatix.app.messages.structures.LoginAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.GenericAward;
import com.pragmatix.craft.domain.Reagent;
import com.pragmatix.server.Server;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.mina.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 16.04.2014 12:14
 */
@Service
public class ReferralLinkService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private ReferralLinkDao referralLinkDao;

    @Resource
    private DaoService daoService;

    @Resource
    private ProfileBonusService profileBonusService;

    @Value("${debug.awardLoginBonus.referralLink:false}")
    private boolean debugAwardLoginBonusReferralLink = false;

    private ConcurrentMap<String, Set<Integer>> referralLinkVisiters = new ConcurrentHashMap<>();

    private ConcurrentMap<String, ReferralLinkEntity> referralLinks = new ConcurrentHashMap<>();

    private boolean initialized = false;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public void init() {
        Server.sysLog.info("ReferralLinkService init ...");
        List<ReferralLinkEntity> allList = referralLinkDao.getAllList();
        for(ReferralLinkEntity referralLink : allList) {
            if(isNotExpiredAndExeeded(referralLink)) {

                Server.sysLog.info("'{}' start='{}' finish='{}' select ...", referralLink.getToken(), sdf.format(referralLink.getStart()), sdf.format(referralLink.getFinish()));

                registerReferrerLink(referralLink);

                final Set<Integer> visitors = new ConcurrentHashSet<>();
                jdbcTemplate.query("SELECT profile_id FROM wormswar.referral_link_visit WHERE referral_link_id  = ?", resultSet -> {
                    visitors.add(resultSet.getInt("profile_id"));
                }, referralLink.getId());

                Server.sysLog.info("'{}' start='{}' finish='{}' visitors={}", referralLink.getToken(), sdf.format(referralLink.getStart()), sdf.format(referralLink.getFinish()), visitors.size());
                referralLinkVisiters.put(referralLink.getToken(), visitors);
            }
        }
        Server.sysLog.info("ReferralLinkService init done.");
        initialized = true;
    }

    protected void registerReferrerLink(ReferralLinkEntity referralLink) {
        GenericAward genericAward = new GenericAward();
        genericAward.setMoney(referralLink.getFuzy());
        genericAward.setRealMoney(referralLink.getRuby());
        genericAward.setBattlesCount(referralLink.getBattles());
        genericAward.setReactionRate(referralLink.getReaction());
        genericAward.setExperience(referralLink.getExperience());
        genericAward.setBossWinAwardToken(referralLink.getBossToken());
        genericAward.setWagerWinAwardToken(referralLink.getWagerToken());
        if(referralLink.getReagents().length() > 0) {
            HashMap<Byte, Integer> reagents = new HashMap<>();
            for(String reagent : referralLink.getReagents().split(" ")) {
                Reagent reagentName = Reagent.valueOf(Integer.parseInt(reagent));
                byte reagentIndex = reagentName.getIndex();
                if(reagents.containsKey(reagentIndex)) {
                    reagents.put(reagentIndex, reagents.get(reagentIndex) + 1);
                } else {
                    reagents.put(reagentIndex, 1);
                }
            }
            genericAward.setReagents(reagents);
        }
        profileBonusService.setAwardItems(referralLink.getWeapons(), genericAward.getAwardItems());

        referralLink.setGenericAward(genericAward);

        referralLinks.put(referralLink.getToken(), referralLink);
    }

    public String addNewReferralLink(Date start, Date finish, int limit, int ruby, int fuzy, int battles, int reaction, String reagents, String weapons, int experience, int bossToken, int wagerToken) {
        String referralLinkToken = RandomStringUtils.randomAlphanumeric(8).toLowerCase();
        final ReferralLinkEntity referralLink = new ReferralLinkEntity(referralLinkToken, start, finish, limit, ruby, fuzy, battles, reaction, reagents, weapons, experience, bossToken, wagerToken);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                referralLinkDao.insert(referralLink);
                referralLink.setId(referralLink.getId());
            }
        });

        registerReferrerLink(referralLink);
        referralLinkVisiters.put(referralLinkToken, new ConcurrentHashSet<Integer>());

        return referralLinkToken;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void cronTask() {
        if(initialized) {
            for(ReferralLinkEntity referralLink : referralLinks.values()) {
                if(!isNotExpiredAndExeeded(referralLink)) {
                    referralLinks.remove(referralLink.getToken());
                    referralLinkVisiters.remove(referralLink.getToken());

                    Server.sysLog.info("invalidate ReferralLink '{}' start='{}' finish='{}' visitors={}",
                            referralLink.getToken(), sdf.format(referralLink.getStart()), sdf.format(referralLink.getFinish()), referralLink.getVisitors());
                }
            }
        }
    }

    public void removeReferralLink(String referralLinkToken) {
        for(ReferralLinkEntity referralLink : referralLinks.values()) {
            if(referralLink.getToken().equals(referralLinkToken)) {
                referralLinks.remove(referralLink.getToken());
                referralLinkVisiters.remove(referralLink.getToken());

                Server.sysLog.info("remove ReferralLink '{}' start='{}' finish='{}' visitors={}",
                        referralLink.getToken(), sdf.format(referralLink.getStart()), sdf.format(referralLink.getFinish()), referralLink.getVisitors());

                daoService.doInTransactionWithoutResult(() -> referralLinkDao.deleteById(referralLink.getId()));
                break;
            }
        }
    }

    @Null
    public LoginAwardStructure awardForReferralLink(String referralLinkToken, final UserProfile profile) {
        final AwardTypeEnum awardType = AwardTypeEnum.REFERRAL_LINK;
        LoginAwardStructure result = null;
        try {
            final ReferralLinkEntity referralLink = getValidReferralLink(referralLinkToken);
            if(referralLink != null) {
                Set<Integer> visitors = getVisitorsByToken(referralLinkToken);
                if(visitors.add(profile.getId().intValue())) {
                    referralLink.visitors.incrementAndGet();
                    transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                            referralLinkDao.updateVisitors(referralLink);
                            jdbcTemplate.update("INSERT INTO wormswar.referral_link_visit (referral_link_id, profile_id) VALUES (? , ?)", referralLink.getId(), profile.getId());
                        }
                    });

                    result = new LoginAwardStructure(awardType, profileBonusService.awardProfile(referralLink.getGenericAward(), profile, awardType, referralLinkToken), referralLinkToken);
                } else if(debugAwardLoginBonusReferralLink) {
                    result = new LoginAwardStructure(awardType, profileBonusService.awardProfile(referralLink.getGenericAward(), profile, awardType, referralLinkToken), referralLinkToken);
                }
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return result;
    }

    protected Set<Integer> getVisitorsByToken(String referralLinkToken) {
        Set<Integer> visitors = referralLinkVisiters.get(referralLinkToken);
        if(visitors == null) {
            visitors = new ConcurrentHashSet<>();
            Set<Integer> prevValue = referralLinkVisiters.putIfAbsent(referralLinkToken, visitors);
            if(prevValue != null) {
                visitors = prevValue;
            }
        }
        return visitors;
    }

    @Null
    private ReferralLinkEntity getValidReferralLink(String referralLinkToken) {
        ReferralLinkEntity referralLink = referralLinks.get(referralLinkToken);
        if(referralLink != null && isValid(referralLink)) {
            return referralLink;
        }
        return null;
    }

    private boolean isValid(ReferralLinkEntity referralLink) {
        Date now = new Date();
        return now.after(referralLink.getStart()) && now.before(referralLink.getFinish())
                && ((referralLink.getLimit() == 0 || (referralLink.getLimit() > 0 && referralLink.getVisitors() < referralLink.getLimit())));
    }

    private boolean isNotExpiredAndExeeded(ReferralLinkEntity referralLink) {
        Date now = new Date();
        return now.before(referralLink.getFinish())
                && ((referralLink.getLimit() == 0 || (referralLink.getLimit() > 0 && referralLink.getVisitors() < referralLink.getLimit())));
    }


}
