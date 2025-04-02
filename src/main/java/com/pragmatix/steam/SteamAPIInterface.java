package com.pragmatix.steam;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static com.pragmatix.steam.SteamAPIInterface.Method.*;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 07.03.2017 15:52
 *         <p>
 * Список всех поддерживаемых интерфейсов Steam Web API и их методов
 */
public enum SteamAPIInterface {
    ISteamMicroTxn(GetUserInfo, InitTxn, FinalizeTxn, QueryTxn),
    ISteamMicroTxnSandbox(GetUserInfo, InitTxn, FinalizeTxn, QueryTxn),
    ISteamEconomy(GetAssetPrices),
    ;

    private Set<Method> supportedMethods;

    public enum Method {
        GetUserInfo("GET", 1),
        InitTxn("POST", 2),
        FinalizeTxn("POST", 1),
        QueryTxn("GET", 1),
        GetAssetPrices("GET", 1)
        ;

        private SteamAPIInterface apiInterface;
        private String httpMethod;
        private int version;

        Method(String httpMethod, int version) {
            this.httpMethod = httpMethod;
            this.version = version;
        }

        public SteamAPIInterface getAPIInterface() {
            if (apiInterface == null) {
                throw new IllegalStateException("Steam API method "+name()+"is for unknown interface");
            }
            return apiInterface;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public boolean isPassParamsInURL() {
            return "GET".equals(httpMethod);
        }

        public int getVersion() {
            return version;
        }
    }

    SteamAPIInterface(Method... methods) {
        this.supportedMethods = EnumSet.copyOf(Arrays.asList(methods));
        for (Method method : methods) {
            if (method.apiInterface != null) {
                method.apiInterface = this;
            }
        }
    }

    public boolean supports(Method method) {
        return supportedMethods.contains(method);
    }
}
