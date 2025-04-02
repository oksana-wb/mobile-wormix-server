package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.serialization.annotations.Command;

import java.util.List;

/**
 * Результат выполнения команды GetFriendListPage
 * User: denis
 * Date: 17.04.2010
 * Time: 17:07:08
 */
@Command(10018)
public class GetFriendListPageResult {
    /**
     * список профайлов
     */
    public List<UserProfileStructure> profileStructures;

    public GetFriendListPageResult() {
    }

    public GetFriendListPageResult(List<UserProfileStructure> profileStructures) {
        this.profileStructures = profileStructures;
    }

    @Override
    public String toString() {
        return "GetFriendListPageResult{(" + profileStructures.size() + ")" +
                profileStructures +
                '}';
    }
}
