package com.krittish.dailytracker;

import static org.junit.Assert.assertEquals;

import com.krittish.dailytracker.util.MoneyFormat;

import org.junit.Test;

public class MoneyFormatTest {
    @Test
    public void formatsIndianGroupingAndPaise() {
        assertEquals("₹180", MoneyFormat.format(18000));
        assertEquals("₹180.50", MoneyFormat.format(18050));
        assertEquals("₹1,50,000", MoneyFormat.format(15000000));
        assertEquals("₹12,34,567", MoneyFormat.format(123456700));
    }

    @Test
    public void parsesToPaise() {
        assertEquals(18050L, MoneyFormat.parseToPaise("180.5"));
        assertEquals(-1L, MoneyFormat.parseToPaise("abc"));
    }
}
