package com.pragmatix.app.common;

import com.pragmatix.serialization.annotations.Ignore;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.09.13 17:18
 */
public class OrderBean {

    public PaymentType paymentType;
    public int paymentAmount;
    public int paymentAmountComeback;
    public float paymentCost;

    public int period;
    public int trialDuration;

    public OrderBean(PaymentType paymentType, int paymentAmount, float paymentCost) {
        this.paymentType = paymentType;
        this.paymentAmount = paymentAmount;
        this.paymentCost = paymentCost;
    }

    public OrderBean(PaymentType paymentType, int paymentAmount, float paymentCost, int period,  int trialDuration) {
        this.paymentType = paymentType;
        this.paymentAmount = paymentAmount;
        this.paymentCost = paymentCost;
        this.period = period;
        this.trialDuration = trialDuration;
    }

    public OrderBean(PaymentType paymentType, int paymentAmount, int paymentAmountComeback, float paymentCost) {
        this.paymentType = paymentType;
        this.paymentAmount = paymentAmount;
        this.paymentAmountComeback = paymentAmountComeback;
        this.paymentCost = paymentCost;
    }

    @Override
    public String toString() {
        return "OrderBean{" +
                "paymentType=" + paymentType +
                ", paymentAmount=" + paymentAmount +
                ", paymentAmountComeback=" + paymentAmountComeback +
                ", paymentCost=" + paymentCost +
                ", period=" + period +
                ", trialDuration=" + trialDuration +
                '}';
    }

    public int getPaymentCostInt() {
        return (int) paymentCost;
    }
}
