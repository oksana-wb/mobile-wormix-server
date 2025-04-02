package com.pragmatix.steam;

import com.pragmatix.app.common.Locale;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.social.SocialUserIdMapService;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.steam.messages.InitPurchaseTxRequest;
import com.pragmatix.steam.messages.InitPurchaseTxResponse;
import com.pragmatix.steam.messages.ProductsInfoResponse;
import com.pragmatix.testcase.AbstractSpringTest;
import org.junit.Test;

import javax.annotation.Resource;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.07.2017 13:13
 */
public class SteamPurchaseServerTest extends AbstractSpringTest {

    @Resource
    SteamPurchaseService steamPurchaseServer;

    @Resource
    SocialUserIdMapService socialUserIdMap;

    @Test
    public void getPurchaseInfoTest() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setProfileStringId("76561198360214312");
        ProductsInfoResponse purchaseInfo = steamPurchaseServer.getProductsInfo(profile);

        println(purchaseInfo);
    }

    @Test
    public void initPurchaseTxTest() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setLocale(Locale.RU);
        profile.setProfileStringId("76561198360214312");

        InitPurchaseTxRequest request = new InitPurchaseTxRequest();
        request.productCode="ruby5";

        InitPurchaseTxResponse initPurchaseTxResponse = steamPurchaseServer.initPurchaseTx(profile, request);

        println(initPurchaseTxResponse);
    }

    @Test
    public void getProductsInfoTest() throws Exception {
        UserProfile profile = getProfile(testerProfileId);
        profile.setLocale(Locale.RU);
        profile.setSocialId((byte)SocialServiceEnum.steam.getType());
        socialUserIdMap.map("76561198360214312", SocialServiceEnum.steam.getShortType(), testerProfileId);
        steamPurchaseServer.onLogin(profile);
        profileService.updateSync(profile);

        ProductsInfoResponse productsInfo = steamPurchaseServer.getProductsInfo(profile);

        println(productsInfo);
    }

}