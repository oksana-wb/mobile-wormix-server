package com.pragmatix.notify.sender;

import com.google.android.gcm.server.*;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.notify.NotifySenderI;
import com.pragmatix.notify.Registrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 11.12.13 15:52
 */
public class GooglePushNotificationSender implements NotifySenderI {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String apiKey;

    private Sender sender;

    @Resource
    private Registrator registrator;

    @PostConstruct
    public void init() {
        sender = new Sender(apiKey);
    }

    @Override
    public boolean send(String registrationId, int timeToLive, String localizedMessage) {
        final List<String> devices = new ArrayList<String>(1);
        devices.add(registrationId);

        Message message = new Message.Builder()
                .timeToLive(timeToLive)
                .addData("title", localizedMessage)
                .build();
        MulticastResult multicastResult;
        try {
            multicastResult = sender.send(message, devices, 3);
        } catch (IOException e) {
            log.error("Error posting messages: " + e.toString(), e);
            return false;
        }
        List<Result> results = multicastResult.getResults();
        // analyze the results
        for(int i = 0; i < devices.size(); i++) {
            String regId = devices.get(i);
            Result result = results.get(i);
            String messageId = result.getMessageId();
            if(messageId != null) {
                if(log.isDebugEnabled()) {
                    log.debug("Succesfully sent message to device: " + regId + "; messageId = " + messageId);
                }
                String canonicalRegId = result.getCanonicalRegistrationId();
                if(canonicalRegId != null) {
                    // same device has more than on registration id: update it
                    registrator.updateRegistration(registrationId, canonicalRegId);
                }

                return true;
            } else {
                String error = result.getErrorCodeName();
                if(error.equals(Constants.ERROR_NOT_REGISTERED)) {
                    registrator.unregistrate(registrationId);
                } else {
                    log.error("Error sending message to " + regId + ": " + error);
                }
            }
        }

        return false;
    }


    @Override
    public SocialServiceEnum getSocialNetId() {
        return SocialServiceEnum.android;
    }

    public static void main(String[] args) {
        GooglePushNotificationSender notificationSender = new GooglePushNotificationSender();
        notificationSender.setApiKey("AIzaSyBZ1jEYrEAnUgDYQb6GHoqXObtnzUd0sH8");
        notificationSender.init();
        String regId;

        String msg = "Hello from Krasnodar! " + new Date();
//        // 47 john.boroda@gmail.com
//        regId = "APA91bH13F50Hz3WD26JcDsiQ9d9OWKcmvQ04mSO7U5AFVCab7uyKbUBj5vx3PigXJAHMM6-mTyZjHvRLfY8GJw98I8hK0QnyHS-i_A8i1ebAiqMNS9RaaVLghLbptJlP7eTwDfkue8r";
//        notificationSender.send(regId, 3600, msg);
//        // 42 vdddslep@gmail.com
//        regId = "APA91bFyRFXGE1g_-TAFDrfMSs9_JEbJs1Cc7LivLXTXPV3BtfxXDZhr1fCEdWGFggA0AdBu8aASJnQCKadn3YF4Hvoulm2w7ldko-vdyoMaYDxPDoXIRuNbWJcyY0yXVpZGeYlrpFdj";
//        notificationSender.send(regId, 3600, msg);
        // 29568 alx.ponomarenko@gmail.com
        regId = "APA91bElijUuPMJOykwtbQa422E55kJpBhsQac388O1vaMis6dlEDFn-ZWE5pCFRw6WTTkgBeoqzipqcJP2mBJ_4dyfjgapCaFk4F8PpTnhI9fGD4eAmU8s12OS3wS19nvHtLDsyeBqpp1G6ECd8l9HHd6INw2tVrJlU9xPjxIapf40Jw7-osFY";
        notificationSender.send(regId, 3600, msg);
    }

    //====================== Getters and Setters =================================================================================================================================================

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
