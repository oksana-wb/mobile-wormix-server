package com.pragmatix.webadmin;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.WeaponService;
import com.pragmatix.craft.model.Recipe;
import com.pragmatix.craft.services.CraftService;
import com.pragmatix.testcase.AbstractSpringTest;
import com.pragmatix.testcase.SocketClientConnection;
import com.pragmatix.wormix.webadmin.interop.CommonResponse;
import com.pragmatix.wormix.webadmin.interop.InteropSerializer;
import com.pragmatix.wormix.webadmin.interop.ServiceResult;
import com.pragmatix.wormix.webadmin.interop.request.ExecScriptRequest;
import com.pragmatix.wormix.webadmin.interop.response.structure.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.01.13 15:20
 */
public class WebadminTest extends AbstractSpringTest {

    @Value("${connection.admin.port}")
    private int adminPort;

    private SocketClientConnection adminConnection;

    private String testerProfileId;
    private static int ticketSeq = 0;

    @Resource
    private AdminService adminService;

    @Resource
    private WeaponService weaponService;

    @Resource
    private CraftService craftService;

    public WebadminTest() {
        startServer = true;
        testerProfileId = "" + super.testerProfileId;
    }

    @Before
    public void setUp() throws Exception {
        adminConnection = new SocketClientConnection(binarySerializer);
        adminConnection.connect(host, adminPort);

        adminService.setValidationUrl("http://127.0.0.1:8080/");
        Server server = new Server(8080);
        server.setHandler(new WebAdminValidationUrlMockHandler());
        server.start();
    }

    @Test
    public void banTest() throws Exception {
        InteropSerializer serializer = new InteropSerializer();
        Map<String, Object> params = new HashMap<>();
        params.put("profileId", "58027749");
        params.put("reason", 45);
        params.put("durationInDays", 10);
        params.put("note", "метерился");
        params.put("attachments", "1234567");
        exec("gamer.ban", serializer.toString(params));
        String responseData = reciveResponseData(String.class, 2000);
    }

    @Test
    public void unbanTest() throws Exception {
        InteropSerializer serializer = new InteropSerializer();
        Map<String, Object> params = new HashMap<>();
        params.put("profileId", "58027749");
        params.put("durationInDays", 0);
        params.put("note", "заплатил");
        exec("gamer.unban", serializer.toString(params));
        String responseData = reciveResponseData(String.class, 2000);
    }

    @Test
    public void batchBanTest() throws Exception {
        InteropSerializer serializer = new InteropSerializer();
        Map<String, Object> params = new HashMap<>();
        params.put("profiles", "58027749 58027748 58027747 58027746 qwe rty uio");
        params.put("reason", 45);
        params.put("durationInDays", 10);
        params.put("note", "помиловали");
        params.put("attachments", "1234567");
        System.out.println(params);
        exec("gamer.batchUnban", serializer.toString(params));
        String responseData = reciveResponseData(String.class, 2000);
        System.out.println(responseData);
    }

    @Test
    public void grantAwardTest() throws Exception {
        InteropSerializer serializer = new InteropSerializer();
        Map<String, Object> params = new HashMap<>();
        params.put("profileId", testerProfileId);
        params.put("money", 100);
        params.put("realmoney", 10);
        params.put("rating", 1000);
        params.put("note", "конкурс");
        System.out.println(params);
//        params.put("itemId", 1000);
        exec("gamer.grantAward", serializer.toString(params));
        String responseMsg = reciveResponseData(String.class, 2000);
        System.out.println(responseMsg);

        Thread.sleep(1000);
    }

    @Test
    public void getServerContextTest() throws Exception {
        exec("server.getContext", "");
        Map<String, Object> resultMap = reciveResponseData(Map.class, 2000);
        System.out.println(resultMap);
    }

    @Test
    public void getUserProfileTest() throws Exception {
        InteropSerializer serializer = new InteropSerializer();
        exec("gamer.view", serializer.toString(testerProfileId));
        UserProfileStructure ups = reciveResponseData(UserProfileStructure.class, 2000);
        assertEquals(testerProfileId, ups.id);
        System.out.println(ups);
    }

    @Test
    public void getPurchasesTest() throws Exception {
        InteropSerializer serializer = new InteropSerializer();
        exec("gamer.view.purchases", serializer.toString(testerProfileId));
        BuyStructure[] purchases = reciveResponseData(BuyStructure[].class, 2000);
        assertTrue(purchases.length > 0);
        System.out.println(Arrays.toString(purchases));
    }

    @Test
    public void getAwardsTest() throws Exception {
        InteropSerializer serializer = new InteropSerializer();
        exec("gamer.view.awards", serializer.toString(testerProfileId));
        AwardStructure[] awards = reciveResponseData(AwardStructure[].class, 2000);
        assertTrue(awards.length > 0);
        System.out.println(Arrays.toString(awards));
    }

    @Test
    public void getPaymentsTest() throws Exception {
        InteropSerializer serializer = new InteropSerializer();
        exec("gamer.view.payments", serializer.toString(testerProfileId));
        PaymentStructure[] payments = reciveResponseData(PaymentStructure[].class, 2000);
        assertTrue(payments.length > 0);
        System.out.println(Arrays.toString(payments));
    }

    @Test
    public void getBanStatistic() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", sdf.parse("2013-01-01 00:00").getTime());
        params.put("toDate", System.currentTimeMillis());
        exec("server.getBanStatistic", new InteropSerializer().toString(params));
        BanItem[] resultData = reciveResponseData(BanItem[].class, 20000);
//        System.out.println(Arrays.toString(resultData));
    }

    @Test
    public void updateProfile() throws Exception {
//        UserProfile userProfile = getProfile(super.testerProfileId);
//        short hatId = (short) 1005;
//        stuffService.selectHat(userProfile, hatId);
//        assertEquals(hatId, userProfile.getHat());

        InteropSerializer serializer = new InteropSerializer();
        Map<String, Object> params = new HashMap<>();
        params.put("id", testerProfileId);
        int newLevel = 5;
        Race newRace = Race.ZOMBIE;
        params.put("level", newLevel);
        params.put("race", newRace.toString());
        params.put("searchKeys", 10);
//        params.put("kitId", 2002);
        exec("gamer.update", serializer.toString(params));
        String resultData = reciveResponseData(String.class, 2000);
        assertEquals("[" + testerProfileId + "] successfully updated", resultData);

        exec("gamer.view", serializer.toString(testerProfileId));
        UserProfileStructure ups = reciveResponseData(UserProfileStructure.class, 2000);
        assertEquals(newLevel, getMasetWorm(ups).level);
        assertEquals(0, getMasetWorm(ups).experience);
        assertEquals(newLevel, getMasetWorm(ups).armor);
        assertEquals(newLevel, getMasetWorm(ups).attack);

        assertEquals(newRace.type, getMasetWorm(ups).raceId);

//        assertEquals(hatId + 1, userProfile.getHat());
        assertEquals(10, ups.searchKeys);
    }

    @Test
    public void updateBackpackProfile() throws Exception {
        UserProfile profile = getProfile(super.testerProfileId);
        profile.setRecipes(new short[0]);
        applyRecipe(profile, 3);
        applyRecipe(profile, 2);
        Recipe recipe = craftService.getAllRecipesMap().get((short) 2);
        rollbackRecipe(profile, recipe.getWeaponId());
    }

    @Test
    public void getBonusDays() throws Exception {
        exec("server.bonus.get", "");
        Map resultData = reciveResponseData(Map.class, 20000);
        assertTrue(resultData.containsKey("startDate"));
        assertTrue(resultData.containsKey("endDate"));
        assertTrue(resultData.containsKey("message"));
        assertTrue(resultData.containsKey("money"));
        assertTrue(resultData.containsKey("realMoney"));
        assertTrue(resultData.containsKey("battlesCount"));
    }

    @Test
    public void setBonusDays() throws Exception {
        String startDate = "2013-03-08 08:00";
        String endDate = "2013-03-10 04:00";
        String message = "с 8 марта";
        int money = 340;
        int realMoney = 34;
        int battlesCount = 4;

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        params.put("message", message);
        params.put("money", money);
        params.put("realMoney", realMoney);
        params.put("battlesCount", battlesCount);

        exec("server.bonus.set", new InteropSerializer().toString(params));
        Map resultData = reciveResponseData(Map.class, 20000);
        assertEquals(startDate, resultData.get("startDate"));
        assertEquals(endDate, resultData.get("endDate"));
        assertEquals(message, resultData.get("message"));
        assertEquals(money, resultData.get("money"));
        assertEquals(realMoney, resultData.get("realMoney"));
        assertEquals(battlesCount, resultData.get("battlesCount"));
    }

    private void applyRecipe(UserProfile profile, int recipeId) {
        Recipe recipe = craftService.getAllRecipesMap().get((short) recipeId);
        weaponService.addOrUpdateWeapon(profile, recipe.getWeaponId(), -1);

        InteropSerializer serializer = new InteropSerializer();
        Map<String, Object> params = new HashMap<>();
        params.put("profileId", testerProfileId);
        params.put("actionId", 2);
        params.put("recipeId", recipeId);

        exec("gamer.update.weapon", serializer.toString(params));
        String resultData = reciveResponseData(String.class, 2000);
        assertEquals("[" + testerProfileId + "] backpack successfully updated", resultData);

        exec("gamer.view", serializer.toString(testerProfileId));
        UserProfileStructure ups = reciveResponseData(UserProfileStructure.class, 2000);
        boolean present = recipeIsPresent(recipeId, ups.recipeIds);
        assertTrue(present);
    }

    private boolean recipeIsPresent(int recipeId, short[] recipeIds) {
        for(short presentRecipeId : recipeIds) {
            if(presentRecipeId == recipeId) {
                return true;
            }
        }
        return false;
    }

    private void rollbackRecipe(UserProfile profile, int weaponId) {
        InteropSerializer serializer = new InteropSerializer();
        Map<String, Object> params = new HashMap<>();
        params.put("profileId", testerProfileId);
        params.put("actionId", 2);
        params.put("recipeId", 0);
        params.put("weaponId", weaponId);

        exec("gamer.update.weapon", serializer.toString(params));
        String resultData = reciveResponseData(String.class, 2000);
        assertEquals("[" + testerProfileId + "] backpack successfully updated", resultData);

        exec("gamer.view", serializer.toString(testerProfileId));
        UserProfileStructure ups = reciveResponseData(UserProfileStructure.class, 2000);
        boolean present = false;
        for(Recipe recipe : craftService.getAllRecipesMap().values()) {
            if(recipe.getWeaponId() == weaponId && recipeIsPresent(recipe.getId(), ups.recipeIds)) {
                present = true;
                break;
            }
        }
        assertFalse(present);
    }

    private WormStructure getMasetWorm(UserProfileStructure ups) {
        for(WormStructure wormStructure : ups.wormsGroup) {
            if(wormStructure.ownerId.equals(testerProfileId)) {
                return wormStructure;
            }
        }
        return null;
    }

    private void exec(String scriptQname, String params) {
        ExecScriptRequest message = new ExecScriptRequest();
        message.adminUser = "admin";
        message.scriptQname = scriptQname;
        message.scriptParams = params;
        ticketSeq++;
//        message.ticketId = "" + ticketSeq;
        message.ticketId = "oWKCf2OiDgb5wDcGJGTrxzQxs4WT4VRi";
        message.signature = "";
        adminConnection.send(message);
    }

    public <T> T reciveResponseData(Class<T> responseDataClass, long delay) {
        CommonResponse cmd = adminConnection.receive(CommonResponse.class, delay);
        assertNotNull(cmd);
        assertEquals(ServiceResult.OK, cmd.result);
        InteropSerializer serializer = new InteropSerializer();
        return serializer.fromString(cmd.data, responseDataClass);
    }

    private static class WebAdminValidationUrlMockHandler extends AbstractHandler {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {

            System.out.println("queryString: " + request.getQueryString());

            response.setContentType("text/xml;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            response.getWriter().println("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<result>\n" +
                    "</result>");
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new WebAdminValidationUrlMockHandler());

        server.start();
        server.join();
    }

}
