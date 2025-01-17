package org.signal.core.util.money;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class FiatMoney {
  private final BigDecimal amount;
  private final Currency   currency;
  private final long       timestamp;

  public FiatMoney(@NonNull BigDecimal amount, @NonNull Currency currency) {
    this(amount, currency, 0);
  }

  public FiatMoney(@NonNull BigDecimal amount, @NonNull Currency currency, long timestamp) {
    this.amount    = amount;
    this.currency  = currency;
    this.timestamp = timestamp;
  }

  public @NonNull BigDecimal getAmount() {
    return amount;
  }

  public @NonNull Currency getCurrency() {
    return currency;
  }

  public long getTimestamp() {
    return timestamp;
  }

  /**
   * @return amount, rounded to the default fractional amount.
   */
  public @NonNull String getDefaultPrecisionString() {
    return getDefaultPrecisionString(Locale.getDefault());
  }

  /**
   * @return amount, rounded to the default fractional amount.
   */
  public @NonNull String getDefaultPrecisionString(@NonNull Locale locale) {
    NumberFormat formatter = NumberFormat.getInstance(locale);
    formatter.setMinimumFractionDigits(currency.getDefaultFractionDigits());
    formatter.setGroupingUsed(false);

    return formatter.format(amount);
  }

  /**
   * Note: This special cases UGX to act as two decimal.
   *
   * @return amount, in smallest possible units (cents, yen, etc.)
   */
  public @NonNull String getMinimumUnitPrecisionString() {
    NumberFormat formatter = NumberFormat.getInstance();
    formatter.setMaximumFractionDigits(0);
    formatter.setGroupingUsed(false);
    
    BigDecimal multiplicand = BigDecimal.TEN.pow(currency.getCurrencyCode().equals("UGX") ? 2 : currency.getDefaultFractionDigits());

    return formatter.format(amount.multiply(multiplicand));
  }
}
