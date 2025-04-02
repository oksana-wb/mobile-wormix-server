package com.pragmatix.app.dao;

import com.pragmatix.app.domain.DepositEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

import javax.persistence.NoResultException;
import java.util.List;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 19.04.2016 10:26
 */
@Component
public class DepositDao extends AbstractDao<DepositEntity> {

    public DepositDao() {
        super(DepositEntity.class);
    }

    /**
     * Возвращает все {@link DepositEntity}, принадлежащие данному игроку,
     * которые ещё не были погашены ({@link DepositEntity#paidOff} == false)
     *
     * @param profileId id профиля игрока
     * @return массив из принадлежащих игроку открытых депозитов (в частности, пустой массив если таких нет)
     */
    public DepositEntity[] getAllOpenForProfile(int profileId) {
        try {
            @SuppressWarnings("unchecked")
            List<DepositEntity> resultList = (List<DepositEntity>) getEm().createNamedQuery("DepositEntity.selectByProfile").setParameter("profileId", profileId).getResultList();
            return resultList.toArray(new DepositEntity[resultList.size()]);
        } catch (NoResultException e) {
            return new DepositEntity[0];
        }
    }

    /**
     * Возвращает _все_ {@link DepositEntity}, принадлежащие данному игроку,
     * в том числе те, которые уже погашены
     *
     * @param profileId id профиля игрока
     * @return массив из принадлежащих игроку депозитов (в частности, пустой массив если таких нет)
     */
    public DepositEntity[] getAllForProfile(int profileId) {
        try {
            @SuppressWarnings("unchecked")
            List<DepositEntity> resultList = (List<DepositEntity>) getEm().createNamedQuery("DepositEntity.selectAllByProfile").setParameter("profileId", profileId).getResultList();
            return resultList.toArray(new DepositEntity[resultList.size()]);
        } catch (NoResultException e) {
            return new DepositEntity[0];
        }
    }

    /**
     * Обновляет в базе прогресс и статус {@link DepositEntity#paidOff} для данного депозита
     *
     * @param deposit уже существующая в базе запись о депозите
     * @return true если обновление прошло успешно, иначе false
     */
    public boolean updateProgress(DepositEntity deposit) {
        int count = getEm().createNamedQuery("DepositEntity.updateProgress")
                           .setParameter("id", deposit.getId())
                           .setParameter("progress", deposit.getProgress())
                           .setParameter("lastPayDate", deposit.getLastPayDate())
                           .setParameter("paidOff", deposit.isPaidOff())
                           .executeUpdate();
        return count == 1;
    }
}
