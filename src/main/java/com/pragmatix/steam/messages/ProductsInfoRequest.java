package com.pragmatix.steam.messages;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.serialization.annotations.Command;

/**
 * Команда запроса страны/валюты игрока для совершения покупок за реальные деньги
 *
 * @see com.pragmatix.steam.SteamController#onPurchaseInfoRequest(ProductsInfoRequest, UserProfile)
 */
@Command(6005)
public class ProductsInfoRequest {

}
