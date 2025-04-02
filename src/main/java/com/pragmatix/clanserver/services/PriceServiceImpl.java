package com.pragmatix.clanserver.services;

import com.pragmatix.clanserver.domain.ClanLevel;
import com.pragmatix.clanserver.domain.Price;
import com.pragmatix.clanserver.domain.Product;
import org.springframework.stereotype.Service;

/**
 * Author: Vladimir
 * Date: 12.04.13 15:21
 */
@Service
public class PriceServiceImpl implements PriceService {

    private Price renameClanPrice = new Price(Price.CURRENCY_RUBY, 5, Product.RENAME_CLAN, null);
    private Price changeClanDescriptionPrice = new Price(Price.CURRENCY_RUBY, 5, Product.CHANGE_CLAN_DESCRIPTION, null);
    private Price changeClanEmblemPrice = new Price(Price.CURRENCY_RUBY, 5, Product.CHANGE_CLAN_EMBLEM, null);
    private Price createClanPrice = new Price(Price.CURRENCY_FUSY, 3000, Product.CREATE_CLAN, null);

    /*
1) расширить клан [кол-во мест, цена, тип цены]

Базовая вместимость - 10;

-10, 10, рубинов, (общая вместимость - 20)
-10, 20, рубинов, (общая вместимость - 30)
-10, 30, рубинов, (общая вместимость - 40)
-10, 40, рубинов, (общая вместимость - 50)

2) создать клан
10 000 фузов

3) редактирование информации клана
-изменить название клана, 5 рубинов
-изменить иконку клана, 5 рубинов
*/

    @Override
    public Price createClanPrice(short socialId) {
        return createClanPrice;
    }

    /**
     *
     * @param socialId идентификатор соцсети
     * @param level уровень клана
     * @return цена
     */
    @Override
    public Price expandClanPrice(short socialId, int level) {
        return ClanLevel.get(level).price;
    }

    @Override
    public Price renameClanPrice(short socialId) {
        return renameClanPrice;
    }
    @Override
    public Price changeClanDescriptionPrice(short socialId) {
        return changeClanDescriptionPrice;
    }


    @Override
    public Price changeClanEmblemPrice(short socialId) {
        return changeClanEmblemPrice;
    }

    public void setRenameClanPrice(Price renameClanPrice) {
        this.renameClanPrice = renameClanPrice;
    }

    public void setChangeClanDescriptionPrice(Price changeClanDescriptionPrice) {
        this.changeClanDescriptionPrice = changeClanDescriptionPrice;
    }

    public void setChangeClanEmblemPrice(Price changeClanEmblemPrice) {
        this.changeClanEmblemPrice = changeClanEmblemPrice;
    }

    public void setCreateClanPrice(Price createClanPrice) {
        this.createClanPrice = createClanPrice;
    }
}
