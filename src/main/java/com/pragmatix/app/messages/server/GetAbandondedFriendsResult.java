package com.pragmatix.app.messages.server;

import com.pragmatix.serialization.TypeSize;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Resize;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 25.09.12 12:45
 */
@Command(10089)
public class GetAbandondedFriendsResult {

    @Resize(TypeSize.UINT32)
    public long[] abandondedFriendsPage;

    public String[] abandondedFriendsPageString;

    public int totalAbandondedFriends;

    public GetAbandondedFriendsResult() {
    }

    public GetAbandondedFriendsResult(Long[] abandondedFriendsPage, String[] abandondedFriendsPageString, int totalAbandondedFriends) {
        this.abandondedFriendsPage = ArrayUtils.toPrimitive(abandondedFriendsPage);
        this.abandondedFriendsPageString = abandondedFriendsPageString;
        this.totalAbandondedFriends = totalAbandondedFriends;
    }

    @Override
    public String toString() {
        return "GetAbandondedFriendsResult{" +
                "abandondedFriendsPage=" + (abandondedFriendsPage == null ? null : Arrays.asList(abandondedFriendsPage)) +
                "abandondedFriendsPageString=" + (abandondedFriendsPageString == null ? null : Arrays.asList(abandondedFriendsPageString)) +
                ", totalAbandondedFriends=" + totalAbandondedFriends +
                '}';
    }

}
