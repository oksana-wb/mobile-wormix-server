package com.pragmatix.app.controllers;

import com.pragmatix.app.common.Connection;
import com.pragmatix.app.messages.client.AddToGroup;
import com.pragmatix.app.messages.client.RemoveFromGroup;
import com.pragmatix.app.messages.client.ReorderGroup;
import com.pragmatix.app.messages.client.ToggleTeamMember;
import com.pragmatix.app.messages.server.AddToGroupResult;
import com.pragmatix.app.messages.server.RemoveFromGroupResult;
import com.pragmatix.app.messages.server.ReorderGroupResult;
import com.pragmatix.app.messages.server.ToggleTeamMemberResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.services.GroupService;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * контроллер обрабатывает команд на добавление и удаление червя из группы
 *
 * @author denis
 *         Date: 03.01.2010
 *         Time: 23:09:13
 */
@Controller
public class GroupController {

    @Resource
    private GroupService groupService;

    /**
     * обработчик команды на добавления червя в группу
     *
     * @param msg     команда
     * @param profile профайл который попросил добавить червя
     * @return AddToGroupResult
     * @throws Exception вслучаи ошибки
     */
    @OnMessage(value = AddToGroup.class, connections = {Connection.MAIN})
    public AddToGroupResult onAddToGroup(AddToGroup msg, UserProfile profile) throws Exception {
        return new AddToGroupResult(groupService.addToGroup(msg, profile));
    }

    /**
     * обработчик команды на удаления червя из группы
     *
     * @param msg     команда
     * @param profile профайл который попросил удалить червя
     * @return RemoveFromGroupResult
     * @throws Exception вслучаи ошибки
     */
    @OnMessage(value = RemoveFromGroup.class, connections = {Connection.MAIN})
    public RemoveFromGroupResult onRemoveFromGroup(RemoveFromGroup msg, UserProfile profile) throws Exception {
        return new RemoveFromGroupResult(groupService.removeFromGroup(profile, msg.teamMemberId));
    }

    /**
     * обработчик команды изменения очередности ходов членов команды
     *
     * @param msg     команда
     * @param profile профайл который попросил удалить червя
     * @return RemoveFromGroupResult
     * @throws Exception вслучаи ошибки
     */
    @Transactional
    @OnMessage(value = ReorderGroup.class, connections = {Connection.MAIN})
    public ReorderGroupResult onReorderGroup(ReorderGroup msg, UserProfile profile) throws Exception {
        return new ReorderGroupResult(groupService.reorderGroup(msg, profile));
    }

    @OnMessage(value = ToggleTeamMember.class, connections = {Connection.MAIN})
    public ToggleTeamMemberResult onToggleTeamMember(ToggleTeamMember msg, UserProfile profile) throws Exception {
        return new ToggleTeamMemberResult(groupService.toggleTeamMember(msg, profile), msg.teamMemberId);
    }

}
