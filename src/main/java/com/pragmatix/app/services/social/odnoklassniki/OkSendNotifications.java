package com.pragmatix.app.services.social.odnoklassniki;

import ch.qos.logback.classic.PatternLayout;
import com.pragmatix.gameapp.social.service.SocialUserIdMap;
import com.pragmatix.gameapp.social.service.odnoklassniki.OdnoklassnikiService;
import com.pragmatix.utils.logging.AccountConverter;
import com.pragmatix.utils.logging.ColorOffConverter;
import com.pragmatix.utils.logging.ColorOnConverter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

/**
 * Рассылка всем
 *
 * @author Eugene Bereskin mailto: <a href="mailto:john.boroda@gmail.com">john.boroda@gmail.com</a>
 *         Created: 22.10.12 18:14
 *         http://dev.odnoklassniki.ru/wiki/display/ok/REST+API+-+notifications.sendMass+ru
 */
public class OkSendNotifications {

    static {
        // для отображения аккаунта
        PatternLayout.defaultConverterMap.put("account", AccountConverter.class.getName());
        // устанавливаем настройки цветов для консоли
        PatternLayout.defaultConverterMap.put("coloron", ColorOnConverter.class.getName());
        PatternLayout.defaultConverterMap.put("coloroff", ColorOffConverter.class.getName());
    }

//  odnoklassniki.apiUrl=http://api.odnoklassniki.ru/fb.do
//  odnoklassniki.applicationKey=CBAMPHHGABABABABA
//  odnoklassniki.applicationSecretKey=18C6488DD5CFAFA016428C96

    private static OdnoklassnikiService odnoklassnikiService = new OdnoklassnikiService();

    public static void main(String[] args) throws IOException {

        //Сообщение, которое будет отправлено как оповещение пользователю. Максимальная длина текста составляет 200 символов.
        String text = "Подарки к празднику! Поздравляем всех бойцов Вормикс с Днем защитника Отечества!";
//        String text = "Акция с ключами заканчивается в понедельник! Успей собрать как можно больше ключей за выходные!";
//        String text = "Собирай ключи для новогодних сундуков скорее! До конца акции всего неделя!";
//        String text = "Поздравляем с Новым Годом! Поспешите зайти в игру, вас ждет праздничный подарок!";
//        String text = "Сегодня Вормикс исполнилось 4 года! 4 года побед. 4 года лидерства. Отметим это событие вместе! Всех ждут особенные подарки!";
//        String text = "Мы ждали этого целый год: новая раса в Вормикс! А ещё: новогодние подарки, волшебные апгрейды и сногшибательные боссы в главном обновлении года!";
//        String text = "Сверхъестественное обновление! В ночь Хэллоуина тебя ждут мистические приключения и призы!";
//        String text = "Большое обновление! Множество крутых шапок, новые топы игроков, суровые боссы и апгрейды. Не пропусти!";
//        String text = "Первое осеннее обновление! Новое оружие, новые супербоссы, клановые флаги, крафт и многое другое!";
//        String text = "Горячее обновление! Открыт сезон эпичного крафта и охоты на супербоссов!";
//        String text = "Заходи в Вормикс каждый день, и на 5 день ты получишь от 3 до 5 рубинов! Успей сорвать банк: акция скоро завершится!";
//        String text = "Горячая акция: 5 дней — 5 рубинов! Заходи в Вормикс каждый день, и через 5 дней получишь от 3 до 5 рубинов! Успей сорвать банк: у тебя две недели!";
//        String text = "Стань командиром собственного клана! Собери друзей и бейся за первое место в топе!";
//        String text = "Нас уже 2 000 000, и мы продолжаем идти дальше! Поспешите зайти в игру и получить подарок!";
//        String text = "Поздравляем с Днем космонавтики! Поторопитесь получить подарки в игре!";
//        String text = "Джекпот! Всем ветеранам вормикса подарки! Быстрее заходи в игру и получи приз!";
//        String text = "Весеннее обновление! Новые боссы, оружие и артефакты уже в игре!";
//        String text = "Февральское обновление! Теперь вы можите играть с другом против боссов, а также вас ждут артефакты и многое другое!!!";
//        String text = "У нас ядерное обновление! Теперь можно играть в полноэкранном режиме, появились бои 2х2 и многое другое!";
//        String text = "Вормикс установили уже более 1 000 000 игроков, и мы уверенно идем дальше! Заходите в игру и получайте подарки!";
//        String text = "Внимание! Осенние скидки на покупку игровой валюты, поторопись купить рубины с огромной скидкой!";
//        String text = "У нас взрывное обновление! 2 оружия, 9 улучшений и много новых возможностей!";
//        String text = "У нас горячее обновление! 18(!) новых улучшений, 5 интересных боссов и многое другое!";
        //Дата и время истечения срока действия этого оповещения
        int expires = 7;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, expires);
        Date expiresTime = cal.getTime();

        Properties prop = new Properties();
        String propsFileName = "/odnoklassniki/server.properties";
        System.out.println("load properties from " + propsFileName);
        InputStream in = OkSendNotifications.class.getResourceAsStream(propsFileName);
        prop.load(in);

        String apiUrl = prop.getProperty("odnoklassniki.apiUrl");
        String applicationKey = prop.getProperty("odnoklassniki.applicationKey");
        String applicationSecretKey = prop.getProperty("odnoklassniki.applicationSecretKey");

        System.out.println("odnoklassniki.apiUrl=" + apiUrl);
        System.out.println("odnoklassniki.applicationKey=" + applicationKey);
        System.out.println("odnoklassniki.applicationSecretKey=" + applicationSecretKey);
        System.out.println("text=" + text);
        System.out.println("text.length=" + text.length());
        System.out.println("expiresTime=" + expiresTime);

        odnoklassnikiService.setConnectionTimeout(1000);
        odnoklassnikiService.setReadTimeout(5000);
        odnoklassnikiService.setApiUrl(apiUrl);
        odnoklassnikiService.setApplicationKey(applicationKey);
        odnoklassnikiService.setApplicationSecretKey(applicationSecretKey);
        odnoklassnikiService.setUserIdMap(new SocialUserIdMap<Long>() {
            @Override
            public String mapToStringId(Object userId) {
                return userId.toString();
            }

            @Override
            public Long mapToNumberId(Object userId) {
                return null;
            }

            @Override
            public void map(String stringId, Long longId) {
            }

            @Override
            public void map(String stringId, short socialNetId, Long longId) {
            }
        });
        odnoklassnikiService.init();

        HashMap<String, String> filter = new HashMap<String, String>();
        String result = odnoklassnikiService.sendMassNotification(filter, text, expiresTime);

        System.out.println("sendMassNotification result=" + result);
    }

}
