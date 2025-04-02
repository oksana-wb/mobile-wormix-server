package com.pragmatix.app.services;

import com.pragmatix.app.common.ItemCheck;
import com.pragmatix.app.common.ItemType;
import com.pragmatix.app.init.WeaponsCreator;
import com.pragmatix.app.messages.client.ExecuteSpecialDeal;
import com.pragmatix.app.messages.client.GetSpecialDeal;
import com.pragmatix.app.messages.server.ExecuteSpecialDealResponse;
import com.pragmatix.app.messages.server.GetSpecialDealResponse;
import com.pragmatix.app.model.BackpackItem;
import com.pragmatix.app.model.Stuff;
import com.pragmatix.app.model.UserProfile;
import com.pragmatix.app.model.Weapon;
import com.pragmatix.app.services.ProfileEventsService.Param;
import com.pragmatix.app.common.ShopResultEnum;
import com.pragmatix.gameapp.controller.annotations.Controller;
import com.pragmatix.gameapp.controller.annotations.OnMessage;
import com.pragmatix.gameapp.sessions.Sessions;
import com.pragmatix.wormix.webadmin.interop.response.structure.MoneyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.12.2014 15:32
 */
@Controller
public class SpecialDealService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private int param_1 = 14;
    private int param_2 = 10;
    private double param_4 = 0.5;
    private double param_3 = 0.3;

    @Resource
    private DailyRegistry dailyRegistry;

    @Resource
    private PaymentService paymentService;

    @Resource
    private WeaponService weaponService;

    @Resource
    private WeaponsCreator weaponsCreator;

    @Resource
    private StuffService stuffService;

    @Resource
    private StatisticService statisticService;

    @Resource
    private ProfileEventsService profileEventsService;

    @Value("#{specialDealWeapons}")
    private Set<Integer> specialDealWeapons = new HashSet<>();

    @Value("#{specialDealStuff}")
    private Set<Integer> specialDealStuff = new HashSet<>();

    @Value("${debug.specialDealValidate:true}")
    private boolean debugSpecialDealValidate = true;

    @PostConstruct
    public void init(){
        specialDealWeapons.forEach(weaponId -> {
            Weapon weapon = weaponService.getWeapon(weaponId);
            if(!weapon.isType(Weapon.WeaponType.INFINITE))
                throw new IllegalStateException("special deal weapon [" + weapon + "] должно быть бесконечным!");
        });
        specialDealStuff.forEach(stuffId -> {
            Stuff stuff = stuffService.getStuff(stuffId);
            if(stuff.isTemporal())
                throw new IllegalStateException("special deal item [" + stuff + "] должен быть постоянным!");
            if(!((stuffId >= 1000 && stuffId < 1500) || (stuffId >= 2000 && stuffId < 2500)))
                throw new IllegalStateException("special deal item [" + stuff + "] имеет не корректный ID!");
            if(!(ItemCheck.hasRealPrice(stuff) && stuff.getRealprice() > 30 ))
                throw new IllegalStateException("special deal item [" + stuff + "] имеет не корректную рубиновую цену!");
        });
    }

    public GetSpecialDealResponse getSpecialDeal(UserProfile profile) {
        GetSpecialDealResponse result = new GetSpecialDealResponse();
        if(specialDealWeapons.isEmpty() && specialDealStuff.isEmpty()) {
            if(log.isDebugEnabled()) log.debug("specialDealItems are both empty");
            return result;
        }
        if(dailyRegistry.isMakePeyment(profile.getId())) {
            if(log.isDebugEnabled()) log.debug("user made payment today");
            if(debugSpecialDealValidate) return result;
        }
        if(paymentService.isDonat(profile.getId())) {
            if(log.isDebugEnabled()) log.debug("user is donat");
            if(debugSpecialDealValidate) return result;
        }
        if(dailyRegistry.isReceivedSpecialDeal(profile.getId())) {
            if(log.isDebugEnabled()) log.debug("user received special deal today");
            if(debugSpecialDealValidate) return result;
        }

        int x = profile.getRealMoney() + param_1 + new Random().nextInt(param_2);
        List<Integer> items = new ArrayList<>();
        for(Integer specialDealWeaponId : specialDealWeapons) {
            float realprice = 0;
            Weapon weapon = weaponsCreator.getWeapon(specialDealWeaponId);
            if(weapon.isType(Weapon.WeaponType.INFINITE)) {
                if(weaponService.isPresentInfinitely(profile, specialDealWeaponId))
                    continue;
                realprice = weapon.getRealprice();
            } else if(weapon.isType(Weapon.WeaponType.COMPLEX)){
                BackpackItem backpackItem = profile.getBackpackItemByWeaponId(specialDealWeaponId);
                if(backpackItem != null && backpackItem.getCount() < 0)
                    continue;
                realprice = weapon.getRealprice() * weapon.getMaxWeaponLevel();
            }
            if (realprice > 0 && x >= Math.round(realprice * param_3) && x <= Math.round(realprice * param_4))
                items.add(specialDealWeaponId);
        }

        for(Integer specialDealStuffId : specialDealStuff) {
            Stuff stuff = stuffService.getStuff(specialDealStuffId);
            if(stuffService.isExist(profile, specialDealStuffId.shortValue(), false))
                continue;
            float realprice = stuff.getRealprice();
            if (realprice > 0 && x >= Math.round(realprice * param_3) && x <= Math.round(realprice * param_4))
                items.add(specialDealStuffId);
        }

        if(items.isEmpty()) {
            return result;
        }

        result.rubyPrice = (byte) x;
        result.itemId = items.get(new Random().nextInt(items.size())).shortValue();

        profile.specialDealItemId = result.itemId;
        profile.specialDealRubyPrice = result.rubyPrice;

        dailyRegistry.receivedSpecialDeal(profile.getProfileId());

        return result;
    }

    public ShopResultEnum executeSpecialDeal(UserProfile profile, short itemId, byte rubyPrice) {
        ShopResultEnum result = ShopResultEnum.ERROR;

        if(profile.specialDealItemId != itemId) {
            log.error("profile.specialDealItemId [{}] != msg.itemId [{}]", profile.specialDealItemId, itemId);
            return result;
        }
        if(profile.specialDealRubyPrice != rubyPrice) {
            log.error("profile.specialDealRubyPrice [{}] != msg.rubyPrice [{}]", profile.specialDealRubyPrice, rubyPrice);
            return result;
        }
        if(profile.getRealMoney() < rubyPrice) {
            log.error("can't buy item: {} with price {}! Not enough realmoney:{}", itemId, rubyPrice, profile.getRealMoney());
            return ShopResultEnum.NOT_ENOUGH_MONEY;
        }
        if(!dailyRegistry.isMakePeyment(profile.getId())) {
            log.error("absent payment today! ruby balance: {}, itemId: {}, rubyPrice: {}", profile.getRealMoney(), itemId, rubyPrice);
            return result;
        }

        boolean addResult = ItemCheck.isWeapon(itemId)
                ? weaponService.addOrUpdateWeapon(profile, itemId, weaponService.getWeapon(itemId).getInfiniteCount())
                : stuffService.addStuff(profile, itemId);

        if(addResult) {
            // логируем покупку предмета по спец цене
            profileEventsService.fireProfileEventAsync(ProfileEventsService.ProfileEventEnum.PURCHASE, profile,
                    Param.eventType, ItemType.SPECIAL_DEAL_ITEM,
                    Param.itemId, itemId,
                    Param.realMoney, -rubyPrice
            );
            statisticService.buyItemStatistic(profile.getId(), MoneyType.REAL_MONEY.type, rubyPrice, ItemType.SPECIAL_DEAL_ITEM, 1, itemId, profile.getLevel());
            // снимаем деньги
            profile.setRealMoney(profile.getRealMoney() - rubyPrice);

            result = ShopResultEnum.SUCCESS;
        }

        return result;
    }

    @OnMessage
    public GetSpecialDealResponse onGetSpecialDeal(final GetSpecialDeal msg, final UserProfile profile) {
        return getSpecialDeal(profile);
    }

    @OnMessage
    public ExecuteSpecialDealResponse onExecuteSpecialDeal(final ExecuteSpecialDeal msg, final UserProfile profile) {
        return new ExecuteSpecialDealResponse(executeSpecialDeal(profile, msg.itemId, msg.rubyPrice), msg.itemId, msg.rubyPrice, Sessions.getKey());
    }

    public Set<Integer> getSpecialDealItems() {
        HashSet<Integer> result = new HashSet<>(specialDealWeapons);
        result.addAll(specialDealStuff);
        return result;
    }
}
