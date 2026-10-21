package com.octopus.appstorepricemonitor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Best-effort parser for the server-rendered country price table on AppStorePrice.org. */
public final class PricePageParser {
    private static final Set<String> CURRENCIES = new HashSet<>(Arrays.asList(
            "PHP","CAD","PKR","EGP","JPY","VND","IDR","BRL","USD","TRY","CLP","AUD",
            "THB","INR","TWD","MXN","KRW","AED","KZT","ILS","SGD","NGN","SAR","CHF",
            "ZAR","MYR","EUR","NOK","GBP","DKK","COP","HKD","PLN","SEK","CZK","HUF",
            "NZD","ARS","PEN","RON","UAH","BDT","LKR","MAD","QAR","KWD","BHD","OMR"
    ));
    private static final Pattern NUMBER = Pattern.compile("[-+]?[0-9][0-9,]*(?:\\.[0-9]+)?");

    private PricePageParser() {}

    public static List<PriceRow> parse(String html) {
        Document doc = Jsoup.parse(html);
        List<PriceRow> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Elements rows = doc.select("tr");
        for (Element row : rows) {
            String country = "";
            Element countryLink = row.selectFirst("a[href*=apps.apple.com]");
            if (countryLink != null) country = countryLink.text().trim();
            if (country.isEmpty()) continue;

            String currency = "";
            String shown = "";
            Elements buttons = row.select("button");
            for (Element button : buttons) {
                String text = normalize(button.text());
                String code = findCurrencyCode(text);
                if (!code.isEmpty() && !"CNY".equals(code)) {
                    currency = code;
                    shown = text;
                    break;
                }
            }
            if (currency.isEmpty()) {
                String text = normalize(row.text());
                currency = findCurrencyCode(text);
                if (currency.isEmpty() || "CNY".equals(currency)) continue;
                shown = text;
            }
            Matcher matcher = NUMBER.matcher(shown);
            if (!matcher.find()) continue;
            double amount;
            try { amount = Double.parseDouble(matcher.group().replace(",", "")); }
            catch (Exception ignored) { continue; }
            if (amount <= 0) continue;
            String key = country + "|" + currency + "|" + amount;
            if (seen.add(key)) result.add(new PriceRow(country, currency, amount, shown));
        }
        return result;
    }

    private static String findCurrencyCode(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        Matcher m = Pattern.compile("\\b[A-Z]{3}\\b").matcher(upper);
        while (m.find()) if (CURRENCIES.contains(m.group())) return m.group();
        return "";
    }

    private static String normalize(String s) { return s == null ? "" : s.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim(); }
}
