package com.pragmatix.clanserver.messages.request;

import com.pragmatix.clanserver.messages.Messages;
import com.pragmatix.serialization.annotations.Command;
import com.pragmatix.serialization.annotations.Ignore;

/**
 * Author: Vladimir
 * Date: 23.04.13 16:23
 *
 * @see com.pragmatix.clanserver.services.ClanServiceImpl#updateRating(UpdateRatingRequest)
 */
@Command(Messages.UPDATE_RATING_REQUEST)
public class UpdateRatingRequest extends AbstractRequest {
    /**
     * идентификатор социальной сети
     */
    @Ignore
    public short socialId;

    /**
     * Идентификатор игрока
     */
    public int profileId;

    /**
     * Рейтинг
     */
    public int rating;

    /**
     * Изменение рейтинга
     */
    public int ratingPoints;

    public boolean wipeRating;

    public UpdateRatingRequest() {
    }

    public UpdateRatingRequest(short socialId, int profileId, int rating, int ratingPoints) {
        this.socialId = socialId;
        this.profileId = profileId;
        this.rating = rating;
        this.ratingPoints = ratingPoints;
    }

    @Override
    public int getCommandId() {
        return Messages.UPDATE_RATING_REQUEST;
    }

    @Override
    protected StringBuilder propertiesString() {
        return appendComma(super.propertiesString())
                .append("profileId=").append(profileId)
                .append(", rating=").append(rating)
                .append(", ratingPoints=").append(ratingPoints)
                ;
    }
}
