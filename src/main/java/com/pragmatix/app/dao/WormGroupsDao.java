package com.pragmatix.app.dao;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.pragmatix.app.domain.WormGroupsEntity;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.group.TeamMember;
import com.pragmatix.app.services.ProfileService;
import com.pragmatix.dao.AbstractDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.validation.constraints.Null;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Dao класс для сохронения и загрузки WormGroupsEntity
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 15.08.12 18:11
 */
@Component
public class WormGroupsDao extends AbstractDao<WormGroupsEntity> {

    private static final Logger log = LoggerFactory.getLogger(WormGroupsDao.class);

    @Resource
    private ProfileService profileService;

    private static final Type teamMemberNamesType = new TypeToken<Map<Integer, String>>(){}.getType();

    private static final Gson gson = new Gson();

    public WormGroupsDao() {
        super(WormGroupsEntity.class);
    }

    @SuppressWarnings("unchecked")
    public WormGroupsEntity getWormGroupsByProfileId(Long profileId) {
        WormGroupsEntity entity = getEm().find(WormGroupsEntity.class, profileId.intValue());
        preProcessLoadedEntity(entity);
        return entity;
    }

    /**
     * Запрашивает состав комманды для игроков одним запросом
     *
     * @param idSet                  список id игроков
     * @param preloadComradeProfiles подгружать ли в кеш профили членов комманд
     * @return состав комманды игроков разложенное по игрокам
     */
    @SuppressWarnings({"unchecked"})
    public Map<Long, WormGroupsEntity> getWormGroupsByProfileIds(Set<Long> idSet, boolean preloadComradeProfiles) {
        List<WormGroupsEntity> result = (List<WormGroupsEntity>) getEm().createNamedQuery("getWormGroupsByProfileIds")
                .setParameter("profileIds", idSet)
                .getResultList();

        Set<Long> comradeIds = new HashSet<Long>();
        Map<Long, WormGroupsEntity> resultMap = new HashMap<Long, WormGroupsEntity>();
        for(WormGroupsEntity entity : result) {
            preProcessLoadedEntity(entity);
            resultMap.put((long) entity.getProfileId(), entity);

            for(int comradeId : entity.getTeamMembers()) {
                if(comradeId > 0)// исключаем наемников
                    comradeIds.add((long) comradeId);
            }
        }

        //подгружаем профайлы членов комманд если выставлен параметр preloadComradeProfiles
        if(preloadComradeProfiles && comradeIds.size() > 0) {
            // false чтобы не грузить членов комманд для членов комманд
            profileService.loadProfiles(comradeIds, false);
        }
        return resultMap;
    }

    // конвертирует имена членов команды из строкового поля в мапу после загрузки
    private void preProcessLoadedEntity(WormGroupsEntity entity) {
        if (entity != null) {
            entity.setTeamMemberNamesMap(teamMemberNamesFromVarchar(entity.getTeamMemberNames()));
        }
    }

    private void postProcessSavingEntity(WormGroupsEntity entity) {
        if (entity != null) {
            entity.setTeamMemberNames(teamMemberNamesToVarchar(entity.getTeamMemberNamesMap()));
        }
    }

    public boolean insertWormGroups(UserProfile profile) {
        int profileId = profile.getId().intValue();
        WormGroupsEntity entity = new WormGroupsEntity();
        entity.setProfileId(profileId);
        entity.setTeamMember1(profileId);
        entity.setTeamMember2(profile.getWormsGroup()[1]);
        entity.setTeamMemberMeta2(toBytea(profile.getTeamMembers(), 1));
        // NB: teamMemberNames на данный момент пуст: пользователь ещё не мог пока успеть никого переименовать

        insert(entity);
        return true;
    }

    public boolean updateWormGroups(UserProfile profile) {
        int[] wormsGroup = profile.getWormsGroup();
        TeamMember[] teamMembers = profile.getTeamMembers();
        int count = getEm().createNamedQuery("updateWormGroups").
                setParameter("profileId", profile.getId().intValue()).
                setParameter("teamMember1", getValueByIndex(wormsGroup, 0)).
                setParameter("teamMember2", getValueByIndex(wormsGroup, 1)).
                setParameter("teamMember3", getValueByIndex(wormsGroup, 2)).
                setParameter("teamMember4", getValueByIndex(wormsGroup, 3)).
                setParameter("teamMember5", getValueByIndex(wormsGroup, 4)).
                setParameter("teamMember6", getValueByIndex(wormsGroup, 5)).
                setParameter("teamMember7", getValueByIndex(wormsGroup, 6)).

                setParameter("teamMemberMeta1", toBytea(teamMembers, 0)).
                setParameter("teamMemberMeta2", toBytea(teamMembers, 1)).
                setParameter("teamMemberMeta3", toBytea(teamMembers, 2)).
                setParameter("teamMemberMeta4", toBytea(teamMembers, 3)).
                setParameter("teamMemberMeta5", toBytea(teamMembers, 4)).
                setParameter("teamMemberMeta6", toBytea(teamMembers, 5)).
                setParameter("teamMemberMeta7", toBytea(teamMembers, 6)).

                setParameter("teamMemberNames", teamMemberNamesToVarchar(getNames(wormsGroup, teamMembers))).
                setParameter("extraGroupSlotsCount", (short) profile.getExtraGroupSlotsCount()).

                executeUpdate();
        profile.setTeamMembersDirty(false);
        return count > 0;
    }

    @Null
    private Integer getValueByIndex(int[] wormsGroup, int i) {
        return i < wormsGroup.length ? wormsGroup[i] : null;
    }

    @Null
    public static Map<Integer, String> getNames(int[] wormsGroup, TeamMember[] teamMembers) {
        if (teamMembers == null) {
            return null;
        }
        Map<Integer, String> res = new HashMap<>(teamMembers.length);
        for (int i = 0; i < teamMembers.length; i++) {
            if (teamMembers[i] != null && teamMembers[i].canBeRenamed()) { // null => свой червь, на которого не заводится TeamMember
                res.put(wormsGroup[i], teamMembers[i].getName());
            }
        }
        return res;
    }

    @Null
    public static byte[] toBytea(TeamMember[] teamMembers, int i) {
        TeamMember teamMember = i < teamMembers.length ? teamMembers[i] : null;
        return teamMember != null ? teamMember.toBytea() : null;
    }

    /**
     * удалить всех червей из группы
     *
     * @param profileId профайла игрока
     * @return true если удалось
     */
    public boolean deleteWormGroups(Long profileId) {
        int count = getEm().createNamedQuery("deleteGroup").
                setParameter("profileId", profileId.intValue()).
                executeUpdate();
        return count > 0;
    }

    @Null
    public static String teamMemberNamesToVarchar(@Null Map<Integer, String> teamMemberNames) {
        if (teamMemberNames == null) {
            return null;
        }
        return gson.toJson(teamMemberNames);
    }

    @Null
    public static Map<Integer, String> teamMemberNamesFromVarchar(@Null String teamMemberNames) {
        try {
            return gson.fromJson(teamMemberNames, teamMemberNamesType);
        } catch (JsonSyntaxException e) { // если в базе что-то не то (например, старое)
            log.warn("Failed to decode team member names from \""+teamMemberNames+"\"", e);
            return null;
        }
    }
}
