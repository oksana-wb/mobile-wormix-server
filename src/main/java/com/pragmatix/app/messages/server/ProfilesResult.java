package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.structures.UserProfileStructure;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;
import java.util.List;

/**
 * ответ от сервера на запрос списка профайлов
 * <p>
 * User: denis
 * Date: 05.12.2009
 * Time: 1:26:16
 */
@Command(10005)
public class ProfilesResult {
    /**
     * список профайлов
     */
    public List<UserProfileStructure> profileStructures;

    public ProfilesResult() {
    }

    public ProfilesResult(List<UserProfileStructure> profileStructures) {
        this.profileStructures = profileStructures;
    }

    @Override
    public String toString() {
        return "ProfilesResult{" +
                "profileStructures=" + profileStructures +
                '}';
    }
}
