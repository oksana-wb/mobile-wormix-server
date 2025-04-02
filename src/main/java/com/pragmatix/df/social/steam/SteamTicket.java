package com.pragmatix.df.social.steam;

import java.util.Date;

/**
 * Author: Vladimir
 * Date: 11.08.2016 9:48
 *
 * НИКОГДА не переноси SteamTicket из пакета COM.PRAGMATIX.DF.SOCIAL.STEAM, иначе отвалится нативная библиотека
 */
public strictfp class SteamTicket {
    public long steamId;
    public int appId;
    public int banStatus;
    public int ownerStatus;
    public Date issueTime;
    
    public byte[] decryptedData;

    public static native void doNothing();

    public static native int add(int a, int b);

    public static native boolean decryptTicket(byte[] encryptedTicket, byte[] decryptedTicket,
                                        int[] decryptedTicketLength, byte[] privateKey);

    public static native boolean isTicketForApp(byte[] decryptedTicket, int appId);

    public static native long getSteamId(byte[] decryptedTicket);

    public static native int getAppId(byte[] decryptedTicket);

    public static native int getBanStatus(byte[] decryptedTicket);

    public static native int getOwnerStatus(byte[] decryptedTicket, int appId);

    public static native long getIssueTime(byte[] decryptedTicket);

    public static native boolean extractTicketFields(byte[] decryptedTicket, long[] ticketFields);

    @Override
    public String toString() {
        return "EncryptedTicket{" +
                "steamId=" + steamId +
                ", appId=" + appId +
                ", banStatus=" + banStatus +
                ", ownerStatus=" + ownerStatus +
                ", issueTime=" + issueTime +
                '}';
    }
}
