package com.pragmatix.spring;

import com.pragmatix.app.common.BoostFamily;
import com.pragmatix.app.model.Stuff;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * Класс парсер элемента stuff
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.09.11 13:16
 */
public class StuffBeanDefinitionParser extends WeaponBeanDefinitionParser {

    @Override
    protected Class getBeanClass(Element element) {
        return Stuff.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder bean) {
        bean.addPropertyValue("stuffId", Integer.valueOf(element.getAttribute("stuffId").trim()));
        bean.addPropertyValue("name", element.getAttribute("name").trim());
        bean.addPropertyValue("price", parsePrice(element.getAttribute("price").trim()));
        bean.addPropertyValue("realprice", parsePrice(element.getAttribute("realPrice").trim()));
        bean.addPropertyValue("requiredLevel", Integer.valueOf(element.getAttribute("requiredLevel").trim()));
        bean.addPropertyValue("hp", Integer.valueOf(element.getAttribute("hp").trim()));
        bean.addPropertyValue("reaction", Integer.valueOf(element.getAttribute("reaction").trim()));
        bean.addPropertyValue("special", Boolean.valueOf(element.getAttribute("special").trim()));
        bean.addPropertyValue("kit", Boolean.valueOf(element.getAttribute("kit").trim()));
        bean.addPropertyValue("temporal", Boolean.valueOf(element.getAttribute("temporal").trim()));
        bean.addPropertyValue("expire", element.getAttribute("expire").trim());
        String boostFamily = element.getAttribute("boostFamily").trim();
        bean.addPropertyValue("boostFamily", boostFamily.isEmpty() ? null : BoostFamily.valueOf(boostFamily));
        bean.addPropertyValue("boostParam", Integer.valueOf(element.getAttribute("boostParam").trim()));
        bean.addPropertyValue("sticker", Boolean.valueOf(element.getAttribute("sticker").trim()));
        bean.addPropertyValue("craftBase", Boolean.valueOf(element.getAttribute("craftBase").trim()));
    }

}
