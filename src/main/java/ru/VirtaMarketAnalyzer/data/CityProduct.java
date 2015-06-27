package ru.VirtaMarketAnalyzer.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.VirtaMarketAnalyzer.parser.CityParser;

import java.io.IOException;

/**
 * Created by cobr123 on 27.06.2015.
 */
final public class CityProduct {
    private final City city;
    private final Product product;
    private final String url;
    private static final Logger logger = LoggerFactory.getLogger(CityProduct.class);

    public CityProduct(final City city, final Product product, final String url) {
        this.city = city;
        this.product = product;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public City getCity() {
        return city;
    }

    public Product getProduct() {
        return product;
    }

    public TradeAtCity getTradeAtCity() {
        try {
            return CityParser.get(getUrl(), getCity(), getProduct());
        } catch (final IOException e) {
            logger.info(e.getLocalizedMessage(), e);
        }
        return null;
    }
}
