package com.pragmatix.app.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pragmatix.app.settings.BattleAwardSettings;
import com.pragmatix.common.utils.MD5;
import com.pragmatix.serialization.utils.EncryptionUtils;
import com.pragmatix.utils.zip.Zip;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 01.12.2016 13:54
 */
public class ServerConfigHandler extends AbstractHandler {

    @Resource
    private BattleAwardSettings battleAwardSettings;

    @Autowired(required = false)
    private EncryptionUtils encryptionUtils;

    private String configJson = "";
    private byte[] md5Hash = new byte[0];
    private byte[] configData = new byte[0];
    private byte[] encryptedConfigData = new byte[0];

    @PostConstruct
    public void init() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("battleAwardSettings", battleAwardSettings.getAwardSettingsMap());

        configJson = mapper.writeValueAsString(config);
        md5Hash = MD5.hashToHex(configJson).getBytes("UTF-8");
        byte[] bytes = configJson.getBytes("UTF-8");
        configData = Zip.compress(bytes);
        if(encryptionUtils != null) {
            encryptedConfigData = encryptionUtils.encrypt(configData);
        }
    }

    @Override
    public void handle(String path, Request request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException, ServletException {
        try {
            if(path.equals("/json")) {
                byte[] data = configJson.getBytes("UTF-8");
                servletResponse.setContentType("application/json");
                servletResponse.setContentLength(data.length);
                servletResponse.getOutputStream().write(data);
            } else if(path.equals("/hash")) {
                servletResponse.getOutputStream().write(md5Hash);
            } else if(path.equals("/encrypted") && encryptedConfigData.length > 0) {
                servletResponse.setContentType("application/octet-stream");
                servletResponse.setContentLength(encryptedConfigData.length);
                servletResponse.getOutputStream().write(encryptedConfigData);
            } else if(path.equals("/data")) {
                servletResponse.setContentType("application/octet-stream");
                servletResponse.setContentLength(configData.length);
                servletResponse.getOutputStream().write(configData);
            }
        } finally {
            request.setHandled(true);
        }
    }

}
