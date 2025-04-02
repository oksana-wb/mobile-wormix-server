package com.pragmatix.notify.sender;

import ch.qos.logback.classic.PatternLayout;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.exceptions.NetworkIOException;
import com.pragmatix.gameapp.services.DailyTaskAvailable;
import com.pragmatix.gameapp.services.IServiceTask;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.notify.*;
import com.pragmatix.server.Server;
import com.pragmatix.utils.logging.AccountConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 21.08.13 12:03
 */
public class ApplePushNotificationSender implements NotifySenderI, DailyTaskAvailable {

    private final Logger log = LoggerFactory.getLogger(ApplePushNotificationSender.class);

    private ApnsService service;

    private String p12SertFileName;

    private String p12SertPassword;

    private boolean prodaction;

    @Resource
    private Registrator registrator;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        service = APNS.newService()
                .withCert(p12SertFileName, p12SertPassword)
                .withAppleDestination(prodaction)
                .build();
        service.start();
        initialized = true;
    }

    @PreDestroy
    public void destroy() {
        service.stop();
    }

    @Override
    public boolean send(String registrationId, int timeToLive, String localizedMessage) {
        try {
            String payload = APNS.newPayload()
                    .alertBody(localizedMessage)
                    .build();
            service.push(registrationId, payload);

            return true;
        } catch (NetworkIOException e) {
            log.error(e.toString(), e);
            return false;
        }
    }

    @Override
    public SocialServiceEnum getSocialNetId() {
        return SocialServiceEnum.mobile;
    }


    public static void main(String[] args) {
        PatternLayout.defaultConverterMap.put( "account", AccountConverter.class.getName() );

        String fileName = "ssl/Wormix_PUSH_IOS_production.p12";
        String password = "littleBoxerJumpedHigh";
//        String fileName = "ssl/WormixAdHoc-notifications.p12";
//        String password = "zxcvm,./";
        ApnsService service = APNS.newService()
                .withCert(fileName, password)
                .withAppleDestination(true)
                .build();
        service.start();

        String payload = APNS.newPayload()
//                .localizedKey("COME_BACK")
//                .localizedArguments("")
                .alertBody("Горячий привет из солнечного Краснодара (на боевой)!")
                .build();

//        String registrationId = "FA630E9BA5ABAD8BAEF7ADD9CF8020BDA87308BC8487401EC70AF9C4EB449293";//Илья
        String registrationId = "4734B13E92AEA1D7243D4473DE1F528E379ACC12F06785EE8D7072786193D733";//Илья (боевой)
//        String registrationId = "4BF0FEA4D22964F39CED30536BD46780E31089F3817388DD636DCC801F149CD8";//denis
//        String registrationId = "E9EC269B57C67723A8F5217BC2B3643F530BAF66C4B6E59062303A4E5666D8BF";// my

        service.push(registrationId, payload);
    }


    @Override
    public IServiceTask getDailyTask() {
        return new IServiceTask() {
            @Override
            public void runServiceTask() {
                Map<String, Date> inactiveDevices = service.getInactiveDevices();
                Server.sysLog.info("ApplePushNotificationSender: InactiveDevices:\n" + inactiveDevices);
                for(final String inactiveRegistrationId : inactiveDevices.keySet()) {
                   registrator.unregistrate(inactiveRegistrationId);
                }
            }

            @Override
            public boolean isInitialized() {
                return initialized;
            }
        };
    }

    public String getP12SertFileName() {
        return p12SertFileName;
    }

    public void setP12SertFileName(String p12SertFileName) {
        this.p12SertFileName = p12SertFileName;
    }

    public String getP12SertPassword() {
        return p12SertPassword;
    }

    public void setP12SertPassword(String p12SertPassword) {
        this.p12SertPassword = p12SertPassword;
    }

    public boolean isProdaction() {
        return prodaction;
    }

    public void setProdaction(boolean prodaction) {
        this.prodaction = prodaction;
    }

}
