package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.messages.response.CommonResponse;

/**
 * Author: Vladimir
 * Date: 06.06.13 9:00
 */
public interface ConcurrentService {
    <RESPONSE extends CommonResponse> void execRead(ClanTask task, RESPONSE response);

    <RESPONSE extends CommonResponse> void execWrite(ClanTask task, RESPONSE response);
}
