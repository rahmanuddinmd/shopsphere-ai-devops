package com.shopsphere.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class QueryParserTest {

    @Test
    void parsesCategoryAndRupeeBudget() {
        var q = QueryParser.parse("Show me laptops under ₹70,000.");
        assertThat(q.category()).isEqualTo("laptops");
        assertThat(q.maxPrice()).isEqualByComparingTo(new BigDecimal("70000"));
        assertThat(q.minRam()).isNull();
    }

    @Test
    void parsesRamAndShorthandBudget() {
        var q = QueryParser.parse("phones with 8 GB RAM below 25k");
        assertThat(q.category()).isEqualTo("smartphones");
        assertThat(q.minRam()).isEqualTo(8);
        assertThat(q.maxPrice()).isEqualByComparingTo(new BigDecimal("25000"));
    }

    @Test
    void detectsProgrammingAndComparisonIntent() {
        assertThat(QueryParser.parse("Which laptop is suitable for programming?").programming()).isTrue();
        assertThat(QueryParser.parse("Compare Nimbus Pro 14 vs Vertex Book 15").comparison()).isTrue();
    }

    @Test
    void detectsAvailabilityAndMobileSynonym() {
        var q = QueryParser.parse("Which mobiles are currently available?");
        assertThat(q.category()).isEqualTo("smartphones");
        assertThat(q.inStockOnly()).isTrue();
    }
}
