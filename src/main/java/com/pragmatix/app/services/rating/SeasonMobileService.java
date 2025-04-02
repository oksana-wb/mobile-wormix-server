package com.pragmatix.app.services.rating;

import com.pragmatix.clanserver.services.ClanSeasonService;
import com.pragmatix.server.Server;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 12.01.2017 10:06
 */
public class SeasonMobileService {

    @Resource
    private ClanSeasonService clanSeasonService;

    @Scheduled(cron = "0 1 0 1 * *")
    public void closeSeason() {
        Server.sysLog.info("close clan season ...");
        clanSeasonService.closeCurrentSeason();
    }

}
