package com.pragmatix.app.domain;

import com.pragmatix.app.common.MoneyType;
import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.gameapp.common.Identifiable;

import java.util.Date;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 18.04.2016 13:04
 *         <p>
 * Для хранения открытого (и постепенно получаемого) "депозита"
 */
public class DepositEntity  implements Identifiable<Integer> {

    /**
     * ID депозита
     */
    private Integer id;

    /**
     * ID игока, которому принадлежит депозит
     */
    private int profileId;

    /**
     * сколько выплачивается в каждый из дней
     */
    private String dividendsByDays;

    /**
     * в какой валюте депозит
     */
    private byte moneyType;

    /**
     * дата внесения
     */
    private Date startDate;

    /**
     * последняя выплаченная часть: от 1 до dividendsByDays.length, либо 0 - если ещё ничего не выплачено
     */
    private int progress;

    /**
     * дата последнего обновления progress (последней выплаты очередной части)
     */
    private Date lastPayDate;

    /**
     * флаг: выплачен до конца и более не активен (после того как progress достиг окончания)
     */
    private boolean paidOff;

    public DepositEntity() {
    }

    public DepositEntity(int id, Long profileId, MoneyType moneyType, String dividendsByDays, Date startDate) {
        this.id = id;
        this.profileId = profileId.intValue();
        this.moneyType = (byte) moneyType.getType();
        this.dividendsByDays = dividendsByDays;
        this.startDate = startDate;
        this.progress = 0;
        this.paidOff = false;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getDividendsByDays() {
        return dividendsByDays;
    }

    public void setDividendsByDays(String dividendsByDays) {
        this.dividendsByDays = dividendsByDays;
    }

    public byte getMoneyType() {
        return moneyType;
    }

    public void setMoneyType(byte moneyType) {
        this.moneyType = moneyType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isPaidOff() {
        return paidOff;
    }

    public void setPaidOff(boolean paidOff) {
        this.paidOff = paidOff;
    }

    public Date getLastPayDate() {
        return lastPayDate;
    }

    public void setLastPayDate(Date lastPayDate) {
        this.lastPayDate = lastPayDate;
    }

    @Override
    public String toString() {
        return "DepositEntity{" +
                "id=" + id +
                ", profileId=" + profileId +
                ", dividendsByDays=" + dividendsByDays +
                ", startDate=" + AppUtils.formatDate(startDate) +
                ", progress=" + progress +
                ", lastPayDate=" + AppUtils.formatDate(lastPayDate) +
                ", paidOff=" + paidOff +
                '}';
    }
}
