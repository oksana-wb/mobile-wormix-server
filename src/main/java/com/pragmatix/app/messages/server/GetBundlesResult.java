package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.client.GetBundles;
import com.pragmatix.app.messages.structures.BundleStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

import java.util.Arrays;
import java.util.List;

/**
 * @see com.pragmatix.app.controllers.ShopController#onGetBundles(GetBundles, UserProfile)
 */
@Command(10116)
public class GetBundlesResult {

    public List<BundleStructure> bundleStructures;

    public GetBundlesResult() {
    }

    public GetBundlesResult(List<BundleStructure> bundleStructures) {
        this.bundleStructures = bundleStructures;
    }

    @Override
    public String toString() {
        return "GetBundlesResult{" + bundleStructures + '}';
    }

}
