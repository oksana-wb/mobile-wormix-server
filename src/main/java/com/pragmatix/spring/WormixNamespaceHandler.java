package com.pragmatix.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Обработчих схемы com/pragmatix/dpring/wormix.xsd
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 28.09.11 13:12
 *
 */
public class WormixNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("weapon", new WeaponBeanDefinitionParser());
        registerBeanDefinitionParser("stuff", new StuffBeanDefinitionParser());
        registerBeanDefinitionParser("level", new LevelBeanDefinitionParser());
        registerBeanDefinitionParser("award", new GenericAwardBeanDefinitionParser());
    }

}
