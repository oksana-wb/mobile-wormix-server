package com.pragmatix.clanserver.domain;

/**
 * Author: Vladimir
 * Date: 10.04.13 17:52
 */
class Permissions {
    static final int ALL = 0xFFFFFFFF;
    static final int INVITE = 0x00000001;
    static final int EXPEL = 0x00000002;
    static final int PROMOTE_IN_RANK = 0x00000004;
    static final int LOWER_IN_RANK = 0x00000008;
    static final int EXPAND = 0x00000010;
    static final int POST_NEWS = 0x00000020;
    static final int EDIT = 0x00000040;
}
