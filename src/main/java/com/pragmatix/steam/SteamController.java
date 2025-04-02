package com.pragmatix.steam;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.steam.messages.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 17.07.2017 12:56
 */
@Controller
public class SteamController {

    private final Logger log = SocialService.PAYMENT_LOGGER;

    @Autowired(required = false)
    private SteamPurchaseService steamPurchaseServer;

    @OnMessage
    public ProductsInfoResponse onPurchaseInfoRequest(ProductsInfoRequest request, UserProfile userProfile) {
        return steamPurchaseServer.getProductsInfo(userProfile);
    }

    @OnMessage
    public InitPurchaseTxResponse onInitPurchaseTxRequest(InitPurchaseTxRequest request, UserProfile userProfile) {
        if(log.isDebugEnabled()){
            log.debug("message in << {}", request);
        }
        InitPurchaseTxResponse response = steamPurchaseServer.initPurchaseTx(userProfile, request);

        if(log.isDebugEnabled()){
            log.debug("message out >> {}", response);
        }
        return response;
    }

    @OnMessage
    public CommitPurchaseTxResponse onCommitPurchaseTxRequest(CommitPurchaseTxRequest request, UserProfile userProfile) {
        if(log.isDebugEnabled()){
            log.debug("message in << {}", request);
        }
        CommitPurchaseTxResponse response = steamPurchaseServer.commitPurchaseTx(userProfile, request);

        if(log.isDebugEnabled()){
            log.debug("message out >> {}", response);
        }
        return response;
    }

}
