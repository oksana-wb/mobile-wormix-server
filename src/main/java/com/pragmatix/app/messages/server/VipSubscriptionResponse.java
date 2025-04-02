package com.pragmatix.app.messages.server;

import com.pragmatix.app.messages.client.GetVipSubscription;
import com.pragmatix.gameapp.social.service.vkontakte.UserSubscription;
import com.pragmatix.serialization.annotations.Command;

import java.util.Optional;

/**
 * @see GetVipSubscription
 */
@Command(10140)
public class VipSubscriptionResponse {

    public enum Status {
        nil,
        chargeable,
        active,
        cancelled,
    }

    public enum CancelReason {
        nil,
        user_decision,
        app_decision,
        payment_fail,
        unknown,
    }

    public int id; //(integer) — идентификатор подписки.
    public String item_id; // (string) — идентификатор товара в приложении.
    public Status status = Status.nil; //(string) — статус подписки. Возможные значения:
    public int price;  //(integer) — стоимость подписки.
    public int period;  //(integer) — период подписки.
    public int period_start_time;  //(integer) — дата начала периода в Unixtime.
    public int next_bill_time;  //(integer) — дата следующего платежа в Unixtime (если status = active).
    public boolean pending_cancel;  //(boolean, [true]) — true, если подписка ожидает отмены.
    public CancelReason cancel_reason = CancelReason.nil;  //(string) — причина отмены (если есть)

    public VipSubscriptionResponse() {
    }

    public VipSubscriptionResponse(int id) {
        this.id = id;
    }

    public VipSubscriptionResponse(UserSubscription subscription) {
        this.id = subscription.id;
        this.item_id = subscription.item_id;
        this.status = Status.valueOf(subscription.status.name());
        this.price = subscription.price;
        this.period = subscription.period;
        this.period_start_time = subscription.period_start_time;
        this.next_bill_time = subscription.next_bill_time;
        this.pending_cancel = subscription.pending_cancel;
        this.cancel_reason = Optional.ofNullable(subscription.cancel_reason).map(cancel_reason -> CancelReason.valueOf(cancel_reason.name())).orElse(CancelReason.nil);
    }

    @Override
    public String toString() {
        return "VipSubscriptionResponse{" +
                "id=" + id +
                ", item_id='" + item_id + '\'' +
                ", status=" + status +
                ", price=" + price +
                ", period=" + period +
                ", period_start_time=" + period_start_time +
                ", next_bill_time=" + next_bill_time +
                ", pending_cancel=" + pending_cancel +
                ", cancel_reason=" + cancel_reason +
                '}';
    }

}
