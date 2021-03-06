package ru.VirtaMarketAnalyzer.parser;

import one.util.streamex.StreamEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.VirtaMarketAnalyzer.data.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * Created by cobr123 on 14.02.2019.
 */
final public class ProductionForRetailParser {
    private static final Logger logger = LoggerFactory.getLogger(ProductionForRetailParser.class);

    /*
     * перебираем города розницы, где местных больше 0.
     * перебираем товары розницы.
     * считаем производство товара в нукусе с качеством на 10, 20, 30 единиц больше местных и сравниваем с ценой местных * 1.5, 2 и 2.5 соответственно.
     * считаем прибыль с учетом доставки, пошлин и налогов в этом городе.
     * если себестоимость не выгодна, то доставку, пошлицы и налоги не считаем.
     * производство считаем на 1000 рабочих, а потом экстраполируем объем до 10% рынка города (или меньше, если местных меньше 10%).
     * сохраняем все с прибылью больше 0.
     */
    public static List<ProductionForRetail> genProductionForRetail(final String host, final String realm, final Product product) throws Exception {
        final List<City> cities = CityListParser.getCities(host, realm, false);
        final List<TechUnitType> techList = TechListParser.getTechUnitTypes(host, realm);
        final List<TechLvl> techLvls = TechMarketAskParser.getTech(host, realm, techList);
        final List<Manufacture> manufactures = ManufactureListParser.getManufactures(host, realm);
        final Map<String, List<ProductRecipe>> productRecipes = ProductRecipeParser.getProductRecipes(host, realm, manufactures);
        final List<Product> products = ProductRecipeParser.getProductFromRecipes(host, realm, productRecipes.get(product.getId()));
        final Map<String, List<ProductRemain>> productRemains = ProductRemainParser.getRemains(host, realm, products);

        return cities.parallelStream()
                .map(city -> genProductionForCity(host, realm, city, product, techLvls, productRemains, productRecipes.get(product.getId())))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparingDouble(ProductionForRetail::getIncomeAfterTax).reversed())
                .collect(Collectors.toList());
    }

    public static List<ProductionForRetail> genProductionForCity(
            final String host,
            final String realm,
            final City city,
            final Product product,
            final List<TechLvl> techLvls,
            final Map<String, List<ProductRemain>> productRemains,
            final List<ProductRecipe> productRecipes
    ) {
        return productRecipes.stream()
                .filter(pr -> pr.getInputProducts() != null && !pr.getInputProducts().isEmpty())
                .map(pr -> genProductionForCity(host, realm, city, product, techLvls, productRemains, pr))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    private static List<ProductionForRetail> genProductionForCity(
            final String host,
            final String realm,
            final City city,
            final Product product,
            final List<TechLvl> techLvls,
            final Map<String, List<ProductRemain>> productRemains,
            final ProductRecipe productRecipe
    ) {
        final int maxTechLvl = techLvls.stream()
                .filter(tl -> productRecipe.getManufactureID().equals(tl.getTechId()))
                .filter(tl -> tl.getPrice() > 0)
                .max((o1, o2) -> (o1.getLvl() > o2.getLvl()) ? 1 : -1)
                .orElse(new TechLvl("", 2, 0.0))
                .getLvl();

        final List<ProductionForRetail> result = new ArrayList<>();
        final TradeAtCity stat = new CityProduct(city, product, host, realm).getTradeAtCity().build();
        if (stat.getLocalPercent() > 0) {
            final List<List<ProductRemain>> materials = ProductionAboveAverageParser.getProductRemain(productRecipe, productRemains);

            for (int i = 0; i <= 3; ++i) {
                final double minQuality = stat.getLocalQuality() + 10.0 * i;
                final List<ProductionForRetail> tmp = IntStream.rangeClosed(1, maxTechLvl)
                        .mapToObj(lvl -> {
                            try {
                                return calcByRecipe(host, realm, stat, materials, productRecipe, lvl, minQuality, productRemains);
                            } catch (Exception e) {
                                logger.error(e.getLocalizedMessage(), e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(toList());
                result.addAll(tmp);
            }
        }
        return result;

    }

    private static List<ProductionForRetail> calcByRecipe(
            final String host,
            final String realm,
            final TradeAtCity stat,
            final List<List<ProductRemain>> materials,
            final ProductRecipe productRecipe,
            final int techLvl,
            final double minQuality,
            final Map<String, List<ProductRemain>> productRemains
    ) throws Exception {
        //Узбекистан - Нукус
        final City productionCity = CityListParser.getCity(host, realm, "310400", false);
        //оставляем по 3 лучших для каждого уровня технологии для каждого товара
        return StreamEx.cartesianProduct(materials)
                .limit(5)
                .map(mats -> {
                    try {
                        return ProductionAboveAverageParser.calcResult(host, realm, productRecipe, mats, techLvl, 0, productRemains, productionCity);
                    } catch (Exception e) {
                        logger.error(e.getLocalizedMessage(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(paa -> paa.getQuality() >= minQuality && paa.getProductID().equals(stat.getProductId()))
                .map(ppa -> {
                    try {
                        return new ProductionForRetail(host, realm, stat, ppa, productionCity);
                    } catch (Exception e) {
                        logger.error(e.getLocalizedMessage(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(o -> o.getSellPrice() - o.getBuyPrice() > o.getCost() * 0.01)
                .sorted(Comparator.comparingDouble(ProductionForRetail::getIncomeAfterTax).reversed())
                .limit(3)
                .collect(toList());
    }
}
