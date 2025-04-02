package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.domain.Clan;

/**
 * Author: Vladimir
 * Date: 22.04.13 9:11
 */
public abstract class ClanTask {
    protected final Clan clan;

    protected ClanTask(Clan clan) {
        this.clan = clan;
    }

    protected abstract void exec();
}
