package com.pragmatix.clanserver.controllers;

import com.pragmatix.app.model.UserProfile;
import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.services.ClanServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;

/**
 * Author: Vladimir
 * Date: 12.04.13 16:41
 */
public class AbstractController {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected ClanServiceImpl clanService;

    protected final Random random = new Random();

    protected ClanMember getClanMember(UserProfile profile) {
        return clanService.getClanMember(profile.getId().intValue());
    }

}
