package com.goldresto.dto;

import java.math.BigDecimal;

public interface UserPaymentSummary {
    Long getUserId();
    String getFullName();
    String getUsername();
    Long getPaymentCount();
    BigDecimal getTotalAmount();
    Double getPercentageOfTotal();
    Double getPercentageOfMax();
    BigDecimal getGrandTotal();
    BigDecimal getMaxAmount();
}
