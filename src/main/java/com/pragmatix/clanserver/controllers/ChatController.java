package com.pragmatix.clanserver.controllers;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.messages.request.PostNewsRequest;
import com.pragmatix.clanserver.messages.request.PostToChatRequest;
import com.pragmatix.clanserver.messages.response.PostNewsResponse;
import com.pragmatix.clanserver.messages.response.PostToChatResponse;
import com.pragmatix.clanserver.services.ChatServiceImpl;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;

import javax.annotation.Resource;

/**
 * Author: Vladimir
 * Date: 15.04.13 13:27
 */
@Controller
public class ChatController extends AbstractController {

    @Resource
    private ChatServiceImpl chatService;

    @OnMessage
    public PostToChatResponse postToChat(PostToChatRequest request, UserProfile profile) {
        return chatService.postToChat(request, getClanMember(profile));
    }

    @OnMessage
    public PostNewsResponse postNews(PostNewsRequest request, UserProfile profile) {
        return chatService.postNews(request, getClanMember(profile));
    }

}
