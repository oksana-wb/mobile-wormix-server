package com.pragmatix.app.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pragmatix.app.settings.BattleAwardSettings;
import com.pragmatix.common.utils.MD5;
import com.pragmatix.testcase.AbstractSpringTest;
import com.pragmatix.utils.zip.Zip;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 01.12.2016 12:29
 */
public class ServerConfigTest extends AbstractSpringTest {

    @Resource
    BattleAwardSettings battleAwardSettings;

    @Test
    public void configToJsonTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> config = new  LinkedHashMap<>();
        config.put("battleAwardSettings", battleAwardSettings.getAwardSettingsMap());
        String json = mapper.writeValueAsString(config);
        byte[] bytes = json.getBytes("UTF-8");
        byte[] compress = Zip.compress(bytes);

        System.out.println("compress: " + bytes.length + " -> " + compress.length + " md5 hash: " + MD5.hashToHex(json));

        System.out.println(new String(Zip.decompress(compress), "UTF-8"));
    }

}
