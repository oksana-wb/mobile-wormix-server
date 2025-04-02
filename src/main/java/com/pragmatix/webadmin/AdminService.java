package com.pragmatix.webadmin;

import com.pragmatix.admin.services.AuditAdminService;
import com.pragmatix.app.domain.stat.AuditAdminActionEntity;
import com.pragmatix.common.utils.CommonUtils;
import com.pragmatix.common.utils.SHA256;
import com.pragmatix.common.xml.XmlWrapper;
import com.pragmatix.wormix.webadmin.interop.CommonResponse;
import com.pragmatix.wormix.webadmin.interop.InteropSerializer;
import com.pragmatix.wormix.webadmin.interop.ServiceResult;
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.01.13 12:48
 */
@Service
public class AdminService implements ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String ADMIN_SCRIPTS_FOLDER = "groovy/webadmin";

    private ApplicationContext applicationContext;

    @Value("${webadmin.validationUrl}")
    private String validationUrl;

    @Value("${server.id:development}")
    private String serverName;

    @Value("${webadmin.secret}")
    private String secret;

    private RestTemplate restTemplate = new RestTemplate();

    private static final String MASTER_TICKET_ID = "oWKCf2OiDgb5wDcGJGTrxzQxs4WT4VRi";

    @Resource
    private AuditAdminService auditAdminService;

    private final Random rnd = new Random();

    @Value("${webadmin.validateTicket:true}")
    private boolean validateTicket = true;

    @PostConstruct
    public void init() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(300)
                .setSocketTimeout(1000)
                .setCookieSpec(CookiePolicy.IGNORE_COOKIES)
                .build();
        CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));
    }

    public CommonResponse execAdminScript(ExecScriptRequest request) {
        if(log.isDebugEnabled()) {
            log.debug("Message in:" + request);
        }
        CommonResponse response;

        boolean logAction = false;

        try {
            if(validateTicket && !validateTicket(request.ticketId, request.adminUser, serverName, request.scriptQname) && !request.ticketId.equals(MASTER_TICKET_ID)) {
                throw new ExecAdminScriptException(ServiceResult.ERR_TICKET_VALIDATE_FAILURE, "запрос не прошел валидацию");
            }

            if(request.ticketId.equals(MASTER_TICKET_ID)) {
                request.adminUser = "MASTER";
            }

            GroovyShell shell = new GroovyShell();

            Script script = shell.parse(new File(ADMIN_SCRIPTS_FOLDER, "route_rules.groovy"));
            Map<String, List<Object>> routeRules = (Map<String, List<Object>>) script.run();

            List<Object> actionProps = routeRules.get(request.scriptQname);

            if(actionProps == null) {
                throw new ExecAdminScriptException(ServiceResult.ERR_INVALID_ARGUMENT, "действие [" + request.scriptQname + "] не зарегистрировано");
            }

            String scriptName = "" + actionProps.get(0);
            String methodName = "" + actionProps.get(1);
            logAction = (Boolean) actionProps.get(2);

            File scriptFile = new File(ADMIN_SCRIPTS_FOLDER, scriptName + ".groovy");

            ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            PrintWriter console = new PrintWriter(new OutputStreamWriter(bos, "UTF-8"));
            Object invokeMethodResult = null;
            ServiceResult result = ServiceResult.OK;

            script = shell.parse(scriptFile);
            try {
                invokeMethodResult = script.invokeMethod(methodName, new Object[]{applicationContext, request, console});
            } catch (ExecAdminScriptException e) {
                log.error("Message in:" + request);
                log.error(e.toString(), e);
                console.print(e.toString());
                result = e.getResult();
            } catch (Exception e) {
                log.error("Message in:" + request);
                log.error(e.toString(), e);
                console.print(e.toString());
                result = ServiceResult.ERR_RUNTIME;
            }

            console.flush();

            if(invokeMethodResult instanceof CommonResponse) {
                response = (CommonResponse) invokeMethodResult;
            } else {
                response = new CommonResponse(result, new String(bos.toByteArray(), "UTF-8"), "" + invokeMethodResult);
            }

        } catch (ExecAdminScriptException e) {
            log.error("Message in:" + request);
            log.error(e.toString(), e);
            response = new CommonResponse(e.getResult(), e.toString(), "");
        } catch (Exception e) {
            log.error("Message in:" + request);
            log.error(e.toString(), e);
            response = new CommonResponse(ServiceResult.ERR_RUNTIME, e.toString(), "");
        }

        if(log.isDebugEnabled()) {
            log.debug("Message out:" + response);
        }

        // логгируем действие на сервере
        try {
            if(logAction && response.result == ServiceResult.OK) {
                InteropSerializer serializer = new InteropSerializer();
                Object params;
                try {
                    params = serializer.fromString(request.scriptParams, Map.class);
                } catch (Exception e) {
                    params = request.scriptParams;
                }
                AuditAdminActionEntity auditAdminActionEntity = auditAdminService.prepareEntity(-1, "", request.scriptQname, request.adminUser);
                auditAdminActionEntity.setNote(String.valueOf(params));

                auditAdminService.addTask(auditAdminActionEntity);
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        return response;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public boolean validateTicket(String ticketId, String userName, String serverName, String scriptQname) {
        String timestamp = Long.toHexString(System.currentTimeMillis() ^ rnd.nextLong());

        Map<String, String> params = CommonUtils.toMap(
                new String[]{"ticketId", "userName", "serverName", "scriptQname", "timestamp"},
                new String[]{ticketId, userName, serverName, scriptQname, timestamp});

        if(log.isTraceEnabled()) {
            log.trace("validateTicketRequest: " + params);
        }

        String url = validationUrl + "?ticketId={ticketId}&userName={userName}&serverName={serverName}&scriptQname={scriptQname}&timestamp={timestamp}";
        String xml = restTemplate.getForObject(url, String.class, params);

        if(log.isTraceEnabled()) {
            log.trace("validateTicketResponce: " + xml);
        }

        XmlWrapper response = new XmlWrapper(xml);

        int errCode = response.getInt("errCode", 0);

        if(errCode == 0) {
            XmlWrapper ticket = response.getChild("ticket");
            String _ticketId = ticket.getString("id", "");
            String issueDate = ticket.getString("issueDate", "");
            String signature = ticket.getString("signature", "");

            return _ticketId.equals(ticketId) && signature.equals(sign(ticketId, issueDate, timestamp, secret));
        } else {
            return false;
        }

    }

    public String sign(String ticketId, String issueDate, String timestamp, String interopSalt) {
        return SHA256.hash(ticketId + " " + issueDate + " " + timestamp, interopSalt);
    }

    public void setValidationUrl(String validationUrl) {
        this.validationUrl = validationUrl;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

}
