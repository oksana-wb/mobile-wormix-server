package com.pragmatix.clanserver.messages;

/**
 * Author: Vladimir
 * Date: 05.04.13 9:52
 */
public class Messages {
    private static final int REQUEST_BASE = 7000;
    private static final int RESPONSE_BASE = 17000;
    private static final int EVENT_BASE = 7100;

    public static final int LOGIN_REQUEST = REQUEST_BASE + 1;

    public static final int LOGIN_CREATE_REQUEST = REQUEST_BASE + 2;
    public static final int LOGIN_JOIN_REQUEST = REQUEST_BASE + 3;

    public static final int LOGIN_RESPONSE = RESPONSE_BASE + 1;
    public static final int LOGIN_ERROR_RESPONSE = RESPONSE_BASE + 3;

    public static final int LOGOUT_REQUEST = REQUEST_BASE + 7;

    public static final int DELETE_CLAN_REQUEST = REQUEST_BASE + 11;
    public static final int DELETE_CLAN_RESPONSE = RESPONSE_BASE + 11;

    public static final int INVITE_TO_CLAN_REQUEST = REQUEST_BASE + 102;
    public static final int INVITE_TO_CLAN_RESPONSE = RESPONSE_BASE + 102;

    public static final int EXPELL_FROM_CLAN_REQUEST = REQUEST_BASE + 104;
    public static final int EXPELL_FROM_CLAN_RESPONSE = RESPONSE_BASE + 104;

    public static final int QUIT_CLAN_REQUEST = REQUEST_BASE + 105;
    public static final int QUIT_FROM_CLAN_RESPONSE = RESPONSE_BASE + 105;

    public static final int PROMOTE_IN_RANK_REQUEST = REQUEST_BASE + 106;
    public static final int PROMOTE_IN_RANK_RESPONSE = RESPONSE_BASE + 106;

    public static final int LOWER_IN_RANK_REQUEST = REQUEST_BASE + 107;
    public static final int LOWER_IN_RANK_RESPONSE = RESPONSE_BASE + 107;

    public static final int EXPAND_CLAN_REQUEST = REQUEST_BASE + 108;
    public static final int EXPAND_CLAN_RESPONSE = RESPONSE_BASE + 108;

    public static final int POST_TO_CHAT_REQUEST = REQUEST_BASE + 109;
    public static final int POST_TO_CHAT_RESPONSE = RESPONSE_BASE + 109;
    public static final int CHAT_MESSAGE_EVENT = EVENT_BASE + 109;

    public static final int POST_NEWS_REQUEST = REQUEST_BASE + 110;
    public static final int POST_NEWS_RESPONSE = RESPONSE_BASE + 110;

    public static final int UPDATE_RATING_REQUEST = REQUEST_BASE + 111;
    public static final int UPDATE_RATING_RESPONSE = RESPONSE_BASE + 111;

    public static final int CLAN_SUMMARY_REQUEST = REQUEST_BASE + 112;
    public static final int CLAN_SUMMARY_RESPONSE = RESPONSE_BASE + 112;

    public static final int LIST_CLANS_REQUEST = REQUEST_BASE + 113;
    public static final int LIST_CLANS_RESPONSE = RESPONSE_BASE + 113;

    public static final int RENAME_CLAN_REQUEST = REQUEST_BASE + 114;
    public static final int RENAME_CLAN_RESPONSE = RESPONSE_BASE + 114;

    public static final int CHANGE_CLAN_EMBLEM_REQUEST = REQUEST_BASE + 115;
    public static final int CHANGE_CLAN_EMBLEM_RESPONSE = RESPONSE_BASE + 115;

    public static final int CHANGE_CLAN_DESCRIPTION_REQUEST = REQUEST_BASE + 116;
    public static final int CHANGE_CLAN_DESCRIPTION_RESPONSE = RESPONSE_BASE + 116;

    public static final int TOP_CLANS_REQUEST = REQUEST_BASE + 117;
    public static final int TOP_CLANS_RESPONSE = RESPONSE_BASE + 117;

    public static final int CHANGE_CLAN_REVIEW_STATE_REQUEST = REQUEST_BASE + 118;
    public static final int CHANGE_CLAN_REVIEW_STATE_RESPONSE = RESPONSE_BASE + 118;

    public static final int UPDATE_CLAN_REQUEST = REQUEST_BASE + 119;
    public static final int UPDATE_CLAN_RESPONSE = RESPONSE_BASE + 119;

    public static final int CHANGE_CLAN_JOIN_RATING_REQUEST = REQUEST_BASE + 120;
    public static final int CHANGE_CLAN_JOIN_RATING_RESPONSE = RESPONSE_BASE + 120;

    public static final int DONATE_REQUEST = REQUEST_BASE + 121;
    public static final int DONATE_RESPONSE = RESPONSE_BASE + 121;

    public static final int CHANGE_CLAN_CLOSED_STATE_REQUEST = REQUEST_BASE + 122;
    public static final int CHANGE_CLAN_CLOSED_STATE_RESPONSE = RESPONSE_BASE + 122;

    public static final int CHANGE_CLAN_MEDAL_PRICE_REQUEST = REQUEST_BASE + 123;
    public static final int CHANGE_CLAN_MEDAL_PRICE_RESPONSE = RESPONSE_BASE + 123;

    public static final int CASH_MEDALS_REQUEST = REQUEST_BASE + 124;
    public static final int CASH_MEDALS_RESPONSE = RESPONSE_BASE + 124;

    public static final int AUDIT_ACTIONS_REQUEST = REQUEST_BASE + 125;
    public static final int AUDIT_ACTIONS_RESPONSE = RESPONSE_BASE + 125;

    public static final int FINISH_GLADIATOR_SERIES_CHAT_ACTION = REQUEST_BASE + 126;

    public static final int SET_EXPEL_PERMIT_REQUEST = REQUEST_BASE + 127;
    public static final int SET_EXPEL_PERMIT_RESPONSE = RESPONSE_BASE + 127;

    public static final int SET_MUTE_MODE_REQUEST = REQUEST_BASE + 128;
    public static final int SET_MUTE_MODE_RESPONSE = RESPONSE_BASE + 128;

    public static final int CRAFT_LEGENDARY_ITEM_CHAT_ACTION = REQUEST_BASE + 129;

}
