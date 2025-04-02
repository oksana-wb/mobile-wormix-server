package com.pragmatix.intercom.messages;

import com.pragmatix.app.common.BossBattleResultType;
import com.pragmatix.app.common.PvpBattleResult;
import com.pragmatix.app.messages.structures.BackpackItemStructure;
import com.pragmatix.pvp.BattleWager;
import com.pragmatix.pvp.PvpBattleType;
import com.pragmatix.pvp.model.BattleParticipant;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;
import jskills.Rating;

import java.util.Arrays;

import static com.pragmatix.app.common.PvpBattleResult.NOT_WINNER;
import static com.pragmatix.app.common.PvpBattleResult.WINNER;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 29.05.12 18:16
 * @see com.pragmatix.app.controllers.AppIntercomController#onEndPvpBattleRequest(EndPvpBattleRequest, com.pragmatix.sessions.IAppServer)
 * @see EndPvpBattleResponse
 */
@Command(4003)
public class EndPvpBattleRequest extends IntercomRequest {
    /**
     * тип боя
     */
    public PvpBattleType battleType;

    public long battleId;

    public BattleWager wager;

    public short[] missionIds;

    public long mapId;

    /**
     * результат
     */
    public PvpBattleResult result;

    public BattleParticipant.State participantState;

    public long offlineTime;

    /**
     * массив id реагентов, которые игрок собрал бою. длинной от 0-3
     * id могут повторяться
     */
    public byte[] collectedReagents;

    /**
     * массив потраченого оружия в уровне
     */
    public BackpackItemStructure[] items;

    /**
     * ожидается ли ответ на этот запрос
     */
    public boolean needResponse = true;

    // обновленное значение True Skill рейтинга

    /**
     * The statistical mean value of the rating (also known as μ). *
     */
    public double newTrueSkillMean;

    /**
     * The standard deviation (the spread) of the rating. This is also known as σ. *
     */
    public double newTrueSkillStandardDeviation;

    // начисленный (потерянный) рейтинг
    public int ratingPoints;

    // начисленные (потерянные) очки сезрнного рейтинга
    public int rankPoints;

    /**
     * во время боя была обнаружена попытка взлома
     */
    public short banType;

    /**
     * доп. информация по бану
     */
    public String banNote;

    public short questId;

    public int droppedUnits;

    public short healthInPercent;

    public int teamSize;

    @Ignore
    public BattleAward battleAward = new BattleAward();

    public static class BattleAward {
        public int money;
        public int extraMoney;
        public int wagerWinAwardToken;
        public int bossWinAwardToken;
        public BossBattleResultType bossBattleResultType;
        public int realMoney;
        public int experience;
        public int boostFactor;
        public int healthInPercent;
        public byte[] collectedReagents;
        public short rareItem;

        public String bossBattleWinTypeName(){
            return bossBattleResultType != null ? bossBattleResultType.name() : "";
        }
    }

    public EndPvpBattleRequest() {
    }

    public EndPvpBattleRequest(BattleParticipant battleParticipant) {
        this.requestId = REQUEST_SEQ.incrementAndGet();

        this.profileId = battleParticipant.getProfileId();
        this.socialNetId = battleParticipant.getSocialNetId();
    }

    @Override
    public String toString() {
        return "EndPvpBattleRequest{" +
                "user=" + socialNetId + ":" + profileId +
                ", requestId=" + requestId +
                ", battleId=" + battleId +
                ", battleType=" + battleType +
                ", result=" + result +
                ", ratingPoints=" + ratingPoints +
                ", rankPoints=" + rankPoints +
                ", wager=" + wager +
                (missionIds != null && missionIds.length > 0 ? ", missionIds=" + Arrays.toString(missionIds) : "") +
                ", mapId=" + mapId +
                (collectedReagents.length > 0 ? ", collectedReagents=" + Arrays.toString(collectedReagents) : "") +
                (items.length > 0 ? ", items=" + Arrays.toString(items) : "") +
                ", droppedUnits=" + droppedUnits +
                ", healthInPercent=" + healthInPercent +
                ", teamSize=" + teamSize +
                ", needResponse=" + needResponse +
                ", rating=" + new Rating(newTrueSkillMean, newTrueSkillStandardDeviation) +
                ", banType=" + banType +
                ", banNote='" + banNote + '\'' +
                '}';
    }

    public short getMissionId() {
        return missionIds.length == 1 ? missionIds[0] : 0;
    }
}
