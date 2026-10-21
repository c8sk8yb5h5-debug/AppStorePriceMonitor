package com.octopus.appstorepricemonitor;

public class PriceRow {
    public final int rank;
    public final String country;
    public final String originalPrice;
    public final String cnyPrice;
    public final String status;

    public PriceRow(int rank, String country, String originalPrice, String cnyPrice, String status) {
        this.rank = rank;
        this.country = country;
        this.originalPrice = originalPrice;
        this.cnyPrice = cnyPrice;
        this.status = status;
    }
}
