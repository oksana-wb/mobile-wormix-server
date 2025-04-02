package com.pragmatix.app.domain;

import com.pragmatix.gameapp.common.Identifiable;

import java.util.Date;

/**
 * User: denis
 * Date: 13.11.2009
 * Time: 1:02:43
 */
public class PaymentStatisticEntity implements Identifiable<Integer> {

    /**
     * id записи
     */
    private Integer id;

    /**
     * id профайла которому принадлежит запись
     */
    private Long profileId;

    /**
     * тип денег
     */
    public int moneyType;

    /**
     * количество голосов
     */
    private float votes;

    /**
     * время
     */
    private Date date;

	/**
	 * удалось ли полностью провести платеж
	 */
	private boolean completed;

    /**
     * профайл пользователя которому принадлежит статистика
     */
    private UserProfileEntity userProfile;

    /**
     * статус платежа
     * 1 - платеж зарегистрирован, но не обработан
     * 0 - платеж обработан успешно
     * 2 - контакт не подтвердил платеж
     * 3 - во время обработки платежа произошли ошибки
     */
    private int paymentStatus = 1;
    
    private Date updateDate;

    private String transactionId;

    private int amount;

    private int balanse;

    private String item;

    /**
     * @return id записи
     */
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return профайл пользователя которому принадлежит статистика
     */
    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public int getMoneyType() {
        return moneyType;
    }

    public void setMoneyType(int moneyType) {
        this.moneyType = moneyType;
    }

    /**
     * @return количество голосов
     */
    public float getVotes() {
        return votes;
    }

    public void setVotes(float votes) {
        this.votes = votes;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	/**
     * @return профайл пользователя которому принадлежит запись
     */
    public UserProfileEntity getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfileEntity userProfile) {
        this.userProfile = userProfile;
    }

    public int getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(int paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PaymentStatisticEntity that = (PaymentStatisticEntity) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getBalanse() {
        return balanse;
    }

    public void setBalanse(int balanse) {
        this.balanse = balanse;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    @Override
    public String toString() {
        return "PaymentStatisticEntity{" +
                "id=" + id +
                ", profileId=" + profileId +
                ", moneyType=" + moneyType +
                ", votes=" + votes +
                ", amount=" + amount +
                ", item=" + item +
                ", balanse=" + balanse +
                ", date=" + date +
                ", completed=" + completed +
                ", paymentStatus=" + paymentStatus +
                ", updateDate=" + updateDate +
                ", transactionId=" + transactionId +
                '}';
    }
}
