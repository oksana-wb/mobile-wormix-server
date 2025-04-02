package com.pragmatix.achieve.domain;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 12.08.2016 12:53
 */
public class RemoteServerRequestMeta {

    public final long createDate = System.currentTimeMillis();

    public final long timeout;

    public final Object causeRequest;

    public final long requestNum;

    public final CompletableFuture future;

    public RemoteServerRequestMeta(long timeout, Object causeRequest, long requestNum, CompletableFuture future) {
        this.timeout = createDate + timeout;
        this.causeRequest = causeRequest;
        this.requestNum = requestNum;
        this.future = future;
    }

    @Override
    public String toString() {
        return "RemoteServerRequestMeta{" +
                "createDate=" +  new SimpleDateFormat("HH:mm:ss").format(new Date(createDate)) +
                ", timeout=" + new SimpleDateFormat("HH:mm:ss").format(new Date(timeout))  +
                ", request=" + causeRequest +
                ", requestNum=" + requestNum +
                '}';
    }

}
