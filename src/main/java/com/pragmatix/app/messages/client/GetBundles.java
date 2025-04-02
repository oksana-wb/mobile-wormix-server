package com.pragmatix.app.messages.client;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

/**
 * @see com.pragmatix.app.controllers.ShopController#onGetBundles(GetBundles, UserProfile)
 */
@Command(116)
public class GetBundles {

    public GetBundles() {
    }

    @Override
    public String toString() {
        return "GetBundles{}";
    }
}
