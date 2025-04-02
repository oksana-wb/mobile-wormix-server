package com.pragmatix.app.services.interrop;

import com.google.gson.Gson;
import com.pragmatix.app.domain.BanEntity;
import com.pragmatix.app.domain.TrueSkillEntity;
import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.BanService;
import com.pragmatix.app.services.DaoService;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.TrueSkillService;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.wormix.webadmin.interop.response.structure.BanItem;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 13.08.2015 11:09
 */
public class BansRestHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ProfileService profileService;

    @Resource
    private DaoService daoService;

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
            List<BanEntity> banEntities = daoService.getBanDao().selectBanEntities(profile.getId());
            List<Map<String, Object>> result = new ArrayList<>();
            for(BanEntity banEntity : banEntities) {
                Map<String, Object> resultMap = new LinkedHashMap<>();

                resultMap.put("startDate", AppUtils.formatDate(banEntity.getStartDate()));
                resultMap.put("endDate", AppUtils.formatDate(banEntity.getEndDate()));
                resultMap.put("note", banEntity.getNote());
                resultMap.put("admin", banEntity.getAdmin());
                resultMap.put("type", banEntity.getType());

                result.add(resultMap);
            }

            String resultJson = new Gson().toJson(result);
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
