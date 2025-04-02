package com.pragmatix.spring;

import com.pragmatix.app.model.Level;
import com.pragmatix.app.model.Stuff;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * Класс парсер элемента level
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 01.02.12 13:16
 */
public class LevelBeanDefinitionParser extends WeaponBeanDefinitionParser {

    @Override
    protected Class getBeanClass(Element element) {
        return Level.class;
    }

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder bean) {
        bean.addPropertyValue("level", Integer.valueOf(element.getAttribute("level").trim()));
        bean.addPropertyValue("nextLevelExp", Integer.valueOf(element.getAttribute("nextLevelExp").trim()));
        bean.addPropertyValue("levelHp", Integer.valueOf(element.getAttribute("levelHp").trim()));
        bean.addPropertyValue("maxWormsCount", Integer.valueOf(element.getAttribute("worms").trim()));
        String award = element.getAttribute("award");
        if(!award.isEmpty()){
            bean.addPropertyReference("award", award);
        }
        bean.addPropertyValue("delay", Integer.valueOf(element.getAttribute("delay").trim()));
    }

}
