package com.pragmatix.webadmin;

import com.pragmatix.gameapp.GameApp;
import com.pragmatix.gameapp.threads.Execution;
import com.pragmatix.gameapp.threads.ExecutionContext;
import com.pragmatix.wormix.webadmin.interop.CommonResponse;
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.security.AnyTypePermission;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Null;
import java.io.IOException;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 05.02.2016 14:37
 *         <p>
 *         HTTP коннект для админки
 *         <p>
 *         В частности, в обход ограничений платформы по длительности выпололнения одного запроса
 */
public class AdminHandler extends AbstractHandler {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final ThreadLocal<AdminHandler> ADMIN_HANDLER = new ThreadLocal<>();

    @Resource
    private AdminService adminService;

    @Resource
    private GameApp gameApp;

    private String remoteAddr = "[undefined]";

    public boolean enabled = true;

    public static final XStream xstream;

    static {
        xstream = new XStream(new PureJavaReflectionProvider());
        xstream.addPermission(AnyTypePermission.ANY);

        xstream.registerConverter(new SqlTimestampConverter(), XStream.PRIORITY_VERY_HIGH);
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException {

        ADMIN_HANDLER.set(this);
        try {
            remoteAddr = request.getHeader("X-Real-IP") != null ? request.getHeader("X-Real-IP") : request.getRemoteAddr();
            log.trace("{} POST {}", remoteAddr, request.getUri());
            if(!enabled){
                servletResponse.sendError(400);
                return;
            }

            ExecScriptRequest execScriptRequest = (ExecScriptRequest) xstream.fromXML(request.getReader());

            ExecutionContext context = new ExecutionContext(gameApp);
            Execution.EXECUTION.set(context);

            CommonResponse execScriptResponse = adminService.execAdminScript(execScriptRequest);
            log.trace("Script {} console output:\n{}", execScriptRequest.scriptQname, execScriptResponse.consoleOutput);

            switch (execScriptResponse.result) {
                case ERR_TICKET_VALIDATE_FAILURE:
                case ERR_BAD_SIGNATURE:
                case ERR_ACCESS_DENIED:
                    servletResponse.sendError(403);
                    return;
                case ERR_RUNTIME:
                case ERR_RESPONSE:
                case ERR_SCRIPT_UNTERMINATED:
                    servletResponse.setStatus(500);
                    break;
                case ERR_INVALID_ARGUMENT:
                case ERR_PROFILE_NOT_FOUND:
                case ERR_CLAN_NOT_FOUND:
                    servletResponse.setStatus(404);
                    break;
                // default: Ok, continue
            }

            String resultXML = xstream.toXML(execScriptResponse);
            servletResponse.setContentType("application/xml");
            servletResponse.getOutputStream().write(resultXML.getBytes("UTF-8"));

        } catch (Exception e) {
            log.error(e.toString(), e);
            servletResponse.sendError(500, e.toString());
        } finally {
            request.setHandled(true);
        }
    }

    @Null
    public static AdminHandler get() {
        return ADMIN_HANDLER.get();
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }
}
