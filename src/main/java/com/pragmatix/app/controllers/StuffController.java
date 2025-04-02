package com.pragmatix.app.controllers;

import com.pragmatix.app.common.Connection;
import com.pragmatix.app.messages.client.SelectStuffs;
import com.pragmatix.app.messages.server.SelectStuffResults;
import com.pragmatix.app.messages.structures.SelectStuffResultStructure;
import com.pragmatix.app.messages.structures.SelectStuffStructure;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.StuffService;
import com.pragmatix.gameapp.common.SimpleResultEnum;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;

import javax.annotation.Resource;

/**
 * класс обрабатывает команды для манипулирования предметами
 * (одеть, снять и тд)
 * User: denis
 * Date: 04.10.2010
 * Time: 23:48:50
 */
@Controller
public class StuffController {

    @Resource
    private StuffService stuffService;

    @OnMessage(value = SelectStuffs.class, connections = {Connection.MAIN})
    public SelectStuffResults onSelectStuff(SelectStuffs msg, UserProfile profile) {
        // сначало всё снимаем
        stuffService.deselectAll(profile);

        // теперь одеваем. Клиент должен был прислать состояние всей команды
        SelectStuffResultStructure[] results = new SelectStuffResultStructure[msg.selectStuffs.length];
        for(int i = 0; i < msg.selectStuffs.length; i++) {
            SelectStuffStructure selectStuff = msg.selectStuffs[i];
            int teamMemberId = (int) (selectStuff.memberId == 0 ? profile.getId() : selectStuff.memberId);

            SelectStuffResultStructure result = new SelectStuffResultStructure(selectStuff.memberId);
            results[i] = result;

            if(selectStuff.hatId > 0) {
                result.resultHat = stuffService.selectHat(profile, teamMemberId, selectStuff.hatId);
            }else{
                result.resultHat = SimpleResultEnum.SUCCESS;
            }
            result.hatId = profile.getStructureHat(teamMemberId);

            if(selectStuff.kitId > 0) {
                result.resultKit = stuffService.selectKit(profile, teamMemberId, selectStuff.kitId);
            }else{
                result.resultKit = SimpleResultEnum.SUCCESS;
            }
            result.kitId = profile.getStructureKit(teamMemberId);
        }

        return new SelectStuffResults(results);
    }

}
