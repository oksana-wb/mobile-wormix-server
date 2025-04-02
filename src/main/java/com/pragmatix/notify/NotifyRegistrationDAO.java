package com.pragmatix.notify;

import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.persistence.NoResultException;
import javax.validation.constraints.Null;
import java.util.Date;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.08.13 14:05
 */
@Component
public class NotifyRegistrationDAO extends AbstractDao<NotifyRegistrationEntity> {


    @Resource
    private TransactionTemplate transactionTemplate;

    protected NotifyRegistrationDAO() {
        super(NotifyRegistrationEntity.class);
    }

    @Null
    public NotifyRegistrationEntity selectByRegistrationId(String registrationId) {
        try {
            return (NotifyRegistrationEntity) getEm().createQuery("select e from NotifyRegistrationEntity e where e.registrationId = :registrationId")
                    .setParameter("registrationId", registrationId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public boolean delete(final NotifyRegistrationEntity entity) {
        if(entity == null)
            return false;
        return transactionTemplate.execute(transactionStatus -> getEm()
                .createQuery("delete NotifyRegistrationEntity e where e.profileId = :profileId")
                .setParameter("profileId", entity.getProfileId())
                .executeUpdate() > 0);
    }

    public void delete(final Long profileId) {
        transactionTemplate.execute(transactionStatus -> getEm()
                .createQuery("delete NotifyRegistrationEntity e where e.profileId = :profileId")
                .setParameter("profileId", profileId)
                .executeUpdate());
    }

    @Override
    public NotifyRegistrationEntity insert(final NotifyRegistrationEntity entity) {
        return transactionTemplate.execute(transactionStatus -> NotifyRegistrationDAO.super.insert(entity));
    }

    @Override
    public NotifyRegistrationEntity update(final NotifyRegistrationEntity entity) {
        return transactionTemplate.execute(transactionStatus -> NotifyRegistrationDAO.super.update(entity));
    }

}
