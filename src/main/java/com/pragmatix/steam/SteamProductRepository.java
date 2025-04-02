package com.pragmatix.steam;

import com.pragmatix.steam.domain.SteamCountry;
import com.pragmatix.steam.domain.SteamCurrency;
import com.pragmatix.steam.domain.SteamPriceSegment;
import com.pragmatix.steam.domain.SteamProduct;
import com.pragmatix.steam.utils.Config;
import com.pragmatix.steam.utils.ConfigLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: Vladimir
 * Date: 10.03.2017 10:45
 */
@Component
public class SteamProductRepository {

    @Value("${SteamProductRepository.configUrl:classpath:json/steam_price.json}")
    private String configUrl;

    public final Map<String, SteamProduct> products = new LinkedHashMap<>();

    public final Map<String, SteamCurrency> currencies = new LinkedHashMap<>();
    public final Map<String, SteamCountry> countries = new LinkedHashMap<>();
    public final Map<String, SteamPriceSegment> priceList = new LinkedHashMap<>();
    public final Map<SteamCountry, SteamPriceSegment> countryToSegmentMap = new LinkedHashMap<>();

    public String defaultLangCode;

    public SteamCurrency defaultCurrency;

    public SteamPriceSegment defaultProductPrices;

    @PostConstruct
    public void init() {
        Map config = ConfigLoader.loadConfig(configUrl);

        SteamProduct[] products = Config.getArray(steamProductConstructor, config, "products");
        for(SteamProduct product : products) {
            boolean duplicate = this.products.containsKey(product.code);
            if(!duplicate) {
                this.products.put(product.code, product);
            }
            if(duplicate) {
                throw new IllegalArgumentException(String.format("Product with key '%s' already exists", product.code));
            }
        }

        SteamCurrency[] currencies = Config.getArray(steamCurrencyConstructor, config, "currencies");
        for(SteamCurrency currency : currencies) {
            this.currencies.put(currency.code, currency);

            for(String segmentCode : currency.segments) {
                if(priceList.containsKey(segmentCode)) {
                    throw new IllegalArgumentException(String.format("Segment with code '%s' already exists", segmentCode));
                }
                SteamPriceSegment segment = new SteamPriceSegment(segmentCode, currency);
                priceList.put(segment.code, segment);
            }
        }

        defaultLangCode = Config.getString(config, "defaultLang").toUpperCase();

        String defaultCurrencyCode = Config.getString(config, "defaultCurrency");
        defaultCurrency = this.currencies.get(defaultCurrencyCode);
        if(defaultCurrency == null) {
            throw new IllegalArgumentException(String.format("Invalid default currency code '%s'", defaultCurrencyCode));
        }
        defaultProductPrices = priceList.get(defaultCurrency.segments[0]);

        SteamCountry[] countries = Config.getArray(steamCountryConstructor, config, "countries");
        for(SteamCountry country : countries) {
            this.countries.put(country.code, country);

            if(countryToSegmentMap.containsKey(country)) {
                throw new IllegalArgumentException(String.format("Country %s belongs to multiple segments %s %s",
                        country.code, country.segment.code, countryToSegmentMap.get(country).code));
            }

            country.segment.addCountry(country);
            countryToSegmentMap.put(country, country.segment);
        }

        loadPriceList((List) config.get("priceList"));
    }

    public SteamProduct getProduct(String code) {
        SteamProduct res = products.get(code);
        if(res == null) {
            throw new IllegalArgumentException("Код платежа steam не зарегистрирован! " + code);
        }
        return res;
    }

    public SteamPriceSegment getProductPrices(String countryCode, String currencyCode) {
        SteamPriceSegment res;

        countryCode = countryCode.toUpperCase();
        currencyCode = currencyCode.toUpperCase();

        SteamCurrency currency = currencies.get(currencyCode);
        SteamCountry country = countries.get(countryCode);

        if(currency != null) {
            if(country != null) {
                res = countryToSegmentMap.get(country);

                if(res != null && res.currency.equals(currency)) {
                    return res;
                }
            }

            return priceList.get(currency.segments[0]);
        }

        if(country != null) {
            res = countryToSegmentMap.get(country);
            if(res != null) {
                return res;
            }
            for(String segmentCode : defaultCurrency.segments) {
                res = priceList.get(segmentCode);
                if(res.countries.contains(country)) {
                    return res;
                }
            }
        }

        return defaultProductPrices;
    }

    private void loadPriceList(List priceListConfigs) {
        for(Object aSegmentConfig : priceListConfigs) {
            Map segmentConfig = (Map) aSegmentConfig;
            String segmentCode = Config.getString(segmentConfig, "segment");
            SteamPriceSegment segment = priceList.get(segmentCode);
            if(segment == null) {
                throw new IllegalArgumentException(String.format("No such segment '%s'", segmentCode));
            }
            SteamCurrency currency = segment.currency;
            List productConfigs = (List) segmentConfig.get("products");
            for(Object aProductConfig : productConfigs) {
                Map productConfig = (Map) aProductConfig;
                String code = Config.getString(productConfig, "code");
                SteamProduct product = getProduct(code);
                if(product == null) {
                    throw new IllegalArgumentException(String.format("No such product '%s'", code));
                }
                int price = Config.getInt(productConfig, "price");
                if(price % currency.centRounding != 0) {
                    throw new IllegalArgumentException(String.format("Invalid product '%s' cent price %d in segment '%s'",
                            product, price, segment.code));
                }
                segment.addProductPrice(product, price);
            }
        }
    }

    private final Config.ArrayConstructor<SteamProduct> steamProductConstructor = new Config.ArrayConstructor<SteamProduct>() {
        @Override
        public SteamProduct[] array(int length) {
            return new SteamProduct[length];
        }

        @Override
        public SteamProduct construct(Object aConfig) {
            Map config = (Map) aConfig;

            int id = Config.getInt(config, "id");
            SteamProduct.SteamProductCategory category = SteamProduct.SteamProductCategory.valueOf(Config.getString(config, "category"));
            String code = Config.getString(config, "code");
            int count = Config.getInt(config, "count");

            Map descriptionConfig = (Map) config.get("description");
            Map<String, String> descriptions = new LinkedHashMap<>();
            for(Object key : descriptionConfig.keySet()) {
                descriptions.put(((String) key).toUpperCase(), (String) descriptionConfig.get(key));
            }
            int paymentAmountComeback = Config.getIntDefault(config, "paymentAmountComeback", 0);
            return new SteamProduct(id, category, code, descriptions, count, paymentAmountComeback);
        }
    };

    private final Config.ArrayConstructor<SteamCurrency> steamCurrencyConstructor = new Config.ArrayConstructor<SteamCurrency>() {
        @Override
        public SteamCurrency[] array(int length) {
            return new SteamCurrency[length];
        }

        @Override
        public SteamCurrency construct(Object aConfig) {
            Map config = (Map) aConfig;

            String code = Config.getString(config, "code").toUpperCase();
            String name = Config.getString(config, "name");
            int centRounding = Config.getIntDefault(config, "centRounding", 1);
            String[] priceSegments = Config.getStringArrayDefault(config, "segments", new String[]{code});
            for(int i = 0; i < priceSegments.length; i++) {
                priceSegments[i] = priceSegments[i].toUpperCase();
            }

            return new SteamCurrency(code, name, centRounding, priceSegments);
        }
    };

    private final Config.ArrayConstructor<SteamCountry> steamCountryConstructor = new Config.ArrayConstructor<SteamCountry>() {
        @Override
        public SteamCountry[] array(int length) {
            return new SteamCountry[length];
        }

        @Override
        public SteamCountry construct(Object aConfig) {
            Map config = (Map) aConfig;

            String code = Config.getString(config, "code").toUpperCase();
            String name = Config.getString(config, "name");
            String segmentCode = Config.getString(config, "segment");
            SteamPriceSegment priceSegment = priceList.get(segmentCode);

            if(priceSegment == null) {
                throw new IllegalArgumentException(String.format("Undeclared price segment %s", segmentCode));
            }

            return new SteamCountry(code, name, priceSegment);
        }
    };
}
