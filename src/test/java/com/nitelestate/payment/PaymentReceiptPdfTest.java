package com.nitelestate.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** The receipt is the only thing a resident keeps after paying, so the amount it spells out has to be right. */
class PaymentReceiptPdfTest {

    @Test
    void spellsOutWholeNairaAmounts() {
        assertThat(AmountInWords.spell(new BigDecimal("240000.00")))
                .isEqualTo("Two Hundred and Forty Thousand Naira Only");
        assertThat(AmountInWords.spell(new BigDecimal("1000000"))).isEqualTo("One Million Naira Only");
        assertThat(AmountInWords.spell(new BigDecimal("15"))).isEqualTo("Fifteen Naira Only");
        assertThat(AmountInWords.spell(new BigDecimal("305"))).isEqualTo("Three Hundred and Five Naira Only");
        assertThat(AmountInWords.spell(new BigDecimal("42"))).isEqualTo("Forty-Two Naira Only");
        assertThat(AmountInWords.spell(BigDecimal.ZERO)).isEqualTo("Zero Naira Only");
    }

    @Test
    void spellsOutKoboWhenPresent() {
        assertThat(AmountInWords.spell(new BigDecimal("1500.50")))
                .isEqualTo("One Thousand Five Hundred Naira and Fifty Kobo Only");
    }

    @Test
    void rendersAPdfWithTheEstateLetterhead() {
        byte[] pdf = PaymentReceiptPdf.render("Mike Iyamu", "NECDA-PYMT-RCPT-1-1", "Development levy",
                new BigDecimal("240000.00"), "BANK_TRANSFER", Instant.parse("2026-10-03T10:15:00Z"),
                "12", "12, 2nd Avenue, Nitel Estate Ikorodu Lagos");

        assertThat(pdf).startsWith('%', 'P', 'D', 'F');
        // The watermark/letterhead logo is the bulk of the file - a receipt rendered without it
        // (missing classpath resource) comes out an order of magnitude smaller.
        assertThat(pdf.length).isGreaterThan(50_000);
    }

    @Test
    void rendersWithoutOptionalPropertyOrLevyDetails() {
        byte[] pdf = PaymentReceiptPdf.render("Jane Tenant", "NECDA-PYMT-RCPT-2-2", null,
                new BigDecimal("5000"), "CASH", Instant.parse("2026-10-03T10:15:00Z"), null, null);

        assertThat(pdf).startsWith('%', 'P', 'D', 'F');
    }
}
