package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.domain.ClanMember;
import com.pragmatix.clanserver.domain.Invite;
import com.pragmatix.clanserver.messages.ServiceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author: Vladimir
 * Date: 26.04.13 11:19
 */
@Service
public class InviteRepoImpl implements InviteRepo {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ConcurrentHashMap<Object, InvitesRef> repo = new ConcurrentHashMap<>();

    /**
     * Количество приглашений отправленное одному человеку
     */
    @Value("${clan.invite.repo.limit:20}")
    int inviteLimit;

    /**
     * Максимальное время жизни инвайта, секунд
     */
    @Value("${clan.invite.maxLifeTime:600}")
    int inviteMaxLifeTime;

    /**
     * Минимальное время жизни инвайта, секунд
     */
    @Value("${clan.invite.minLifeTime:600}")
    int inviteMinLifeTime;

    /**
     * Период очистки устаревших инвайтов, секунд
     */
    @Value("${clan.invite.trimInterval:600}")
    int trimInterval;

    /**
     * Таймаут миллисекунд
     */
    @Value("${clan.invite.repo.timeout:1}")
    int timeout;

    final Trimmer trimmer = new Trimmer();

    public void setInviteLimit(int inviteLimit) {
        this.inviteLimit = inviteLimit;
    }

    public void setInviteMaxLifeTime(int inviteMaxLifeTime) {
        this.inviteMaxLifeTime = inviteMaxLifeTime;
    }

    public void setInviteMinLifeTime(int inviteMinLifeTime) {
        this.inviteMinLifeTime = inviteMinLifeTime;
    }

    public void setTrimInterval(int trimInterval) {
        this.trimInterval = trimInterval;
    }

    @Override
    public ServiceResult addInvite(short hostSocialId, int hostProfileId, int clanId, short socialId, int profileId) {
        return addInvite(ClanMember.getId(hostSocialId, hostProfileId), clanId, socialId, profileId);
    }

    @Override
    public ServiceResult addInvite(Object key, int clanId, short socialId, int profileId) {
        boolean success = false;

        long timeout = System.nanoTime() + 1000000L * this.timeout;

        while (System.nanoTime() < timeout && !success) {
            InvitesRef ref = repo.get(key);

            if (ref == null) {
                ref = new InvitesRef(new Invite[] {new Invite(clanId, (short)0, profileId)});
                success = repo.putIfAbsent(key, ref) == null;
            } else {
                synchronized (ref) {
                    int length = ref.invites.length;
                    int offset = 0;
                    long now = System.currentTimeMillis();
                    long timeA = now - 1000L * inviteMaxLifeTime;
                    long timeB = now - 1000L * inviteMinLifeTime;

                    for (Invite inv: ref.invites) {
                        long inviteTime = inv.inviteDate.getTime();

                        if (inviteTime < timeA) {
                            offset++;
                        } else if (length - offset < inviteLimit) {
                            break;
                        } else if (inviteTime < timeB) {
                            offset++;
                        } else {
                            return ServiceResult.ERR_PROFILE_INVITE_LIMIT;
                        }
                    }

                    Invite[] invites = new Invite[length - offset + 1];
                    int i = 0;
                    while (offset < length) {
                        invites[i++] = ref.invites[offset++];
                    }
                    invites[i] = new Invite(clanId, (short)0, profileId);

                    ref.invites = invites;

                    success = ref.equals(repo.get(key));
                }
            }
        }

        trimmer.test();

        return success ? ServiceResult.OK : ServiceResult.ERR_DEADLOCK;
    }

    @Override
    public Invite[] getInvites(short hostSocialId, int hostProfileId) {
        return getInvites(ClanMember.getId(hostSocialId, hostProfileId));
    }

    @Override
    public Invite[] getInvites(Object key) {
        InvitesRef ref = repo.get(key);

        return ref != null ? ref.invites : null;
    }

    @Override
    public Invite[] removeInvites(short hostSocialId, int hostProfileId) {
        return removeInvites(ClanMember.getId(hostSocialId, hostProfileId));
    }

    @Override
    public Invite[] removeInvites(Object key) {
        InvitesRef ref = repo.remove(key);

        return ref != null ? ref.invites : null;
    }

    private class Trimmer implements Runnable {
        final AtomicBoolean busy = new AtomicBoolean();

        volatile long trimDate = 0;

        void test() {
            if (System.currentTimeMillis() - trimDate > trimInterval * 1000 && busy.compareAndSet(false, true)) {
                new Thread(this).start();
            }
        }

        @Override
        public void run() {
            try {
                long now = System.currentTimeMillis();
                long timeA = now - 1000L * inviteMaxLifeTime;

                for (Iterator<Map.Entry<Object,InvitesRef>> itr = repo.entrySet().iterator(); itr.hasNext();) {
                    Map.Entry<Object,InvitesRef> entry = itr.next();
                    InvitesRef ref = entry.getValue();
                    synchronized (ref) {
                        int offset = 0;

                        for (Invite inv: ref.invites) {
                            long inviteTime = inv.inviteDate.getTime();

                            if (inviteTime < timeA) {
                                offset++;
                            } else {
                                break;
                            }
                        }

                        int length = ref.invites.length;

                        if (length == 0 || offset >= length) {
                            itr.remove();
                        } else {
                            ref.invites = Arrays.copyOfRange(ref.invites, offset, ref.invites.length);
                        }
                    }
/*
                    ConcurrentUtils.writeLock(ref.lock);
                    try {
                        int offset = 0;

                        for (Invite inv: ref.invites) {
                            long inviteTime = inv.inviteDate.getTime();

                            if (inviteTime < timeA) {
                                offset++;
                            } else {
                                break;
                            }
                        }

                        int length = ref.invites.length;

                        if (length == 0 || offset >= length) {
                            itr.remove();
                        } else {
                            ref.invites = Arrays.copyOfRange(ref.invites, offset, ref.invites.length);
                        }
                    } finally {
                        ConcurrentUtils.writeUnlock(ref.lock);
                    }
*/
                }

            } finally {
                trimDate = System.currentTimeMillis();

                busy.set(false);
            }
        }
    }

    private static class InvitesRef {
/*
        final AtomicInteger lock = new AtomicInteger();
*/
        volatile Invite[] invites;

        private InvitesRef(Invite[] invites) {
            this.invites = invites;
        }
    }
}
