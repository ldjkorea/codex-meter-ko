package dev.bennett.codexmeter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Currency;

/** User-entered charge. No token counts or monetary savings are inferred from quota percentages. */
public final class SubscriptionCost {
    public final BigDecimal amount;
    public final String currency;
    public final LocalDate start,end;
    public SubscriptionCost(String amount,String currency,LocalDate start,LocalDate end){
        this.amount=new BigDecimal(amount);this.currency=currency;
        Currency.getInstance(currency);
        long days=ChronoUnit.DAYS.between(start,end);
        if(this.amount.signum()<0||this.amount.compareTo(new BigDecimal("1000000000"))>0||this.amount.scale()>4||days<1||days>400)
            throw new IllegalArgumentException("Invalid billing period or amount");
        this.start=start;this.end=end;
    }
    public boolean active(LocalDate today){return !today.isBefore(start)&&today.isBefore(end);}
    /** This release does not have authenticated token/cost data for the billing period. */
    public boolean monetaryComparisonAvailable(){return false;}
}
