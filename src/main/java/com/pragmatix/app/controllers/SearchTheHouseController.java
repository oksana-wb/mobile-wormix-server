package com.pragmatix.app.controllers;

import com.pragmatix.app.common.Connection;
import com.pragmatix.app.messages.client.SearchTheHouse;
import com.pragmatix.app.messages.server.SearchTheHouseResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.CheatersCheckerService;
import com.pragmatix.app.services.DailyRegistry;
import com.pragmatix.app.services.SearchTheHouseService;
import com.pragmatix.gameapp.cache.SoftCache;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;

import javax.annotation.Resource;

import static com.pragmatix.app.messages.server.SearchTheHouseResult.ResultEnum;
import static com.pragmatix.app.settings.AppParams.IS_MOBILE;

/**
 * Контроллер обрабатывает команды на обыск домика игрока
 * <p/>
 * User: denis
 * Date: 01.08.2010
 * Time: 20:29:07
 */
@Controller
public class SearchTheHouseController {

    @Resource
    private SoftCache softCache;

    @Resource
    private SearchTheHouseService searchService;

    @Resource
    private CheatersCheckerService cheatersCheckerService;

    @Resource
    private DailyRegistry dailyRegistry;

    @OnMessage(value = SearchTheHouse.class, connections = {Connection.MAIN})
    public SearchTheHouseResult onSearchTheHouse(SearchTheHouse msg, UserProfile profile) {
        if(IS_MOBILE())
            return null;

        //если падлюка читерит, то не обыскиваем домик
        if(cheatersCheckerService.checkSearchHouseDelay(profile)) {
            //|| (cheatersCheckerService.checkFriend(profile, msg.friendId))) {
            return null;
        }
        
        if(msg.friendId == profile.getId()){
            return null;
        }
        
        //устанавливаем новое время обыска домика
        profile.setLastSearchFriendTime(System.currentTimeMillis());

        //загружаем профайл обыскиваемого друга
        final UserProfile friendProfile = softCache.get(UserProfile.class, msg.friendId);
        byte searchKeys = dailyRegistry.getSearchKeys(profile.getId());

        if(friendProfile != null) {
            if(!validateCommand(msg, searchKeys)) {
                return new SearchTheHouseResult(ResultEnum.KEY_LIMIT_EXCEED, 0, searchKeys, msg.friendId);
            } else {
                SearchTheHouseResult searchTheHouseResult = searchService.searchTheHouse(profile, friendProfile);
                searchTheHouseResult.friendId = msg.friendId;
                return searchTheHouseResult;
            }
        } else {
            cheatersCheckerService.searchNonExistentUser(profile);
            return new SearchTheHouseResult(ResultEnum.ERROR, 0, searchKeys, msg.friendId);
        }
    }

    private boolean validateCommand(SearchTheHouse msg,  byte searchKeys) {
        return searchKeys > 0 && msg.keyNum == searchKeys ;
    }

    //====================== Getters and Setters =================================================================================================================================================\

    public void setSearchService(SearchTheHouseService searchService) {
        this.searchService = searchService;
    }

    public void setCheatersCheckerService(CheatersCheckerService cheatersCheckerService) {
        this.cheatersCheckerService = cheatersCheckerService;
    }

}
