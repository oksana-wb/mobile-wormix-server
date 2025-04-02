package com.pragmatix.steam;

import com.pragmatix.df.social.steam.SteamTicket;
import com.pragmatix.gameapp.social.ISocialService;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.gameapp.social.SocialServiceId;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Arrays;
import java.util.Date;

/**
 * Author: Vladimir
 * Date: 08.08.2016 11:45
 */
public class SteamService implements ISocialService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int TICKET_DATA_IX_STEAM_ID = 0;
    private static final int TICKET_DATA_IX_APP_ID = 1;
    private static final int TICKET_DATA_IX_BAN_STATUS = 2;
    private static final int TICKET_DATA_IX_OWNER_STATUS = 3;
    private static final int TICKET_DATA_IX_ISSUE_TIME = 4;
    private static final int TICKET_DATA_IX_EOF = 5;

    private int appId;

    private byte[] privateKey;

    private String[] libList;

    private int ticketExpireTime = 10 * 60 * 1000;

    private boolean debugMode = false;

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public void setPrivateKey(String privateKey) throws DecoderException {
        this.privateKey = Hex.decodeHex(privateKey.toCharArray());
    }

    public void setLibList(String[] libList) {
        this.libList = libList;
    }

    public void setTicketExpireTime(int ticketExpireTime) {
        this.ticketExpireTime = ticketExpireTime;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @PostConstruct
    public void init() {
        for(String lib : libList) {
            System.load(new File(lib).getAbsolutePath());
        }
    }

    @Override
    public SocialServiceId getSocialId() {
        return SocialServiceEnum.steam;
    }

    @Override
    public boolean checkKey(Object oSocialUserId, String authKey) {
        if(debugMode) {
            logger.warn("turn off validate SteamTicket!!!");
            return true;
        }

        String socialUserId = (String) oSocialUserId;
        if(socialUserId.contains("#")) {
            //allStringId like 12#76561198398194527#...#...
            String[] ss = socialUserId.split("#");
            for (int i = 0; i < ss.length - 1; i++) {
                if (ss[i].equals("" + getSocialId().getType())) {
                    socialUserId = ss[i + 1];
                    break;
                }
            }
        }

        try {
            byte[] encryptedData = Hex.decodeHex(authKey.toCharArray());// new BASE64Decoder().decodeBuffer(authKey);
            byte[] decryptedData = new byte[1024];

            int[] decryptedDataLen = new int[1];
            decryptedDataLen[0] = decryptedData.length;

            SteamTicket ticket;

            if(SteamTicket.decryptTicket(encryptedData, decryptedData, decryptedDataLen, privateKey)) {
                decryptedData = Arrays.copyOf(decryptedData, decryptedDataLen[0]);

                long[] ticketFields = new long[TICKET_DATA_IX_EOF];

                if(SteamTicket.extractTicketFields(decryptedData, ticketFields)) {
                    ticket = new SteamTicket();
                    ticket.decryptedData = decryptedData;
                    ticket.steamId = ticketFields[TICKET_DATA_IX_STEAM_ID];
                    ticket.appId = (int) ticketFields[TICKET_DATA_IX_APP_ID];
                    ticket.banStatus = (int) ticketFields[TICKET_DATA_IX_BAN_STATUS];
                    ticket.ownerStatus = (int) ticketFields[TICKET_DATA_IX_OWNER_STATUS];
                    ticket.issueTime = new Date(1000L * ticketFields[TICKET_DATA_IX_ISSUE_TIME]);
                } else {
                    logger.warn(String.format("Can't extract fields: ticket %s, user %s", authKey, socialUserId));

                    return false;
                }
            } else {
                logger.warn(String.format("Can't decrypt: ticket %s, user %s", authKey, socialUserId));

                return false;
            }

            if(!socialUserId.equals(Long.toString(ticket.steamId))) {
                logger.warn(String.format("User steam id %s doesn't match ticket steam id %s", socialUserId, ticket.steamId));

                return false;
            }

            if(appId != ticket.appId) {
                logger.warn(String.format("Invalid ticket appId %d, userId %s", ticket.appId, socialUserId));

                return false;
            }

            if(ticket.banStatus != 0) {
                logger.warn(String.format("User %s banned by steam", socialUserId));

                return false;
            }

            if(ticket.ownerStatus != 1) {
                logger.warn(String.format("User %s doesn't owns the app", socialUserId));

                return false;
            }

            if(System.currentTimeMillis() - ticket.issueTime.getTime() >= ticketExpireTime) {
                logger.warn(String.format("User %s ticket expired, issue time %s", socialUserId, ticket.issueTime));

                return false;
            }
        } catch (Exception e) {
            logger.error(e.toString(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean checkSig(Object bean, String sig) {
        return false;
    }

    @Override
    public boolean sendMessage(Object socialUserId, String message) {
        return false;
    }

    @Override
    public boolean sendNotification(Object socialUserId, String message) {
        return false;
    }

    @Override
    public boolean setUserLevel(Object socialUserId, int level) {
        return false;
    }
}
