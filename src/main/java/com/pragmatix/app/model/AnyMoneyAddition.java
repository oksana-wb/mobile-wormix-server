package com.pragmatix.app.model;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 21.04.2016 10:08
 *         <p>
 * Добавка денег (в общем случае - как фузов, так и рубинов)
 */
public interface AnyMoneyAddition {

    int getMoney();

    void setMoney(int money);

    int getRealMoney();

    void setRealMoney(int realMoney);
}
