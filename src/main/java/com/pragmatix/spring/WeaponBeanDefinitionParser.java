package com.pragmatix.spring;

import com.pragmatix.app.common.ItemCheck;
import com.pragmatix.app.model.Weapon;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

/**
 * Класс парсер элемента weapon
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.09.11 13:16
 */
public class WeaponBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class getBeanClass(Element element) {
        return Weapon.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder bean) {
        bean.addPropertyValue("weaponId", Integer.valueOf(element.getAttribute("weaponId").trim()));
        bean.addPropertyValue("name", element.getAttribute("name").trim());
        bean.addPropertyValue("price", parsePrice(element.getAttribute("price").trim()));
        bean.addPropertyValue("realprice", parsePrice(element.getAttribute("realPrice").trim()));
        bean.addPropertyValue("requiredLevel", Integer.valueOf(element.getAttribute("level").trim()));
        bean.addPropertyValue("type", Weapon.WeaponType.valueOf(element.getAttribute("type").trim()));
        bean.addPropertyValue("sellPrice", Integer.valueOf(element.getAttribute("sellPrice").trim()));
        bean.addPropertyValue("maxWeaponLevel", Integer.valueOf(element.getAttribute("maxWeaponLevel").trim()));
        bean.addPropertyValue("shotsByTurn", Integer.valueOf(element.getAttribute("shots").trim()));
        bean.addPropertyValue("bulletsByShot", Integer.valueOf(element.getAttribute("bullets").trim()));
    }

    protected Integer parsePrice(String price) {
        try {
            return Integer.valueOf(price);
        } catch (NumberFormatException e) {
            return ItemCheck.EMPTY_PRICE;
        }
    }

    private Integer parseSingleId(String price) {
        try {
            return Integer.valueOf(price);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
