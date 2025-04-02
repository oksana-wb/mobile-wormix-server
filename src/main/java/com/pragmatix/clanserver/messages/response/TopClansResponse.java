package com.pragmatix.clanserver.messages.response;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.clanserver.messages.request.TopClansRequest;
import com.pragmatix.clanserver.messages.structures.ClanTO;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.DateTime;

import java.util.Arrays;
import java.util.Date;

/**
 * Author: Vladimir
 * Date: 21.05.13 16:04
 *
 * @see com.pragmatix.clanserver.services.ClanService#topClans(com.pragmatix.clanserver.messages.request.TopClansRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see com.pragmatix.clanserver.controllers.ClanController#topClans(com.pragmatix.clanserver.messages.request.TopClansRequest, com.pragmatix.clanserver.domain.ClanMember)
 * @see TopClansRequest
 */
@Command(Messages.TOP_CLANS_RESPONSE)
public class TopClansResponse extends CommonResponse<TopClansRequest> {
    /**
     * Топовые кланы
     */
    public ClanTO[] clans;

    /**
     * позиция в ТОП кланов из clans  30 мин. назад, по соответствующим индексам
     * равна 0 если не попал в ТОП кланов (текущий сезонный рейтинг <= 0)
     */
    public Integer[] oldPlaces;

    public int rating;

    /**
     * позиция в ТОП всех кланов 30 мин. назад
     * равна 0 если не попал в ТОП кланов (текущий сезонный рейтинг <= 0)
     */
    public int oldPlace;

    /**
     * Место собственного клана, c 1-цы
     */
    public int position;

    @DateTime
    public Date startSeasonDate;

    @DateTime
    public Date finishSeasonDate;

    public int seasonId;

    public TopClansResponse() {
    }

    public TopClansResponse(TopClansRequest request) {
        super(request);
    }

    @Override
    public String toString() {
        return "TopClansResponse{" +
                "clans=" + Arrays.toString(clans) +
                ", oldPlaces=" + Arrays.toString(oldPlaces) +
                ", rating=" + rating +
                ", oldPlace=" + oldPlace +
                ", position=" + position +
                ", startSeasonDate=" + new java.sql.Date(startSeasonDate.getTime()) +
                ", finishSeasonDate=" + new java.sql.Date(finishSeasonDate.getTime()) +
                ", seasonId=" + seasonId +
                '}';
    }

}
