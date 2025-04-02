package com.pragmatix.app.dao;

import com.pragmatix.app.domain.BundleEntity;
import com.pragmatix.app.domain.ReferralLinkEntity;
import com.pragmatix.dao.AbstractDao;
import org.springframework.stereotype.Component;

@Component
public class BundleDao extends AbstractDao<BundleEntity> {

    public BundleDao() {
        super(BundleEntity.class);
    }

}
