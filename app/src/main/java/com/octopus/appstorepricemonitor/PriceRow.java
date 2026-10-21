package com.octopus.appstorepricemonitor;

import java.util.Locale;

public class PriceRow {
    public final int rank;
    public final String country;
    public final String originalPrice;
    public final String cnyPrice;
    public final String status;

    // Compatibility fields used by the background price-change monitor.
    public final String currency;
    public final double amount;
    public final String displayedPrice;

    public PriceRow(int rank, String country, String originalPrice, String cnyPrice, String status) {
        this.rank = rank;
        this.country = country == null ? "" : country.trim();
        this.originalPrice = originalPrice == null ? "" : originalPrice.trim();
        this.cnyPrice = cnyPrice == null ? "" : cnyPrice.trim();
        this.status = status == null ? "" : status.trim();
        this.displayedPrice = this.originalPrice;

        String cleaned = this.originalPrice.replace(",", "").replaceAll("\\s+", "").trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^([^0-9.\\-]*)(-?[0-9]+(?:\\.[0-9]+)?)")
                .matcher(cleaned);
        String code = "";
        double value = -1;
        if (matcher.find()) {
            code = matcher.group(1);
            try { value = Double.parseDouble(matcher.group(2)); }
            catch (NumberFormatException ignored) { value = -1; }
        }
        if (code.isEmpty()) {
            String lower = this.country.toLowerCase(Locale.ROOT);
            if (lower.contains("united states") || lower.contains("usa") || lower.contains("美国")) code = "USD";
            else if (lower.contains("euro") || lower.contains("德国") || lower.contains("法国")) code = "EUR";
            else if (lower.contains("英国") || lower.contains("united kingdom")) code = "GBP";
            else if (lower.contains("日本") || lower.contains("japan")) code = "JPY";
            else if (lower.contains("中国") || lower.contains("china")) code = "CNY";
            else code = this.country;
        }
        this.currency = code;
        this.amount = value;
    }
}
