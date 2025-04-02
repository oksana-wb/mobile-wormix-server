package com.pragmatix.steam.web.request;

import com.pragmatix.steam.SteamAPIInterface;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Ivan Novikov mailto: <a href="mailto:novikov@pragmatix-corp.com">novikov@pragmatix-corp.com</a>
 *         Created: 07.03.2017 16:15
 *         <p>
 * Базовый класс запросов к steam web api
 */
public abstract class SteamWebRequest {

    /**
     * @return метод Steam Web API, которому соответствует данный класс
     */
    public abstract SteamAPIInterface.Method getMethod();

    /**
     * @param appId id приложения, который может входить в число параметров в некоторых запросах
     * @return параметры данного запроса в виде Map'ы
     */
    public abstract Map<String, String> toMap(int appId);

    public List<NameValuePair> toNameValues(int appId) {
        Map<String, String> params = toMap(appId);
        List<NameValuePair> res = new ArrayList<>(params.size());
        params.forEach((key, value) ->
                res.add(newPair(key, value))
        );
        return res;
    }

    public static NameValuePair newPair(String key, String value) {
        return new BasicNameValuePair(key, value);
    }

    // утилитный метод для превращения в параметры вложенного списка элементов
    protected static <T> void itemsToMap(Collection<T> items, Function<T, Map<String,String>> itemToMap, Map<String, String> result) {
        int i = 0;
        for (T item : items) {
            Map<String, String> itemMap = itemToMap.apply(item);
            for (Map.Entry<String, String> entry : itemMap.entrySet()) {
                result.put(entry.getKey()+"["+i+"]", entry.getValue());
            }
            i++;
        }
    }
}
