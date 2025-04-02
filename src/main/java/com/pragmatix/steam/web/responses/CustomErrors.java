package com.pragmatix.steam.web.responses;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 09.03.2017 16:20
 *         <p>
 * Список "наших" внутренних ошибок, возвращаемых из методов {@link com.pragmatix.df.server.social.steam.SteamWebAPI}
 * и помещаемых в {@link SteamWebResponse#error}
 *
 * @see SteamErrors - документированые коды ошибок, которые возвращает стим
 */
public class CustomErrors {

    private static final int ERR_BASE = 1000;

    public static final int CONN_FAILED = ERR_BASE + 1;
    public static final String CONN_FAILED_DESC = "Error connecting to Steam Web API";

    public static final int BAD_HTTP_STATUS = ERR_BASE + 2;
    public static final String BAD_HTTP_STATUS_DESC = "Got HTTP response with non-OK status";

    public static final int BAD_JSON = ERR_BASE + 3;
    public static final String BAD_JSON_DESC = "Response is not correct JSON format";

    public static final int UNEXPECTED_EXCEPTION = ERR_BASE + 4;
    public static final String UNEXPECTED_EXCEPTION_DESC = "Unexpected exception thrown";

    public static final int NULL_RESPONSE = ERR_BASE + 5;
    public static final String NULL_RESPONSE_DESC = "Response JSON didn't contain field 'response'";

    public static final int INTERNAL_ERROR = ERR_BASE + 5;
    public static final String INTERNAL_ERROR_DESC = "Error in source code: assertion failed";
}
