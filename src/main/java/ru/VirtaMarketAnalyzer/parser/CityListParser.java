package ru.VirtaMarketAnalyzer.parser;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.VirtaMarketAnalyzer.data.City;
import ru.VirtaMarketAnalyzer.data.Region;
import ru.VirtaMarketAnalyzer.main.Utils;
import ru.VirtaMarketAnalyzer.main.Wizard;
import ru.VirtaMarketAnalyzer.scrapper.Downloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cobr123 on 25.04.2015.
 */
public final class CityListParser {
    private static final Logger logger = LoggerFactory.getLogger(CityListParser.class);

    public static void main(final String[] args) throws IOException {
        BasicConfigurator.configure(new ConsoleAppender(new PatternLayout("%r %d{ISO8601} [%t] %p %c %x - %m%n")));
        final Document doc = Downloader.getDoc(Wizard.host + "olga/main/geo/citylist/331858");
        final Element table = doc.select("table[class=\"grid\"]").last();
        //System.out.println(list.outerHtml());
        final Elements towns = table.select("table > tbody > tr");
        for (Element town : towns) {
            final String[] parts = town.select("tr > td:nth-child(1) > a").eq(0).attr("href").split("/");
            logger.info(parts[parts.length - 1]);
            logger.info("" + Utils.toDouble(town.select("tr > td:nth-child(6)").html()));
        }
    }

    public static List<City> fillWealthIndex(final String url, final List<Region> regions) throws IOException {
        final List<City> cities = new ArrayList<>();
        for (final Region region : regions) {
            getWealthIndex(url, region, cities);
        }
        return cities;
    }

    public static void getWealthIndex(final String url, final Region region, final List<City> cities) throws IOException {
        final Document doc = Downloader.getDoc(url + region.getId());
        final Element table = doc.select("table[class=\"grid\"]").last();
//        System.out.println(table.outerHtml());
        final Elements towns = table.select("table > tbody > tr");
        towns.stream().filter(town -> !town.select("tr > td:nth-child(1) > a").isEmpty()).forEach(town -> {
            final String[] parts = town.select("tr > td:nth-child(1) > a").eq(0).attr("href").split("/");
            final String caption = town.select("tr > td:nth-child(1) > a").eq(0).text();
            final String id = parts[parts.length - 1];
            final String averageSalary = town.select("tr > td:nth-child(3)").text();
            final String educationIndex = town.select("tr > td:nth-child(5)").text();
            final String wealthIndex = town.select("tr > td:nth-child(6)").html();
            final int demography = Utils.toInt(town.select("tr > td:nth-child(7)").text().replace("ths.","").replace("тыс. чел.","").replaceAll("\\s+",""));
            cities.add(new City(region.getCountryId(), region.getId(), id, caption, Utils.toDouble(wealthIndex), Utils.toDouble(educationIndex), Utils.toDouble(averageSalary), demography));
        });
    }
}