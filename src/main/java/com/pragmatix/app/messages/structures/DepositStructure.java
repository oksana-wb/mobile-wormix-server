package com.pragmatix.app.messages.structures;

import com.pragmatix.common.utils.AppUtils;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.serialization.annotations.Structure;

import java.util.Arrays;
import java.util.Date;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 21.04.2016 11:10
 *         <p>
 * Структура для отправки на клиент, описывающая открытый вклад
 */
@Structure
public class DepositStructure {

    /**
     * в какой валюте депозит
     */
    public MoneyType moneyType;

    /**
     * сколько выплачивается в каждый из дней
     * @see com.pragmatix.app.model.DepositBean#dividendsByDays
     */
    public int[] dividendsByDays;

    /**
     * время открытия (timestamp в секундах)
     */
    public int startDate;

    /**
     * прогресс выдачи процентов: номер последней выплаченной части (нумеруются с 1)
     */
    public int progress;

    /**
     * Какую долю можно забрать сегодня: 0 - если не положено (уже забирал), иначе dividendsByDays[progress]
     */
    public int todayDividend;

    public DepositStructure() {
    }

    public DepositStructure(int moneyType, int[] dividendsByDays, Date startDate, int progress) {
        this.moneyType = MoneyType.valueOf(moneyType);
        this.dividendsByDays = dividendsByDays;
        this.startDate = (int) (startDate.getTime() / 1000L);
        this.progress = progress;
    }

    @Override
    public String toString() {
        return "{" +
                "moneyType=" + moneyType +
                ", dividendsByDays=" + Arrays.toString(dividendsByDays) +
                ", startDate=" + AppUtils.formatDateInSeconds(startDate) +
                ", progress=" + progress +
                ", todayDividend=" + todayDividend +
                '}';
    }
}
