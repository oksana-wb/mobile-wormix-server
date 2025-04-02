package com.pragmatix.app.services.social.vkontakte;

import com.pragmatix.app.dao.PaymentStatisticDao;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.social.service.VkontakteService;
import com.pragmatix.gameapp.social.service.vkontakte.UserSubscription;
import com.pragmatix.gameapp.social.service.vkontakte.UserSubscriptions;
import com.pragmatix.gameapp.social.service.vkontakte.VkPaymentRecord;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.10.12 18:23
 */
public class WormixVkPaymentProcessorTest extends AbstractSpringTest {

    @Resource
    private WormixVkPaymentProcessor paymentProcessor;

    @Resource
    private VkontakteService vkontakteService;

    @Resource
    private PaymentStatisticDao paymentStatisticDao;

    @Before
    public void setUp() throws Exception {
        paymentProcessor.setPhotoUrlTemplate("test%s");

        HashMap<VkPaymentRecord.Lang, Map<String, String>> titleByLangAndItem = new HashMap<VkPaymentRecord.Lang, Map<String, String>>();
        HashMap<String, String> value = new HashMap<String, String>();
        value.put("1", "3 рубина");
        value.put("11", "300 фузов");
        titleByLangAndItem.put(VkPaymentRecord.Lang.ru_RU, value);
//        paymentProcessor.setTitleByLangAndItem(titleByLangAndItem);

        HashMap<String, List<Integer>> priceByItem = new HashMap<String, List<Integer>>();
        priceByItem.put("1", Arrays.asList(0, 3, 1));
        priceByItem.put("11", Arrays.asList(1, 300, 1));
        paymentProcessor.setPriceByItem(priceByItem);
    }

    @Test
    public void subscriptionsTest() throws Exception {
        int userId = (int) testerProfileId;
        UserSubscriptions userSubscriptions = vkontakteService.getUserSubscriptions(userId);
        println(userSubscriptions);

//        for(UserSubscription item : userSubscriptions.items) {
//            println("cancelSubscription [" + item.id + "] => " + vkontakteService.cancelSubscription(userId, item.id, false));
//        }

    }

    @Test
    public void testHandleGetItem() throws Exception {

        VkPaymentRecord paymentRecord = new VkPaymentRecord();
        paymentRecord.setLang(VkPaymentRecord.Lang.ru_RU.name());
        paymentRecord.setItem("1");
        Map<String, Object> result = paymentProcessor.handleGetItem(paymentRecord);

        assertEquals("3 рубина", result.get("title"));
        assertEquals("1", result.get("price"));

        System.out.println(result);
    }

    @Test
    public void testHandleOrderStatusChange() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        int realMoney = profile.getRealMoney();

        int orderId = new Random().nextInt();
        VkPaymentRecord paymentRecord = new VkPaymentRecord();
        paymentRecord.setLang(VkPaymentRecord.Lang.ru_RU.name());
        paymentRecord.setItem("1");
        paymentRecord.setStatus(VkPaymentRecord.Status.chargeable.name());
        paymentRecord.setItemPrice(1);
        paymentRecord.setReceiverId((int) testerProfileId);
        paymentRecord.setOrderId(orderId);
        paymentRecord.setDate(new Date());

        Map<String, Object> result = paymentProcessor.handleOrderStatusChange(paymentRecord);

        System.out.println(result);

        assertEquals("" + orderId, result.get("order_id"));
        int app_order_id = (Integer) result.get("app_order_id");
        assertTrue(app_order_id > 0);
        assertEquals(realMoney + 3, profile.getRealMoney());

        System.out.println(paymentStatisticDao.selectByTransactionId("" + orderId));

        result = paymentProcessor.handleOrderStatusChange(paymentRecord);

        System.out.println(result);

        assertEquals("" + orderId, result.get("order_id"));
        assertEquals((Integer) app_order_id, (Integer) result.get("app_order_id"));
        assertEquals(realMoney + 3, profile.getRealMoney());

        System.out.println(paymentStatisticDao.selectByTransactionId("" + orderId));
    }

}
