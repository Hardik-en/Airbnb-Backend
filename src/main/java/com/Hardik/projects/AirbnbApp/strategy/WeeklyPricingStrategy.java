package com.Hardik.projects.AirbnbApp.strategy;

import com.Hardik.projects.AirbnbApp.entity.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.time.DayOfWeek;

@RequiredArgsConstructor
public class WeeklyPricingStrategy implements PricingStrategy {
    @Qualifier("basePricingStrategy")
    private final PricingStrategy wrapped;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);

        DayOfWeek day = inventory.getDate().getDayOfWeek();

        if(day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY){
            price = price.multiply(BigDecimal.valueOf(1.20));
        }

        return price;
    }
}
