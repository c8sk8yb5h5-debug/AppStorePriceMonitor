package com.octopus.appstorepricemonitor;

public class PriceRow {
    public final String country;
    public final String currency;
    public final double amount;
    public final String displayedPrice;

    public PriceRow(String country, String currency, double amount, String displayedPrice) {
        this.country = country;
        this.currency = currency;
        this.amount = amount;
        this.displayedPrice = displayedPrice;
    }
}
