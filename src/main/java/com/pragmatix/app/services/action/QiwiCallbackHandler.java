package com.pragmatix.app.services.action;

import com.pragmatix.app.common.AwardTypeEnum;
import com.pragmatix.app.common.Connection;
import com.pragmatix.app.messages.server.ItemGranted;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.StatisticService;
import com.pragmatix.app.services.WeaponService;
import com.pragmatix.gameapp.GameApp;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.gameapp.sessions.Session;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.gameapp.threads.ExecutionContext;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 12.08.2014 9:50
 */
public class QiwiCallbackHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ProfileService profileService;

    @Resource
    private WeaponService weaponService;

    @Resource
    private StatisticService statisticService;

    @Resource
    private GameApp gameApp;

    private int prizeWeapon;

    private int prizeWeaponCount = -1;

    private String prizeNote = "";

    @Override
    public void handle(String s, Request request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException {
        try {
            String remoteAddr = request.getHeader("X-Real-IP") != null ? request.getHeader("X-Real-IP") : request.getRemoteAddr();
            log.info("{} GET ?{}", remoteAddr, request.getQueryString());

            int profileId = Integer.parseInt(request.getParameter("userId"));
            final UserProfile profile = profileService.getUserProfile(profileId);
            if(profile == null) {
                log.error("profile not found by id [{}]", profileId);
                servletResponse.sendError(404);
                return;
            }
            boolean result = weaponService.addOrUpdateWeapon(profile, prizeWeapon, prizeWeaponCount);
            if(result) {
                statisticService.awardStatistic(profile.getProfileId(), 0, 0, prizeWeapon, AwardTypeEnum.ACTION.getType(), prizeNote);

                if(Execution.EXECUTION.get() == null) {
                    ExecutionContext context = new ExecutionContext(gameApp);
                    Execution.EXECUTION.set(context);
                }
                Session session = Sessions.get(profile);
                if(session != null) {
                    Messages.toUser(new ItemGranted(prizeWeapon, prizeWeaponCount, session.getKey()), profile, Connection.MAIN);
                    sendResponse(servletResponse, "OK online");
                } else {
                    profileService.updateSync(profile);
                    sendResponse(servletResponse, "OK offline");
                }
            }else{
                sendResponse(servletResponse, "FAILURE alredy granted");
            }
        } catch (NumberFormatException e) {
            log.error(e.toString());
            servletResponse.sendError(500);
        } catch (Exception e) {
            log.error(e.toString(), e);
            servletResponse.sendError(500);
        } finally {
            request.setHandled(true);
        }
    }

    private void sendResponse(HttpServletResponse servletResp, String s) throws IOException {
        servletResp.setContentType("text/plain");
        servletResp.getOutputStream().write(s.getBytes("UTF-8"));
        log.info("Отправлен ответ:\n{}\n", s);
    }

    public int getPrizeWeapon() {
        return prizeWeapon;
    }

    public void setPrizeWeapon(int prizeWeapon) {
        this.prizeWeapon = prizeWeapon;
    }

    public int getPrizeWeaponCount() {
        return prizeWeaponCount;
    }

    public void setPrizeWeaponCount(int prizeWeaponCount) {
        this.prizeWeaponCount = prizeWeaponCount;
    }

    public String getPrizeNote() {
        return prizeNote;
    }

    public void setPrizeNote(String prizeNote) {
        this.prizeNote = prizeNote;
    }
}
