package com.pragmatix.steam.web.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pragmatix.steam.web.request.SteamWebRequest;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 07.03.2017 16:16
 *         <p>
 * Базовый класс ответов на запросы к steam web api
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class SteamWebResponse<REQ extends SteamWebRequest> {

    @JsonIgnore
    public REQ request;

    @JsonIgnore
    public Result result;

    @JsonIgnore
    public ErrorBlock error;

    public enum Result {
        OK,          // Steam сообщил об успехе
        Failure,     // Steam сообщил об ошибке
        CustomError  // "Наша" ошибка, произошедшая до запроса к Steam'у или при обработке ответа от него
    }
    public static class ErrorBlock {
        /**
         * код ошибки из {@link SteamErrors} или {@link CustomErrors}
         */
        public int errorcode;
        public String errordesc;

        @JsonIgnore
        public Exception cause;

        @Override
        public String toString() {
            return errorcode + ": \"" + errordesc + '"' +
                    (cause != null ? " " + cause.getClass().getSimpleName() +": \"" + cause.getMessage() + '"' : "");
        }
    }

    // for json deserialization
    protected SteamWebResponse() {}

    protected SteamWebResponse(REQ request) {
        this.request = request;
    }

    public boolean isSuccess() {
        return result == Result.OK;
    }


    @Override
    public String toString() {
        return "result=" + result +
                (error != null ? ", error=" + error : "");
    }
}
