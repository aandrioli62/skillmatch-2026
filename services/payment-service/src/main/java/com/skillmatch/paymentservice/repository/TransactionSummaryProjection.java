package com.skillmatch.paymentservice.repository;

import java.math.BigDecimal;

/** Aggregate totals for a (possibly filtered) set of transactions — the "earnings" side of admin oversight. */
public interface TransactionSummaryProjection {

    BigDecimal getTotalVolume();

    BigDecimal getTotalCommission();

    BigDecimal getTotalNet();

    Long getCount();
}
