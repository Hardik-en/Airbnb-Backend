package com.Hardik.projects.AirbnbApp.strategy;

import com.Hardik.projects.AirbnbApp.entity.Inventory;
import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal calculatePrice(Inventory inventory);
}
