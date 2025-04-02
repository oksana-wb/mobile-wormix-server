package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.messages.response.CommonResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Author: Vladimir
 * Date: 06.06.13 9:04
 */
@Service
public class ConcurrentServiceImpl implements ConcurrentService {

    @Value("${ConcurrentService.deadlockTimeout:250}")
    /**
     * время ожидания разблокировок миллисекунд
     * по умолчанию 250 мс
     */
    private int deadlockTimeout = 250;

    @Override
    public <RESPONSE extends CommonResponse> void execRead(ClanTask task, RESPONSE response) {
        synchronized (task.clan) {
            task.exec();
        }
    }

    @Override
    public <RESPONSE extends CommonResponse> void execWrite(ClanTask task, RESPONSE response) {
        synchronized (task.clan) {
            task.exec();
        }
    }
}
