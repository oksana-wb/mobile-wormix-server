package com.pragmatix.steam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.steam.web.request.*;
import com.pragmatix.steam.web.responses.*;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 07.03.2017 14:11
 *         <p>
 * Реализация клиента, взаимодействующего со Steam Web API - в первую очередь, для совершения in-game платежей
 */
@Component
public class SteamWebAPI {

    private final Logger log = SocialService.PAYMENT_LOGGER;

    @Value("${steam.appId:0}")
    private int appId = 0;

    @Value("${steam.publisherKey:}")
    private String publisherKey = "";

    @Value("${debug.paymentResultSuccess:false}")
    private boolean debugPaymentMode = false;

    private int connectionTimeout = 1000;
    private int readTimeout = 15000;

    private String apiUrl = "https://api.steampowered.com"; // or https://partner.steam-api.com (see https://partner.steamgames.com/documentation/webapi#host)

    private ObjectMapper jackson = new ObjectMapper();

    public GetUserInfoResponse getUserInfo(GetUserInfoRequest request) {
        return callMethod(getPaymentInterface(), request, GetUserInfoResponse.class);
    }

    public InitTxnResponse initTxn(InitTxnRequest request) {
        return callMethod(getPaymentInterface(), request, InitTxnResponse.class);
    }

    public FinalizeTxnResponse finalizeTxn(FinalizeTxnRequest request) {
        return callMethod(getPaymentInterface(), request, FinalizeTxnResponse.class);
    }

    public QueryTxnResponse queryTxn(QueryTxnRequest request) {
        return callMethod(getPaymentInterface(), request, QueryTxnResponse.class);
    }

    protected <REQ extends SteamWebRequest, RESP extends SteamWebResponse<REQ>>
    RESP callMethod(SteamAPIInterface iface, REQ request, Class<RESP> responseClass) {
        return callMethod(iface, request.getMethod(), request.getMethod().getVersion(), request, responseClass);
    }
    protected <REQ extends SteamWebRequest, RESP extends SteamWebResponse<REQ>>
    RESP callMethod(SteamAPIInterface iface, SteamAPIInterface.Method method, int version, REQ request, Class<RESP> responseClass) {
        if (iface.supports(method)) {

            log.info(">> Steam request: {}", request);

            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(connectionTimeout)
                    .setSocketTimeout(readTimeout)
                    .build();
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .build();

            RequestBuilder builder = RequestBuilder.create(method.getHttpMethod())
                    .setUri(String.format("%s/%s/%s/V%04d/", apiUrl, iface, method, version));
            List<NameValuePair> params = request.toNameValues(appId);
            params.add(SteamWebRequest.newPair("format", "json"));
            params.add(SteamWebRequest.newPair("key", publisherKey));
            if (method.getHttpMethod().equals("GET")) {
                // параметры в URL
                params.forEach(builder::addParameter);
            } else {
                // параметры в теле запроса (можно было бы тоже через builder.addParameter, но получается неправильная кодировка)
                builder.setEntity(new UrlEncodedFormEntity(params, Charsets.UTF_8));
            }
            HttpUriRequest httpRequest = builder.build();

            if (log.isTraceEnabled()){
                log.trace(">>> HTTP request: {}\n>>> Headers: {}\n>>> {}",
                        obfuscateKey(httpRequest.getRequestLine().toString()),
                        Arrays.toString(httpRequest.getAllHeaders()),
                        obfuscateKey(debugEntityToString(httpRequest)));
            }

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
                if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    log.error("<<< (!) HTTP error: {}\n<<< (!) {}",
                            httpResponse.getStatusLine(),
                            obfuscateKey(debugEntityToString(httpResponse.getEntity()))
                    );
                    return createErrorResponse(request, CustomErrors.BAD_HTTP_STATUS, CustomErrors.BAD_HTTP_STATUS_DESC + " " + httpResponse.getStatusLine(), null, responseClass);
                }
                String data = EntityUtils.toString(httpResponse.getEntity(), Charsets.UTF_8);
                if (log.isTraceEnabled()){
                    log.trace("<<< HTTP response: \n<<< {}", data);
                }

                // parse json
                BaseResponseBody body = jackson.readValue(data, BaseResponseBody.class);
                if (body.response == null) {
                    log.error("<<< (!) Fail: {}", CustomErrors.NULL_RESPONSE_DESC);
                    return createErrorResponse(request, CustomErrors.NULL_RESPONSE, CustomErrors.NULL_RESPONSE_DESC, null, responseClass);
                }
                RESP response;
                if (body.response.params != null) {
                    response = jackson.treeToValue(body.response.params, responseClass);
                } else {
                    // params может отсутствовать в случае ошибки. Поэтому создаем response с пустыми полями, и ниже прописываем result и error
                    response = responseClass.newInstance();
                }
                response.request = request;
                response.result = body.response.result;
                response.error = body.response.error;

                log.info("<< Steam response: {}", response);

                return response;
            } catch (JsonProcessingException e) {
                log.error("<<< (!) JsonProcessingException: " + e.getMessage(), e);
                return createErrorResponse(request, CustomErrors.BAD_JSON, CustomErrors.BAD_JSON_DESC, e, responseClass);
            } catch (IOException e) {
                log.error("<<< (!) IOException: " + e.getMessage(), e);
                return createErrorResponse(request, CustomErrors.CONN_FAILED, CustomErrors.CONN_FAILED_DESC, e, responseClass);
            } catch (Exception e) {
                log.error("<<< (!) Exception: " + e.getMessage(), e);
                return createErrorResponse(request, CustomErrors.UNEXPECTED_EXCEPTION, CustomErrors.UNEXPECTED_EXCEPTION_DESC, e, responseClass);
            }
        } else {
            String errorDesc = "Steam WebAPI interface " + iface + " doesn't support method " + method;
            log.error("Internal error: {}", errorDesc);
            return createErrorResponse(request, CustomErrors.INTERNAL_ERROR, errorDesc, null, responseClass);
        }
    }

    protected <RESP extends SteamWebResponse<REQ>, REQ extends SteamWebRequest>
    RESP createErrorResponse(REQ request, int errCode, String errDesc, Exception cause, Class<RESP> responseClass) {
        RESP response;
        try {
            response = responseClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Error creating response class " + responseClass.getName(), e);
            throw new RuntimeException(e);
        }
        response.request = request;
        response.result = SteamWebResponse.Result.CustomError;
        response.error = new SteamWebResponse.ErrorBlock();
        response.error.errorcode = errCode;
        response.error.errordesc = errDesc;
        response.error.cause = cause;
        return response;
    }

    private static class BaseResponseBody {
        public BaseResponseFields response;

        public static class BaseResponseFields {
            public SteamWebResponse.Result result;
            public SteamWebResponse.ErrorBlock error;
            public ObjectNode params;
        }
    }

    private SteamAPIInterface getPaymentInterface() {
        return debugPaymentMode ? SteamAPIInterface.ISteamMicroTxnSandbox : SteamAPIInterface.ISteamMicroTxn;
    }


    private String obfuscateKey(String logMessage) {
        if (logMessage == null) {
            return null;
        } else {
            return logMessage.replaceAll(publisherKey, "SECRET");
        }
    }

    private String debugEntityToString(HttpRequest httpRequest) {
        if (httpRequest instanceof HttpEntityEnclosingRequestBase) {
            return debugEntityToString(((HttpEntityEnclosingRequestBase) httpRequest).getEntity());
        } else {
            return "";
        }
    }
    private String debugEntityToString(HttpEntity entity) {
        try {
            if (entity != null) {
                return EntityUtils.toString(entity, Charsets.UTF_8);
            } else {
                return "null";
            }
        } catch (IOException e) {
            return "?!?!";
        }
    }


    public void setAppId(int appId) {
        this.appId = appId;
    }

    public int getAppId() {
        return appId;
    }

    public void setPublisherKey(String publisherKey) {
        this.publisherKey = publisherKey;
    }

    public void setDebugPaymentMode(boolean enabled) {
        this.debugPaymentMode = enabled;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public boolean isDebugPaymentMode() {
        return debugPaymentMode;
    }
}
