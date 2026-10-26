package com.krittish.dailytracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.krittish.dailytracker.sms.SmsParser;

import org.junit.Test;

/** Add 30+ of YOUR real messages here (hide names and numbers) and tune the regexes until 90%+ pass. */
public class SmsParserTest {

    @Test
    public void upiDebit() {
        SmsParser.Parsed p = SmsParser.parse("Rs.180.00 debited from A/c XX1234 on 20-10-26. Info: UPI/Zomato/123456789012");
        assertNull(p.ignoreReason);
        assertEquals("EXPENSE", p.type);
        assertEquals(18000L, p.amountPaise);
        assertEquals("Zomato", p.merchant);
        assertEquals("123456789012", p.refNo);
        assertEquals("1234", p.account);
    }

    @Test
    public void upiCredit() {
        SmsParser.Parsed p = SmsParser.parse("Your a/c XX1234 is credited with INR 500.00 on 20-Oct-26 by UPI Ref 123456789012");
        assertEquals("INCOME", p.type);
        assertEquals(50000L, p.amountPaise);
        assertEquals("123456789012", p.refNo);
    }

    @Test
    public void cardSpend() {
        SmsParser.Parsed p = SmsParser.parse("INR 250.00 spent on Card XX5678 at AMAZON on 20-10-26");
        assertEquals("EXPENSE", p.type);
        assertEquals(25000L, p.amountPaise);
        assertEquals("AMAZON", p.merchant);
        assertEquals("5678", p.account);
    }

    @Test
    public void otpIsIgnored() {
        assertEquals("OTP", SmsParser.parse("123456 is your OTP. Do not share it with anyone").ignoreReason);
    }

    @Test
    public void balanceAmountIsNotUsedAsTheTransactionAmount() {
        SmsParser.Parsed p = SmsParser.parse("Rs.1,250.50 debited from A/c XX1234 to VPA zomato@icici on 20-10-26. Avl Bal Rs.9,000.00");
        assertEquals(125050L, p.amountPaise);
        assertEquals("zomato", p.merchant);
    }

    @Test
    public void promoIsIgnored() {
        assertNotNull(SmsParser.parse("Get a pre-approved loan of Rs 5,00,000. Apply now").ignoreReason);
    }
}
