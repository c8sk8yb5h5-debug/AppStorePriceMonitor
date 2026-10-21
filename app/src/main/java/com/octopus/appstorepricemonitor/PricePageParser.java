package com.octopus.appstorepricemonitor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;

public final class PricePageParser {
    private PricePageParser() {}

    public static List<PriceRow> parse(String html) {
        Document doc = Jsoup.parse(html);
        List<PriceRow> result = new ArrayList<>();
        for (Element row : doc.select("tr")) {
            Elements cells = row.select("th,td");
            if (cells.size() < 4) continue;
            String first = clean(cells.get(0).text());
            if (!first.matches("\\d{1,3}")) continue;
            int rank;
            try { rank = Integer.parseInt(first); } catch (Exception e) { continue; }

            Element countryImage = cells.get(1).selectFirst("img");
            String country = countryImage == null ? clean(cells.get(1).text()) : clean(countryImage.attr("alt"));
            if (country.isEmpty()) country = clean(cells.get(1).text());
            country = country.replaceAll("^(.{2,})(?:\\s+\\1)$", "$1");
            String original = stripCurrencyCode(cellValue(cells.get(2)));
            String cny = cellValue(cells.get(3));
            String status = cells.size() > 4 ? clean(cells.get(4).text()) : "";
            if (country.isEmpty() || original.isEmpty() || cny.isEmpty()) continue;
            if (country.contains("地区") || original.contains("原价") || cny.equalsIgnoreCase("CNY")) continue;
            result.add(new PriceRow(rank, country, original, cny, status));
        }
        return result;
    }

    private static String cellValue(Element cell) {
        Element button = cell.selectFirst("button");
        return clean(button == null ? cell.text() : button.text());
    }

    private static String stripCurrencyCode(String s) {
        // Keep the local currency symbol and amount; remove trailing ISO currency codes such as IDR, INR, USD.
        return clean(s).replaceAll("\\s+[A-Z]{3}$", "").trim();
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }
}
