package com.pragmatix.admin.services;

import com.pragmatix.admin.common.RoleType;
import com.pragmatix.admin.model.AdminProfile;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

    public boolean check(AdminProfile profile, Class command) {
        return profile.getRole() == RoleType.SUPER_ADMIN_ROLE;
    }

}
