package com.pragmatix.app.controllers;

import com.pragmatix.app.messages.client.facebook.FbRegisterPayment;
import com.pragmatix.app.messages.server.FbRegisterPaymentResult;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.social.service.VkontakteService;
import com.pragmatix.gameapp.social.service.facebook.FacebookService;
import com.pragmatix.testcase.AbstractSpringTest;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Test;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.01.12 12:29
 */
public class PaymentControllerTest extends AbstractSpringTest {

    protected Server server;

    @Resource
    private VkontakteService vkontakteService;

    //    @Before
    public void prepare() throws Exception {
        server = new Server(8080);
        server.setHandler(new VkontakteApiMockHandler());

        server.start();
//        server.join();

        vkontakteService.setTokenUrl("http://127.0.0.1:8080/");
        vkontakteService.setApiUrl("http://127.0.0.1:8080/");
    }

    //    @After
    public void finish() throws Exception {
        server.stop();
    }

    @Test
    public void registerPaymentTest() throws Exception {
        loginMain();

        FbRegisterPayment message = new FbRegisterPayment();
        message.itemId = "1";
        sendMain(message);

        FbRegisterPaymentResult fbRegisterPaymentResult = receiveMain(FbRegisterPaymentResult.class, 1000);

        assertEquals(SimpleResultEnum.SUCCESS, fbRegisterPaymentResult.result);
        assertEquals(message.itemId, fbRegisterPaymentResult.itemId);
        assertTrue(fbRegisterPaymentResult.paymentId > 0);
    }

    @Test
    public void registerInvalidPaymentTest() throws Exception {
        loginMain();

        FbRegisterPayment message = new FbRegisterPayment();
        message.itemId = "0";
        sendMain(message);

        FbRegisterPaymentResult fbRegisterPaymentResult = receiveMain(FbRegisterPaymentResult.class, 1000);

        assertEquals(SimpleResultEnum.ERROR, fbRegisterPaymentResult.result);
        assertEquals(message.itemId, fbRegisterPaymentResult.itemId);
        assertTrue(fbRegisterPaymentResult.paymentId <= 0);
    }

    private static int delay = 6500;

    private static class VkontakteApiMockHandler extends AbstractHandler {
        public void handle(String target,
                           Request baseRequest,
                           HttpServletRequest request,
                           HttpServletResponse response)
                throws IOException, ServletException {

//            try {
//                Thread.sleep(delay);
//                delay -= 1000;
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

            String votes = request.getParameter("votes");

            response.setContentType("text/xml;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
//            response.getWriter().println("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
//                    "<response>\n" +
//                    "<transferred>" + votes + "</transferred>\n" +
//                    "</response>");
            response.getWriter().println("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<error>\n" +
                    "<error_code>502</error_code>\n" +
                    "<error_msg>a vot i nety</error_msg>\n" +
                    "</error>");
        }
    }

    public static void main(String[] args) throws Exception {
//        Server server = new Server(8080);
//        server.setHandler(new VkontakteApiMockHandler());
//
//        server.start();
//        server.join();
//        String authSecret = "a4e84fa7040c7afc16673a6876315900";
        String authSecret = "ef4feb6074b1b4c94f711b5054c7f933";
        String encodedRequest = "PpEFJ1XI_KA2fj2ZwYQKPKTZgdWUG00-tfUsw7_XElI.eyJhbGdvcml0aG0iOiJITUFDLVNIQTI1NiIsImFtb3VudCI6IjAuOTkiLCJjdXJyZW5jeSI6IlVTRCIsImlzc3VlZF9hdCI6MTQzODg1NzkxNSwicGF5bWVudF9pZCI6NjgzMTU3NzY4NDgwOTkyLCJxdWFudGl0eSI6IjEiLCJyZXF1ZXN0X2lkIjoiODM5Iiwic3RhdHVzIjoiY29tcGxldGVkIn0";
        Base64 base64 = new Base64(true);
        int i = encodedRequest.indexOf('.');
        String signature = new String(base64.decode(encodedRequest.substring(0, i).getBytes("UTF-8")));
        String content = encodedRequest.substring(i + 1);

        System.out.println(signature.equals(FacebookService.hmacSHA256(content, authSecret)));
    }

}
