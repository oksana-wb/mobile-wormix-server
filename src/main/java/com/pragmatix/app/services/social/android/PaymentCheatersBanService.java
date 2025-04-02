package com.pragmatix.app.services.social.android;

import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.PageInfo;
import com.google.api.services.androidpublisher.model.TokenPagination;
import com.google.api.services.androidpublisher.model.VoidedPurchase;
import com.google.api.services.androidpublisher.model.VoidedPurchasesListResponse;
import com.pragmatix.app.services.BanService;
import com.pragmatix.server.Server;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.03.2016 8:34
 */
public class PaymentCheatersBanService {

    private final Logger log = Server.sysLog;

    @Value("${PaymentCheatersBanService.enabled:true}")
    boolean enabled = true;

    int checkForDays = 60;

    @Resource
    JdbcTemplate jdbcTemplate;

    @Resource
    BanService banService;

    @Resource
    GooglePurchaseService googlePurchaseService;

    public void runDailyTask() {
        if(enabled) {
            try {
                requestVoidedPurchasesAndBan();
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }
    }

    public void requestVoidedPurchasesAndBan() throws GeneralSecurityException, IOException {
        log.info("request voided purchases ...");
        List<VoidedPurchase> voidedPurchasesList = new ArrayList<>();

        // https://developers.google.com/android-publisher/voided-purchases
        VoidedPurchasesListResponse response = googlePurchaseService.getAndroidPublisher().purchases().voidedpurchases().list(googlePurchaseService.packageName).execute();

        voidedPurchasesList.addAll(response.getVoidedPurchases());

        PageInfo pageInfo = response.getPageInfo();
        TokenPagination tokenPagination = response.getTokenPagination();
        while(pageInfo != null && tokenPagination != null) {
            log.info("pageInfo={}, tokenPagination={}", pageInfo, tokenPagination);

            String nextPageToken = tokenPagination.getNextPageToken();
            AndroidPublisher publisher = googlePurchaseService.getAndroidPublisher(request -> request.put("token", nextPageToken));
            response = publisher.purchases().voidedpurchases().list(googlePurchaseService.packageName).execute();

            voidedPurchasesList.addAll(response.getVoidedPurchases());

            pageInfo = response.getPageInfo();
            tokenPagination = response.getTokenPagination();
        }
        log.info("voided purchases ({})", voidedPurchasesList.size());

        Map<String, Tuple2<Date, Date>> voidedTransactions = voidedPurchasesList.stream().collect(Collectors.toMap(
                VoidedPurchase::getPurchaseToken,
                p -> Tuple.of(new Date(p.getPurchaseTimeMillis()), new Date(p.getVoidedTimeMillis()))
        ));

        String transactionsAsParam = voidedTransactions.keySet().stream().map(t -> '\'' + t + '\'').collect(Collectors.joining(","));
        String sqlQuery = "SELECT profile_id, item, transaction_id FROM wormswar.payment_statistic WHERE completed AND payment_status = 0 AND date > now() - INTERVAL '" + checkForDays + " DAYS' " +
                " and transaction_id in (" + transactionsAsParam + ")";
        List<Payment> payments = jdbcTemplate.query(sqlQuery, (rs, rowNum) -> {
            String transaction_id = rs.getString("transaction_id");
            return new Payment(rs.getLong("profile_id"), rs.getString("item"), transaction_id, voidedTransactions.get(transaction_id)._2);
        });
        log.info("void {} app payments ...", payments.size());

        JobResult result = job(payments);

        for(Long cheater : result.cheaters) {
            banService.banForever(cheater);
        }
        log.info("banned {} profiles", result.cheaters.size());
        log.info("done.");
    }

    JobResult job(List<Payment> payments) throws GeneralSecurityException, IOException {
        JobResult result = new JobResult();
        for(Payment payment : payments) {
            try {
                jdbcTemplate.update("UPDATE wormswar.payment_statistic SET completed = FALSE, update_date = ? WHERE transaction_id = ?", payment.voidedTime, payment.transactionId);
                result.addCheater(payment.profileId);
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }
        return result;
    }

    static class Payment {
        final long profileId;
        final String item;
        final String transactionId;
        final Date voidedTime;

        public Payment(long profileId, String item, String transactionId, Date voidedTime) {
            this.profileId = profileId;
            this.item = item;
            this.transactionId = transactionId;
            this.voidedTime = voidedTime;
        }
    }

    static class JobResult {
        Set<Long> cheaters = new HashSet<>();
        int canceled;

        JobResult addCheater(long profileId) {
            cheaters.add(profileId);
            canceled++;
            return this;
        }
    }
}
