package com.pragmatix.intercom.service;

import com.pragmatix.achieve.domain.RemoteServerRequestMeta;
import com.pragmatix.gameapp.messages.Messages;
import com.pragmatix.intercom.messages.IntercomRequestI;
import com.pragmatix.sessions.IAppServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 * Created: 19.05.2017 15:04
 */
public abstract class ServerAPI {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private TimeUnit responseTimeoutUnit = TimeUnit.SECONDS;

    private long responseTimeoutInSeconds = 1;

    protected <R> R ask(IntercomRequestI request, List<RemoteServerRequestMeta> remoteServerRequestQueue, IAppServer destServer, Object msg, R errorResult) {
        return ask(request, remoteServerRequestQueue, destServer, msg, errorResult, responseTimeoutInSeconds);
    }

    protected <R> R ask(IntercomRequestI request, List<RemoteServerRequestMeta> remoteServerRequestQueue, IAppServer destServer, Object msg, R errorResult, long responseTimeoutInSeconds) {
        CompletableFuture<R> future = new CompletableFuture<>();
        long timeout = responseTimeoutUnit.toMillis(responseTimeoutInSeconds);
        remoteServerRequestQueue.add(new RemoteServerRequestMeta(timeout, msg, request.getRequestId(), future));
        Messages.toServer(request, destServer, true);
        try {
            return future.get(responseTimeoutInSeconds, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            log.error("[{}] на команду {} не получено подверждение в течении {} сек. [{}]", destServer, request, responseTimeoutInSeconds, msg);
            return errorResult;
        }
    }

}
