package com.pragmatix.app.services;

import com.pragmatix.app.services.rating.RatingService;
import com.pragmatix.app.services.social.android.PaymentCheatersBanService;
import com.pragmatix.server.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 03.09.13 10:46
 */
@Service
public class AppDailyScheduleService {

    @Resource
    private UserRegistry userRegistry;

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Resource
    private CheatRegisterService cheatRegisterService;

    @Resource
    private RatingService ratingService;

    @Resource
    private PaymentService paymentService;

    @Autowired(required = false)
    private PaymentCheatersBanService paymentCheatersBanService;

    /**
     * в 3 часа ночи запускаем "тяжелые" задачи не относящиеся к боям
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runDailyTask() {
        long start = System.currentTimeMillis();
        Server.sysLog.info("App daily tasks: run ...");

        runDailySubTask("UserRegistry", () -> userRegistry.incrumentUpdateFromDB());

        runDailySubTask("CheatRegisterService", () -> cheatRegisterService.runDailyTask());

        runDailySubTask("CheatersCheckerService", () -> cheatersCheckerService.runDailyTask());

        runDailySubTask("RatingService", () -> ratingService.longRunDailyTask());

        runDailySubTask("PaymentService", () -> paymentService.runDailyTask());

        if(paymentCheatersBanService != null)
            runDailySubTask("PaymentCheatersBanService", () -> paymentCheatersBanService.runDailyTask());

        Server.sysLog.info("App daily tasks: done in {} sec.", Math.round(System.currentTimeMillis() - start) / (double) 1000L);
    }

    private void runDailySubTask(final String className, Runnable task) {
        long taskStart = System.currentTimeMillis();
        Server.sysLog.info("\t run " + className + "'s daily task ...");
        try {
            task.run();
        } catch (Exception e) {
            Server.sysLog.error(e.toString(), e);
        }
        Server.sysLog.info("\t " + className + "'s daily task done in {} sec.", Math.round((System.currentTimeMillis() - taskStart) / (double) 1000L));
    }

}
