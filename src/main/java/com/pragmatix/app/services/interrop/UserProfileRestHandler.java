package com.pragmatix.app.services.interrop;

import com.google.gson.Gson;
import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BanService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.TrueSkillService;
import com.pragmatix.common.utils.AppUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.08.2015 11:09
 */
public class UserProfileRestHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ProfileService profileService;

    @Resource
    private BanService banService;

    @Resource
    private TrueSkillService trueSkillService;

    public boolean enabled = true;

    @Override
    public void handle(String s, Request request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException {
        try {
            String remoteAddr = request.getHeader("X-Real-IP") != null ? request.getHeader("X-Real-IP") : request.getRemoteAddr();
            log.info("{} GET ?{}", remoteAddr, request.getQueryString());
            if(!enabled){
                servletResponse.sendError(400);
                return;
            }

            String userId = request.getParameter("userId");
            final UserProfile profile = profileService.getUserProfile(userId);
            if(profile == null) {
                log.warn("profile not found by id [{}]", userId);
                servletResponse.sendError(404);
                return;
            }
            UserProfileStructure userProfileStructure = profileService.getUserProfileStructure(profile);
            TrueSkillEntity trueSkillEntity = trueSkillService.getTrueSkillFor(profile);

            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("id", profile.getId());
            resultMap.put("socialId", profile.getProfileStringId());
            resultMap.put("banned", banService.isBanned(profile.getId()));
            resultMap.put("level", profile.getLevel());
            resultMap.put("money", profile.getMoney());
            resultMap.put("realMoney", profile.getRealMoney());
            resultMap.put("rating", profile.getRating());
            resultMap.put("lastLoginTime", AppUtils.formatDate(profile.getLastLoginTime()));
            resultMap.put("reactionRate", profile.getReactionRate());
            resultMap.put("boss", profile.getCurrentMission());
            resultMap.put("superBoss", profile.getCurrentNewMission());
            resultMap.put("skill", Math.round((trueSkillEntity.getMean() - trueSkillEntity.getStandardDeviation() * (double) 3) * (double) 500));
            resultMap.put("team", userProfileStructure.wormsGroup());
            resultMap.put("clanId", profile.getClanId());
            resultMap.put("rank", profile.getRankInClan());

            String resultJson = new Gson().toJson(resultMap);
            servletResponse.setContentType("application/json");
            servletResponse.getOutputStream().write(resultJson.getBytes("UTF-8"));
            log.info("Отправлен ответ:\n{}\n", resultJson);
        } catch (Exception e) {
            log.error(e.toString(), e);
            servletResponse.sendError(500);
        } finally {
            request.setHandled(true);
        }
    }

}
