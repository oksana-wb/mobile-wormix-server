package com.pragmatix.intercom.messages;

import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Resize;
import com.pragmatix.serialization.annotations.Structure;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.05.12 12:34
 */
@Structure(isAbstract = true)
public abstract class IntercomResponse implements IntercomResponseI {

    @Resize(TypeSize.UINT32)
    public long profileId;

    public byte socialNetId;

    public long requestId;

    protected IntercomResponse() {
    }

    protected IntercomResponse(IntercomRequestI request) {
        this.requestId = request.getRequestId();
        this.profileId = request.getProfileId();
        this.socialNetId = request.getSocialNetId();
    }

    @Override
    public long getProfileId() {
        return profileId;
    }

    @Override
    public byte getSocialNetId() {
        return socialNetId;
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

}
