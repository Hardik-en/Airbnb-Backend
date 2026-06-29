package com.Hardik.projects.AirbnbApp.strategy;

import com.Hardik.projects.AirbnbApp.entity.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import java.math.BigDecimal;


@RequiredArgsConstructor
public class HolidayPricingStrategy implements PricingStrategy{
    @Qualifier("basePricingStrategy")
    private final PricingStrategy wrapped;
    private final HolidayService holidayService;


    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
         BigDecimal price=wrapped.calculatePrice(inventory);
         boolean isTodayHoliday=holidayService.isHoliday(inventory.getDate(),"IN"); //Use api to check
         if (isTodayHoliday){
             price=price.multiply(BigDecimal.valueOf(1.25));
         }
         return price;
    }
}
