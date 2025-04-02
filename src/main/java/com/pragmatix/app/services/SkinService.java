package com.pragmatix.app.services;

import com.pragmatix.app.common.ItemType;
import com.pragmatix.app.common.MoneyType;
import com.pragmatix.app.common.Race;
import com.pragmatix.app.messages.structures.WormStructure;
import com.pragmatix.app.model.PurchaseResult;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.settings.ItemRequirements;
import com.pragmatix.craft.domain.Reagent;
import com.pragmatix.quest.QuestService;
import com.pragmatix.quest.dao.QuestEntity;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 23.08.2016 11:54
 */
@Service
public class SkinService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Map<Byte, SkinMeta> skins = Collections.emptyMap();

    @Resource
    private ShopService shopService;

    @Resource
    private QuestService questService;

    @Resource
    private ProfileService profileService;

    public SkinService() {
    }

    public SkinService(ShopService shopService) {
        this.shopService = shopService;
    }

    @Value("#{SkinService_skins}")
    public void setSkinsMeta(Collection<SkinMeta> skins) {
        this.skins = skins.stream().collect(Collectors.toMap(s -> s.id, s -> s));
    }

    public static class SkinMeta extends ItemRequirements {
        public final byte id;
        public final Race targetRace;

        public SkinMeta(int id, Race targetRace) {
            this.id = (byte) id;
            this.targetRace = targetRace;
        }
    }

    public PurchaseResult buySkin(byte skinId, MoneyType moneyType, UserProfile profile) {
        SkinMeta skinMeta = skins.get(skinId);
        if(skinMeta == null) {
            log.error("Ошибка покупки скина [{}]. Скин не зарегистрирован.", skinId);
            return PurchaseResult.ERROR;
        }
        if(!Race.hasRace(profile.getRaces(), skinMeta.targetRace)) {
            log.error("Ошибка покупки скина [{}] для расы [{}]. Отсутствует необходимая раса.", skinId, skinMeta.targetRace);
            return PurchaseResult.MIN_REQUIREMENTS_ERROR;
        }
        if(haveSkin(profile, skinId)) {
            log.error("Ошибка покупки скина [{}]! Скин уже в наличии. skins={}", skinId, Arrays.toString(profile.getSkins()));
            return PurchaseResult.ERROR;
        }
        ItemRequirements itemRequirements = skinMeta;
        // игрок может купить облик за мутаген, стоимость обликов в мутагене при этом переменная: (кол-во ранее открытых обликов за мутаген + 1)*100
        if(moneyType == MoneyType.REAGENTS && questService.isQuestEnabled(3)) {
            int buySkinForReagent = questService.getQuestEntity(profile).q3().buySkinForReagent;
            itemRequirements = skinMeta.cloneSetReagents(Collections.singletonMap(Reagent.mutagen, (buySkinForReagent + 1) * 100));
        }
        PurchaseResult purchaseResult = shopService.tryBuyItemReturnCost(profile, itemRequirements, ItemType.SKIN, moneyType, 1, skinId);
        if(!purchaseResult.isSuccess()) {
            return purchaseResult;
        }

        profile.setSkins(ArrayUtils.add(profile.getSkins(), skinId));
        setActiveSkin(profile, skinMeta.targetRace, skinId);

        if(moneyType == MoneyType.REAGENTS) {
            QuestEntity questEntity = questService.getQuestEntity(profile);
            questEntity.q3().buySkinForReagent++;
            questEntity.dirty = true;
        }
        profileService.updateSync(profile);

        return purchaseResult;
    }

    public boolean addSkin(UserProfile profile, byte skinId, boolean setSkin) {
        SkinMeta skinMeta = skins.get(skinId);
        if(skinMeta == null) {
            log.error("Ошибка добавления скина [{}]. Скин не зарегистрирован.", skinId);
            return false;
        }
        if(!Race.hasRace(profile.getRaces(), skinMeta.targetRace)) {
            log.error("[{}] Ошибка добавления скина [{}] для расы [{}]. Отсутствует необходимая раса.", profile, skinId, skinMeta.targetRace);
            return false;
        }
        if(haveSkin(profile, skinId)) {
            log.debug("[{}] Ошибка добавления скина [{}]! Скин уже в наличии. skins={}", profile, skinId, Arrays.toString(profile.getSkins()));
            return false;
        }

        profile.setSkins(ArrayUtils.add(profile.getSkins(), skinId));
        if(setSkin)
            setActiveSkin(profile, skinMeta.targetRace, skinId);

        return true;
    }

    public boolean haveSkin(UserProfile profile, byte skinId) {
        for(byte skinStruct : profile.getSkins()) {
            if((byte) Math.abs(skinStruct) == skinId)
                return true;
        }
        return false;
    }

    public byte getSkin(UserProfile profile) {
        return getSkin(profile.getSkins(), profile.getRace());
    }

    public byte getSkin(UserProfile profile, byte race) {
        return getSkin(profile.getSkins(), race);
    }

    public byte getSkin(byte[] skins, short race) {
        for(byte skinStruct : skins) {
            boolean isActive = skinStruct < 0;
            byte skinId = (byte) Math.abs(skinStruct);
            SkinMeta skinMeta = this.skins.get(skinId);
            if(skinMeta == null) {
                log.error("Скин с id [{}] не зарегистрирован!", skinId);
                continue;
            }
            if(skinMeta.targetRace.getShortType() == race && isActive)
                return skinId;
        }
        return (byte) 0;
    }

    // если activeSkinId=0 снимаем скин с расы
    public boolean setActiveSkin(UserProfile profile, Race race, byte activeSkinId) {
        Race activeSkinRace = race;
        if(activeSkinId > 0) {
            SkinMeta skinMeta = this.skins.get(activeSkinId);
            if(skinMeta == null) {
                log.error("Ошибка при выборе скина [{}]. Скин не зарегистрирован.", activeSkinId);
                return false;
            }
            activeSkinRace = skinMeta.targetRace;
        }
        byte[] skinsRaw = profile.getSkins();
        for(int i = 0; i < skinsRaw.length; i++) {
            byte skinStruct = skinsRaw[i];
            byte skinId = (byte) Math.abs(skinStruct);
            SkinMeta skinMeta = skins.get(skinId);
            if(skinMeta != null && skinMeta.targetRace == activeSkinRace) {
                if(skinId == activeSkinId) {
                    skinsRaw[i] = (byte) -skinId;
                } else {
                    skinsRaw[i] = skinId;
                }
            }
        }
        WormStructure wormStructure = profile.getWormStructure();
        if(profile.inRace(activeSkinRace) && wormStructure != null) {
            wormStructure.skin = activeSkinId;
        }
        profile.markDirty();
        return true;
    }

    public String mkString(byte[] skinsRaw) {
        Map<Race, Set<String>> skins = new EnumMap<>(Race.class);
        for(byte skinStruct : skinsRaw) {
            boolean isActive = skinStruct < 0;
            byte skinId = (byte) Math.abs(skinStruct);
            SkinMeta skinMeta = this.skins.get(skinId);
            String s = "" + skinId + (isActive ? "*" : "");
            Set<String> set = skins.getOrDefault(skinMeta.targetRace, new TreeSet<>());
            set.add(s);
            skins.put(skinMeta.targetRace, set);
        }
        return skins.toString();
    }

    public Map<Byte, SkinMeta> getSkinsMap() {
        return Collections.unmodifiableMap(skins);
    }
}
