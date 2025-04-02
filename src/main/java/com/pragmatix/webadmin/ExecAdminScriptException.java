package com.pragmatix.webadmin;

import com.pragmatix.wormix.webadmin.interop.ServiceResult;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.01.13 11:31
 */
public class ExecAdminScriptException extends RuntimeException {

    private ServiceResult result;

    public ExecAdminScriptException(ServiceResult result, String message) {
        super(message);
        this.result = result;
    }

    public ServiceResult getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "ExecAdminScriptException: " +
                result + ": " +
                getMessage();
    }
}
