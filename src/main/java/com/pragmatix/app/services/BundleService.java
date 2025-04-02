package com.pragmatix.app.services;

import com.pragmatix.app.common.AwardKindEnum;
import com.pragmatix.app.dao.BundleDao;
import com.pragmatix.app.domain.BundleEntity;
import com.pragmatix.app.messages.structures.BundleStructure;
import com.pragmatix.app.messages.structures.GenericAwardStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.server.Server;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class BundleService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private BundleDao bundleDao;

    @Resource
    private ProfileBonusService profileBonusService;

    public ConcurrentMap<String, BundleStructure> bundles = new ConcurrentHashMap<>();

    private Map<String, BundleStructure> preConfiguredBundles = Collections.emptyMap();

    private boolean initialized = false;

    public int PurchaseTimeoutInMinutes = 10;

    public void init() {
        bundles = selectValidBundles();
        initialized = true;
    }

    @Autowired(required = false)
    public void setPreConfiguredBundles(Collection<BundleStructure> preConfiguredBundles) {
        log.debug("BundleService.setPreConfiguredBundles: {}", preConfiguredBundles);
        this.preConfiguredBundles = preConfiguredBundles.stream().collect(Collectors.toMap(
                i -> i.code,
                i -> i
        ));
        log.debug("BundleService.setPreConfiguredBundles: {}", this.preConfiguredBundles);
    }

    protected ConcurrentMap<String, BundleStructure> selectValidBundles() {
        ConcurrentMap<String, BundleStructure> bundles = new ConcurrentHashMap<>();
        for(BundleEntity bundle : bundleDao.getAllList()) {
            if(log.isDebugEnabled())
                log.debug("select {}", bundle);
            if(isValid(bundle))
                bundles.put(bundle.getCode(), entityToStructure(bundle));
        }
        bundles.forEach((code, bundleStructure) -> Server.sysLog.info("registered bundle: {}=>{}", code, bundleStructure));
        preConfiguredBundles.forEach((code, bundleStructure) -> Server.sysLog.info("pre configured bundle: {}=>{}", code, bundleStructure));

        bundles.putAll(preConfiguredBundles);
        return bundles;
    }

    protected BundleStructure entityToStructure(BundleEntity bundle) {
        BundleStructure bundleStructure = new BundleStructure();
        bundleStructure.code = bundle.getCode();
        bundleStructure.order = bundle.getSortOrder();
        bundleStructure.votes = bundle.getVotes();
        bundleStructure.discount = bundle.getDiscount();
        bundleStructure.finish = bundle.getFinish();
        ArrayList<GenericAwardStructure> awardItems = new ArrayList<>();
        profileBonusService.setGenericAwardItems(bundle.getItems(), awardItems);
        Arrays.stream(bundle.getRaces().split(" "))
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .forEach(raceId -> awardItems.add(new GenericAwardStructure(AwardKindEnum.RACE, 1, raceId)));
        Arrays.stream(bundle.getSkins().split(" "))
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .forEach(skinId -> awardItems.add(new GenericAwardStructure(AwardKindEnum.SKIN, 1, skinId)));
        bundleStructure.items = awardItems.toArray(new GenericAwardStructure[0]);
        return bundleStructure;
    }

    public void persistBundle(final Long id, final String code, final int order, final Date start, final Date finish, final int discount, final float votes, final String races, final String skins, final String items, final boolean disabled) {
        if(id == null || id == 0)
            addBundle(code, order, start, finish, discount, votes, races, skins, items, disabled);
        else
            updateBundle(id, code, order, start, finish, discount, votes, races, skins, items, disabled);

        bundles = selectValidBundles();
    }

    private void addBundle(String code, int order, Date start, Date finish, int discount, float votes, String races, String skins, String items, boolean disabled) {
        final BundleEntity bundle = new BundleEntity(code, order, start, finish, discount, votes, races, skins, items, disabled);
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                bundleDao.insert(bundle);
                bundle.setId(bundle.getId());
            }
        });
    }

    private void updateBundle(final Long id, final String code, final int order, final Date start, final Date finish, final int discount, final float votes, final String races, final String skins, final String items, final boolean disabled) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                BundleEntity bundle = bundleDao.get(id);
                bundle.setCode(code);
                bundle.setSortOrder(order);
                bundle.setStart(start);
                bundle.setFinish(finish);
                bundle.setDiscount(discount);
                bundle.setVotes(votes);
                bundle.setRaces(races);
                bundle.setSkins(skins);
                bundle.setItems(items);
                bundle.setDisabled(disabled);
                bundle.setUpdateDate(new Date());
                bundleDao.update(bundle);
            }
        });
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void cronTask() {
        if(initialized) {
            for(BundleStructure bundle : bundles.values()) {
                if(!isLive(bundle)) {
                    bundles.remove(bundle.code);
                    Server.sysLog.info("invalidate bundle: {}", bundle);
                    for(BundleStructure bundleStructure : bundles.values()) {
                        Server.sysLog.info("registered bundle: {}", bundleStructure);
                    }
                }
            }
        }
    }

    private boolean isLive(BundleStructure bundle) {
        return isLive(bundle, new Date());
    }

    private boolean isLive(BundleStructure bundle, Date now) {
        return bundle.finish == null || now.before(bundle.finish);
    }

    @Null
    public BundleStructure getValidBundle(String code) {
        BundleStructure bundle = bundles.get(code);
        if(bundle != null && isLive(bundle)) return bundle;
        else return null;
    }

    public BundleStructure getBundle(String code) {
        return bundles.get(code);
    }

    public List<BundleStructure> getValidBundles() {
        List<BundleStructure> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -PurchaseTimeoutInMinutes);
        for(BundleStructure bundle : bundles.values()) {
            if(!bundle.isServerOnly() && isLive(bundle, cal.getTime()))
                result.add(bundle);
        }
        return result;
    }

    public boolean isValid(BundleEntity bundle) {
        Date now = new Date();
        return !bundle.isDisabled() &&
                (bundle.getStart() == null || now.after(bundle.getStart()))
                && (bundle.getFinish() == null || now.before(bundle.getFinish()));
    }

    public Tuple2<List<GenericAwardStructure>, Integer> issueBundle(UserProfile profile, BundleStructure validBundle) {
        return profileBonusService.issueBundle(profile, validBundle.getItems(profile), validBundle.period);
    }

}
