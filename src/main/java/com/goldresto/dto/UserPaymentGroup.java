package com.goldresto.dto;

import com.goldresto.entity.User;
import com.goldresto.entity.Paiement;
import java.math.BigDecimal;
import java.util.List;

public class UserPaymentGroup {
    private User user;
    private List<Paiement> payments;
    private long count;
    private BigDecimal total;

    public UserPaymentGroup(User user, List<Paiement> payments) {
        this.user = user;
        this.payments = payments;
        this.count = payments.size();
        this.total = payments.stream()
            .map(p -> p.getMontantAPayer() != null ? p.getMontantAPayer() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public User getUser() {
        return user;
    }

    public List<Paiement> getPayments() {
        return payments;
    }

    public long getCount() {
        return count;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
