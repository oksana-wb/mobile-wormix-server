package com.pragmatix.app.messages.client;

import com.pragmatix.app.common.MoneyType;
import com.pragmatix.serialization.annotations.Command;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 05.03.14 8:47
 * @author Ivan Novikov <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 15.01.2016 15:27
 *
 * Команда смены имени (за деньги)
 *
 * @see com.pragmatix.app.controllers.ShopController#onBuyRename(BuyRename, com.pragmatix.app.model.UserProfile)
 * @see com.pragmatix.app.messages.server.BuyRenameResult
 */
@Command(119)
public class BuyRename {

    /**
     * id члена команды, которого нужно переименовать
     */
    public int teamMemberId;

    public String name;

    public MoneyType moneyType;

    public BuyRename() {
    }

    public BuyRename(int teamMemberId, String name, MoneyType moneyType) {
        this.teamMemberId = teamMemberId;
        this.name = name;
        this.moneyType = moneyType;
    }

    @Override
    public String toString() {
        return "BuyRename{" +
                "teamMemberId=" + teamMemberId +
                ", name='" + name + '\'' +
                ", moneyType=" + moneyType +
                '}';
    }
}
