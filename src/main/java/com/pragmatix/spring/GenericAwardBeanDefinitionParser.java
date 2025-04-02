package com.pragmatix.spring;

import com.pragmatix.app.common.Race;
import com.pragmatix.app.settings.GenericAward;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 14.06.2016 13:50
 */
public class GenericAwardBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected Class getBeanClass(Element element) {
        return GenericAward.class;
    }

    private static final Pattern numberRangePattern = Pattern.compile("\\d+(-\\d+)?");

    private static final Pattern reagentsCountPattern = Pattern.compile("\\d+:\\d+(-\\d+)?");

    private static final Pattern reagentsMapCountPattern = Pattern.compile("\\d+:\\d+( \\d+:\\d+)*");

    @Override
    protected void doParse(Element element, BeanDefinitionBuilder bean) {
        /*
               <xsd:attribute name="name" type="xsd:string" use="optional"/>
               <xsd:attribute name="awardItemsStr" type="xsd:string" use="optional"/>
               <xsd:attribute name="reagentsMassStr" type="xsd:string" use="optional"/>
               <xsd:attribute name="reagentsCount" type="xsd:string" use="optional"/>
               <xsd:attribute name="reagentsMap" type="xsd:string" use="optional"/>
               <xsd:attribute name="seasonWeapons" type="xsd:string" use="optional"/>
               <xsd:attribute name="money" type="xsd:string" use="optional" default="0"/>
               <xsd:attribute name="realMoney" type="xsd:string" use="optional" default="0"/>
               <xsd:attribute name="reactionRate" type="xsd:string" use="optional" default="0"/>
               <xsd:attribute name="key" type="xsd:integer" use="optional" default="0"/>
               <xsd:attribute name="price" type="xsd:integer" use="optional" default="0"/>
               <xsd:attribute name="setStuff" type="xsd:boolean" use="optional" default="false"/>
               <xsd:attribute name="setItem" type="xsd:boolean" use="optional" default="false"/>
               <xsd:attribute name="addExperience" type="xsd:boolean" use="optional" default="false"/>
               <xsd:attribute name="race" type="xsd:string" use="optional" default=""/>
               <xsd:attribute name="skins" type="xsd:string" use="optional" default=""/>
               <xsd:attribute name="wagerToken" type="xsd:string" use="optional" default=""/>
               <xsd:attribute name="bossToken" type="xsd:string" use="optional" default=""/>
               <xsd:attribute name="battles" type="xsd:string" use="optional" default=""/>
         */
        bean.addPropertyValue("name", element.getAttribute("name"));
        bean.addPropertyValue("awardItemsStr", element.getAttribute("awardItemsStr"));
        bean.addPropertyValue("reagentsMassStr", element.getAttribute("reagentsMassStr"));
        bean.addPropertyValue("seasonWeapons", element.getAttribute("seasonWeapons"));

        addNumberRangeField(bean, element, "money", "money", "moneyFrom", "moneyTo");
        addNumberRangeField(bean, element, "realMoney", "realMoney", "realMoneyFrom", "realMoneyTo");
        addNumberRangeField(bean, element, "reactionRate", "reactionRate", "reactionRateFrom", "reactionRateTo");

        String reagentsCount = element.getAttribute("reagentsCount");
        if(!reagentsCount.isEmpty()) {
            if(!reagentsCountPattern.matcher(reagentsCount).matches())
                throw new IllegalStateException("GenericAward: Не корректный формат в поле 'reagentsCount'! [" + reagentsCount + "]");
            String[] ss = reagentsCount.split(":");
            bean.addPropertyValue("reagentsCount", Integer.valueOf(ss[0]));
            addNumberRangeField(bean, "reagentsCount", "singleReagentCount", "singleReagentCountFrom", "singleReagentCountTo", ss[1]);
        }

        String reagentsMap = element.getAttribute("reagentsMap").trim();
        if(!reagentsMap.isEmpty()) {
            if(!reagentsMapCountPattern.matcher(reagentsMap).matches())
                throw new IllegalStateException("GenericAward: Не корректный формат в поле 'reagentsMap'! [" + reagentsMap + "]");
            Map<Byte, Integer> reagents = Arrays.stream(reagentsMap.split(" ")).map(s -> s.split(":")).collect(Collectors.toMap(ss -> Byte.valueOf(ss[0]), ss -> Integer.valueOf(ss[1])));
            bean.addPropertyValue("reagents", reagents);
        }

        bean.addPropertyValue("key", Integer.valueOf(element.getAttribute("key")));
        bean.addPropertyValue("price", Integer.valueOf(element.getAttribute("price")));

        bean.addPropertyValue("setItem", Boolean.valueOf(element.getAttribute("setStuff")) || Boolean.valueOf(element.getAttribute("setItem")));
        bean.addPropertyValue("addExperience", Boolean.valueOf(element.getAttribute("addExperience")));

        String race = element.getAttribute("race");
        if(StringUtils.isNotEmpty(race))
            bean.addPropertyValue("race", Race.valueOf(race));

        String skins = element.getAttribute("skins");
        if(StringUtils.isNotEmpty(skins))
            bean.addPropertyValue("skins", skins.split(","));

        String wagerToken = element.getAttribute("wagerToken").trim();
        if(StringUtils.isNotEmpty(wagerToken))
            bean.addPropertyValue("wagerWinAwardToken", Integer.valueOf(wagerToken));

        String bossToken = element.getAttribute("bossToken").trim();
        if(StringUtils.isNotEmpty(bossToken))
            bean.addPropertyValue("bossWinAwardToken", Integer.valueOf(bossToken));

        String battles = element.getAttribute("battles").trim();
        if(StringUtils.isNotEmpty(battles))
            bean.addPropertyValue("battlesCount", Integer.valueOf(battles));

        String experience = element.getAttribute("experience").trim();
        if(StringUtils.isNotEmpty(experience))
            bean.addPropertyValue("experience", Integer.valueOf(experience));
    }

    private void addNumberRangeField(BeanDefinitionBuilder bean, Element element, String attrName, String singleValueName, String fromValueName, String toValueName) {
        String attr = element.getAttribute(attrName).trim();
        if(!attr.isEmpty()) {
            if(!numberRangePattern.matcher(attr).matches())
                throw new IllegalStateException("GenericAward: Не корректный формат в поле '" + attrName + "'! [" + attr + "]");

            addNumberRangeField(bean, attrName, singleValueName, fromValueName, toValueName, attr);
        }
    }

    private void addNumberRangeField(BeanDefinitionBuilder bean, String attrName, String singleValueName, String fromValueName, String toValueName, String attr) {
        if(attr.contains("-")) {
            String[] ss = attr.split("-");
            int from = Integer.valueOf(ss[0]);
            int to = Integer.valueOf(ss[1]);
            if(from >= to || from == 0)
                throw new IllegalStateException("GenericAward: Не корректный диапазон в поле '" + attrName + "'! [" + attr + "]");
            bean.addPropertyValue(fromValueName, from);
            bean.addPropertyValue(toValueName, to);

        } else {
            bean.addPropertyValue(singleValueName, Integer.valueOf(attr));
        }
    }

}
