package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.ServiceResult;
import com.pragmatix.clanserver.messages.request.AbstractRequest;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Structure;

/**
 * Author: Vladimir
 * Date: 05.04.2013 09:15
 */
@Structure(isAbstract = true)
public class CommonResponse<REQUEST extends AbstractRequest> {
    /**
     * исходный запрос
     */
    @Ignore
    public REQUEST request;

    /**
     * код завершения
     */
    public ServiceResult serviceResult = ServiceResult.OK;

    @Ignore
    public String logMessage;

    public CommonResponse() {
    }

    public CommonResponse(REQUEST request) {
        this(ServiceResult.OK, request, null);
    }

    public CommonResponse(ServiceResult serviceResult, REQUEST request, String logMessage) {
        this.serviceResult = serviceResult;
        this.logMessage = logMessage;
        this.request = request;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + propertiesString() + '}';
    }
    
    protected StringBuilder propertiesString() {
        StringBuilder properties = new StringBuilder()
                .append("serviceResult=").append(serviceResult.name()).append(":").append(serviceResult.code);

        if (logMessage != null) {
            properties.append(", logMessage=").append(logMessage);
        }

        return properties;
    }

    public boolean isOk() {
        return ServiceResult.OK == serviceResult;
    }
}
