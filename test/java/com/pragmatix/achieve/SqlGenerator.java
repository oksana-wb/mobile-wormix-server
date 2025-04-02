package com.pragmatix.achieve;

import com.pragmatix.achieve.award.AchieveAward;
import com.pragmatix.achieve.award.WormixAchieveAward;
import com.pragmatix.achieve.domain.WormixAchievements;
import com.pragmatix.achieve.services.MaintainedAchievementService;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.08.2014 16:58
 */
public class SqlGenerator extends AbstractSpringTest {

    @Value("#{wormixAchievementService}")
    MaintainedAchievementService service;

    @Test
    public void doIt() {
        StringBuilder sb = new StringBuilder();
        int max = 0;
        for(WormixAchievements.AchievementName name : WormixAchievements.AchievementName.values()) {
            if(name.isStat()) continue;
            List<AchieveAward> achieveAwards = service.getAchieveAwards(name.getIndex());
            if(haveReactionAward(achieveAwards)) {
                sb.append(" case\n");
                WormixAchieveAward achieveAward1 = (WormixAchieveAward) achieveAwards.get(0);
                WormixAchieveAward achieveAward2 = (WormixAchieveAward) achieveAwards.get(1);
                WormixAchieveAward achieveAward3 = (WormixAchieveAward) achieveAwards.get(2);
                if(achieveAward1.getReaction() > 0) {
                    sb.append(String.format("   when %s >=%s and %s < %s then %s\n", name.name(), achieveAward1.getProgress(), name.name(), achieveAward2.getProgress(), achieveAward1.getReaction()));
                    max += achieveAward1.getReaction();
                }
                if(achieveAward2.getReaction() > 0) {
                    sb.append(String.format("   when %s >=%s and %s < %s then %s\n", name.name(), achieveAward2.getProgress(), name.name(), achieveAward3.getProgress(), achieveAward2.getReaction()));
                    max += achieveAward2.getReaction();
                }
                if(achieveAward3.getReaction() > 0) {
                    sb.append(String.format("   when %s >=%s then %s\n", name.name(), achieveAward3.getProgress(), achieveAward3.getReaction()));
                    max += achieveAward3.getReaction();
                }
                sb.append(" else 0 end +\n");
                System.out.println("--max=" + max);
            }
        }
        System.out.println(sb.toString());
        System.out.println("max=" + max);
    }

    private boolean haveReactionAward(List<AchieveAward> achieveAwards) {
        for(AchieveAward achieveAward : achieveAwards) {
            WormixAchieveAward wormixAchieveAward = (WormixAchieveAward) achieveAward;
            if(wormixAchieveAward.getReaction() > 0)
                return true;
        }
        return false;
    }

}
