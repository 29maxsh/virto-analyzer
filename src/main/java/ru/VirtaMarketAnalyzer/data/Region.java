package ru.VirtaMarketAnalyzer.data;

import com.google.gson.annotations.SerializedName;

/**
 * Created by cobr123 on 25.04.2015.
 */
public final class Region {
    @SerializedName("ci")
    final private String countryId;
    @SerializedName("i")
    final private String id;
    @SerializedName("c")
    final private String caption;

    public Region(final String countryId, final String id, final String caption) {
        this.countryId = countryId;
        this.id = id;
        this.caption = caption;
    }

    public String getId() {
        return id;
    }

    public String getCountryId() {
        return countryId;
    }

    public String getCaption() {
        return caption;
    }

}
