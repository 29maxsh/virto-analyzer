package ru.VirtaMarketAnalyzer.main;

import ru.VirtaMarketAnalyzer.data.*;
import ru.VirtaMarketAnalyzer.parser.CityInitParser;
import ru.VirtaMarketAnalyzer.parser.CityListParser;
import ru.VirtaMarketAnalyzer.parser.CityParser;
import ru.VirtaMarketAnalyzer.parser.ProductInitParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by cobr123 on 25.04.2015.
 */
public final class Wizard {
    public static void main(String[] args) throws IOException {
        final List<String> realms = new ArrayList<>();
        realms.add("lien");
        realms.add("olga");
        realms.add("vera");
        realms.add("anna");
        realms.add("mary");
        for (final String realm : realms) {
            collectToJson(realm);
        }
        //публикуем на сайте
    }

    public static void collectToJson(final String realm) throws IOException {
        final String baseDir = Utils.getDir() + realm + File.separator;
        //страны
        final List<Country> countries = CityInitParser.getCountries("http://virtonomica.ru/" + realm + "/main/common/main_page/game_info/world/");
        Utils.writeToGson(baseDir + "countries.json", countries);
        //регионы
        final List<Region> regions = CityInitParser.getRegions("http://virtonomica.ru/" + realm + "/main/geo/regionlist/", countries);
        Utils.writeToGson(baseDir + "regions.json", regions);
        //города и уровень богатства городов
        final List<City> cities = CityListParser.fillWealthIndex("http://virtonomica.ru/" + realm + "/main/geo/citylist/", regions);
        Utils.writeToGson(baseDir + "cities.json", cities);
        Utils.log("cities.size() = ",cities.size());
        //получаем список доступных розничных товаров
        final List<Product> products = ProductInitParser.getProducts("http://virtonomica.ru/" + realm + "/main/common/main_page/game_info/trading/");
        Utils.writeToGson(baseDir + "products.json", products);
        Utils.log("products.size() = ", products.size());
        //получаем список доступных розничных категорий товаров
        final List<ProductCategory> product_categories = ProductInitParser.getProductCategories(products);
        Utils.writeToGson(baseDir + "product_categories.json", product_categories);
        //собираем данные продаж товаров в городах
        final Map<String, List<TradeAtCity>> stats = CityParser.collectByTradeAtCities("http://virtonomica.ru/" + realm + "/main/globalreport/marketing/by_trade_at_cities/", cities, products);
        //сохраняем их в json
        for (final String key : stats.keySet()) {
            final List<TradeAtCity> list = stats.get(key);
            Utils.writeToGson(baseDir + "tradeAtCity_" + key + ".json", list);
        }
    }
}
