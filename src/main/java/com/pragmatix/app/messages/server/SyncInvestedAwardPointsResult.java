package com.pragmatix.app.messages.server;

import com.pragmatix.serialization.annotations.Command;

/**
 * Команда вернён пересчитанное значение потраченных призовых очков достижений
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 31.01.12 16:31
 */
@Command(10041)
public class SyncInvestedAwardPointsResult {

    public byte investedAwardPoints;

    public SyncInvestedAwardPointsResult() {
    }

    public SyncInvestedAwardPointsResult(byte investedAwardPoints) {
        this.investedAwardPoints = investedAwardPoints;
    }

    @Override
    public String toString() {
        return "SyncInvestedAwardPointsResult{" +
                "investedAwardPoints=" + investedAwardPoints +
                '}';
    }
}
