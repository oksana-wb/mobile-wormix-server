package com.pragmatix.gameapp.social.service.facebook;

import com.pragmatix.testcase.AbstractSpringTest;
import net.sf.json.JSONObject;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 27.02.14 15:20
 */
public class FacebookServiceTest extends AbstractSpringTest {

    @Resource
    FacebookService facebookService;

    @Test
    public void testGetPaymentDetails() throws Exception {
        JSONObject paymentDetails = facebookService.getPaymentDetails(1266499270133651L, LoggerFactory.getLogger(this.getClass()));
        System.out.println(paymentDetails);
    }

    @Test
    public void validateRealTimeUpdateTest() throws Exception {
        def body = """{"object":"payments","entry":[{"id":"1376988865750321","time":1498859245,"changed_fields":["actions"]}]}"""
        println(hmacSHA1(body, facebookService.authSecret))
    }

    public static String hmacSHA1(String data, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA1");

            Mac mac = Mac.getInstance("HmacSHA1");

            mac.init(secretKey);

            byte[] digest = mac.doFinal(data.getBytes("UTF-8"));
            return toHexString(digest)
//            BigInteger hash = new BigInteger(1, digest);
//            String hmac = hash.toString(16);
//
//            if(hmac.length() % 2 != 0) {
//                hmac = "0" + hmac;
//            }
//            return hmac;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHexString(byte[] bytes) {
   		Formatter formatter = new Formatter();

   		for (byte b : bytes) {
   			formatter.format("%02x", b);
   		}

   		return formatter.toString();
   	}
}
