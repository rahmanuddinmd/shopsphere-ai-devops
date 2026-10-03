package com.shopsphere.ai;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a shopper's question into catalog filters, e.g.
 * "Show laptops under Rs 70,000 with 16 GB RAM" -> category=laptops, maxPrice=70000, minRam=16.
 * Deterministic parsing grounds the AI in real data and prevents invented products or prices.
 */
public final class QueryParser {

    private static final Pattern MAX_PRICE = Pattern.compile(
            "(?:under|below|less than|within|upto|up to|max(?:imum)?|cheaper than|<)\\s*(?:rs\\.?|inr|₹)?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)(?:\\s*(k|thousand|lakhs?|l)\\b)?");
    private static final Pattern MIN_PRICE = Pattern.compile(
            "(?:above|over|more than|at least|starting|from|>)\\s*(?:rs\\.?|inr|₹)?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)(?:\\s*(k|thousand|lakhs?|l)\\b)?");
    private static final Pattern RAM = Pattern.compile("([0-9]{1,3})\\s*gb\\s*(?:of\\s*)?(?:ram|memory)");

    private QueryParser() {
    }

    public record ParsedQuery(String category, BigDecimal minPrice, BigDecimal maxPrice, Integer minRam,
                              boolean inStockOnly, boolean programming, boolean comparison) {
    }

    public static ParsedQuery parse(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        return new ParsedQuery(category(q), amount(MIN_PRICE, q), amount(MAX_PRICE, q), ram(q),
                q.contains("available") || q.contains("in stock") || q.contains("in-stock"),
                q.contains("program") || q.contains("coding") || q.contains("developer") || q.contains("software"),
                q.contains("compare") || q.contains(" vs ") || q.contains("versus") || q.contains("difference"));
    }

    static String category(String q) {
        if (q.matches(".*\\b(laptop|laptops|notebook|ultrabook)\\b.*")) {
            return "laptops";
        }
        if (q.matches(".*\\b(phone|phones|mobile|mobiles|smartphone|smartphones)\\b.*")) {
            return "smartphones";
        }
        if (q.matches(".*\\b(monitor|monitors|display|screen)\\b.*")) {
            return "monitors";
        }
        if (q.matches(".*\\b(keyboard|keyboards|headphone|headphones|earphones|accessory|accessories)\\b.*")) {
            return "accessories";
        }
        return null;
    }

    static BigDecimal amount(Pattern pattern, String q) {
        Matcher m = pattern.matcher(q);
        if (!m.find()) {
            return null;
        }
        BigDecimal value = new BigDecimal(m.group(1).replace(",", ""));
        String unit = m.group(2);
        if (unit != null) {
            switch (unit) {
                case "k", "thousand" -> value = value.multiply(BigDecimal.valueOf(1_000));
                case "lakh", "lakhs", "l" -> value = value.multiply(BigDecimal.valueOf(100_000));
                default -> { }
            }
        }
        return value;
    }

    static Integer ram(String q) {
        Matcher m = RAM.matcher(q);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }
}
