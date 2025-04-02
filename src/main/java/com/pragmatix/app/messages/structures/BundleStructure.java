package com.pragmatix.app.messages.structures;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.serialization.annotations.Ignore;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;
import java.util.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 30.07.2015 12:26
 */
@Structure
public class BundleStructure {

    @Ignore
    public Date finish;

    public int expireInSeconds;

    public String code;

    public int order;

    public int discount;

    public float votes;

    public GenericAwardStructure[] items;

    @Ignore
    public boolean vip;

    @Ignore
    public int period;

    @Ignore
    public boolean subscription;

    @Ignore
    public int trialDuration;

    public BundleStructure() {
    }

    @Override
    public String toString() {
        return "{" +
                "code='" + code + '\'' +
                ", order=" + order +
                ", votes=" + votes +
                (finish != null ? ", finish=" + AppUtils.formatDate(finish) : "") +
                (discount > 0 ? ", discount=" + discount : "") +
                ", items=" + Arrays.toString(items) +
                '}';
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public float getVotes() {
        return votes;
    }

    public void setVotes(float votes) {
        this.votes = votes;
    }

    public GenericAwardStructure[] getItems(UserProfile profile) {
        return items;
    }

    public void setItems(GenericAwardStructure[] items) {
        this.items = items;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public int getTrialDuration() {
        return trialDuration;
    }

    public void setTrialDuration(int trialDuration) {
        this.trialDuration = trialDuration;
    }

    public boolean isSubscriptionBundle(){
        return subscription;
    }

    public boolean isVip() {
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    public int getOrder() {
        return order;
    }

    public boolean isServerOnly(){
        return false;
    }

    public boolean isSubscription() {
        return subscription;
    }

    public void setSubscription(boolean subscription) {
        this.subscription = subscription;
    }
}
