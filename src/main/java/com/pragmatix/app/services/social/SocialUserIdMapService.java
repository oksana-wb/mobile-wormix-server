package com.pragmatix.app.services.social;

import com.pragmatix.app.services.ProfileService;
import com.pragmatix.app.services.persist.ProfilesByStringIdMapKeeper;
import com.pragmatix.gameapp.services.persist.PersistenceService;
import com.pragmatix.gameapp.social.SocialService;
import com.pragmatix.gameapp.social.SocialServiceEnum;
import com.pragmatix.gameapp.social.service.SocialUserIdMap;
import com.pragmatix.server.Server;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SocialUserIdMapService implements SocialUserIdMap<Long> {

    private Logger logger = LoggerFactory.getLogger(ProfileService.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private PersistenceService persistenceService;

    @Resource
    private ProfileService profileService;

    @Resource
    private SocialService socialService;

    /**
     * в этой мапе будем хранить соответствие строковых ID профайлов и "лонговых"
     */
    private Map<String, Long> profilesByStringIdMap = new ConcurrentHashMap<>();
    private Map<SocialServiceEnum, Map<Long, String>> profilesByNetAndLongIdMap = new EnumMap<>(SocialServiceEnum.class);

    public static final String keepFileName = "SocialUserIdMapService.profilesByStringIdMap";

    @Value("${SocialService.mapToAllStringIds:false}")
    private boolean mapToAllStringIds = false;

    private SocialServiceEnum defaultSocialId;
    private Set<SocialServiceEnum> registeredSocialServices = Collections.emptySet();

    /**
     * подгружаем структуры соответствий строковых и лонговых ID
     */
    public void init() {
        defaultSocialId = profileService.getDefaultSocialId();

        for(SocialServiceEnum socialServiceEnum : SocialServiceEnum.values()) {
            profilesByNetAndLongIdMap.put(socialServiceEnum, new ConcurrentHashMap<>());
        }

        Map<String, Pair<Short, Long>> map = persistenceService.restoreObjectFromFile(Map.class, keepFileName, new ProfilesByStringIdMapKeeper());
        if(map != null) {
            for(Map.Entry<String, Pair<Short, Long>> entry : map.entrySet()) {
                Pair<Short, Long> socialNetId_profileId = entry.getValue();
                map(entry.getKey(), socialNetId_profileId.getKey(), socialNetId_profileId.getValue());
            }
            Server.sysLog.info("Restored SocialIdEntities - {}", profilesByStringIdMap.size());
        } else {
            Server.sysLog.info("Selecting SocialIdEntities...");
            final int[] i = {0};
            jdbcTemplate.query("SELECT string_id, profile_id, COALESCE(social_net_id, " + defaultSocialId.getType() + ") as social_net_id FROM wormswar.social_id", new RowCallbackHandler() {
                @Override
                public void processRow(ResultSet res) throws SQLException {
                    map(res.getString("string_id"), res.getShort("social_net_id"), res.getLong("profile_id"));
                    i[0]++;
                    if(i[0] % 100_000 == 0) {
                        Server.sysLog.info("{}...", i[0]);
                    }
                }
            });
            Server.sysLog.info("Loaded SocialIdEntities - {}", profilesByStringIdMap.size());
        }
        persistenceService.rename(keepFileName);

        for(Map.Entry<SocialServiceEnum, Map<Long, String>> entry : profilesByNetAndLongIdMap.entrySet()) {
            int size = entry.getValue().size();
            if(size > 0)
                Server.sysLog.info("{} - {}", entry.getKey(), size);
        }

        registeredSocialServices = socialService.getSocialServicesMap().keySet().stream().map(SocialServiceEnum::valueOf).collect(Collectors.toSet());
    }

    public SocialServiceEnum getMobileMappedPlatform(Long profileId) {
        for(SocialServiceEnum socialServiceEnum : SocialServiceEnum.values()) {
            if(socialServiceEnum.isMobileOS() && profilesByNetAndLongIdMap.get(socialServiceEnum).get(profileId) != null) {
                return socialServiceEnum;
            }
        }
        return SocialServiceEnum.indefinite;
    }

    public String mapToAllStringIds(Long userId) {
        if(userId == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for(SocialServiceEnum socialServiceEnum : SocialServiceEnum.values()) {
            String stringId = profilesByNetAndLongIdMap.get(socialServiceEnum).get(userId);
            if(stringId != null) {
                sb.append('#').append(socialServiceEnum.getType()).append('#').append(stringId);
            }
        }
        return sb.length() > 0 ? sb.substring(1) : sb.toString();
    }

    public String mapToStringId(Long userId, SocialServiceEnum socialNetId) {
        if(userId == null) {
            return null;
        }
        if(socialNetId == null) {
            logger.error("не указана соц. сеть для userId={}", userId);
            return null;
        }
        return profilesByNetAndLongIdMap.get(socialNetId).get(userId);
    }

    @Override
    public String mapToStringId(Object userId) {
        if(mapToAllStringIds) {
            return mapToAllStringIds((Long) userId);
        } else if(registeredSocialServices.size() == 1) {
            return mapToStringId((Long) userId, defaultSocialId);
        } else {
            for(SocialServiceEnum socialServiceEnum : registeredSocialServices) {
                String stringId = mapToStringId((Long) userId, socialServiceEnum);
                if(stringId != null)
                    return stringId;
            }
            return null;
        }
    }

    @Override
    public Long mapToNumberId(Object userId) {
        if(userId == null) {
            return null;
        }

        return profilesByStringIdMap.get((String) userId);
    }

    @Override
    public void map(String stringId, Long longId) {
        //do nothing
    }

    public void map(String stringId, short socialNetId, Long longId) {
        SocialServiceEnum socialServiceEnum = SocialServiceEnum.valueOf(socialNetId);
        profilesByStringIdMap.put(stringId, longId);
        profilesByNetAndLongIdMap.get(socialServiceEnum).put(longId, stringId);
    }

    public void persistToDisk() {
        persistenceService.persistObjectToFile(profilesByNetAndLongIdMap, keepFileName, new ProfilesByStringIdMapKeeper());
    }

    public Map<SocialServiceEnum, Map<Long, String>> getProfilesByNetAndLongIdMap() {
        return profilesByNetAndLongIdMap;
    }

    public Map<String, Long> getProfilesByStringIdMap() {
        return profilesByStringIdMap;
    }

    public boolean isMapToAllStringIds() {
        return mapToAllStringIds;
    }
}
