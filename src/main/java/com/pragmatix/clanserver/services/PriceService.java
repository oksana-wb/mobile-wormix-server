package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.domain.Price;

/**
 * Author: Vladimir
 * Date: 12.04.13 15:13
 */
public interface PriceService {
    Price createClanPrice(short socialId);

    Price expandClanPrice(short socialId, int capacity);

    Price renameClanPrice(short socialId);

    Price changeClanEmblemPrice(short socialId);

    Price changeClanDescriptionPrice(short socialId);
}
