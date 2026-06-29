package com.Hardik.projects.AirbnbApp.strategy;

import com.Hardik.projects.AirbnbApp.entity.Inventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final HolidayService holidayService;

    public BigDecimal calculateDynamicPricing(Inventory inventory){
        PricingStrategy pricingStrategy=new BasePricingStrategy();


        pricingStrategy=new SurgePriceStrategy(pricingStrategy);
        pricingStrategy=new OccupancyPricingStrategy(pricingStrategy);
        pricingStrategy=new UrgencyPricingStrategy(pricingStrategy);
        pricingStrategy=new HolidayPricingStrategy(pricingStrategy,holidayService);
        pricingStrategy=new WeeklyPricingStrategy(pricingStrategy);

        return pricingStrategy.calculatePrice(inventory);
    }

    public BigDecimal calculateTotalPrice(List<Inventory>inventoryList){
        BigDecimal totalPrice=inventoryList.stream()
                .map(inventory -> calculateDynamicPricing(inventory))
                .reduce(BigDecimal.ZERO,BigDecimal::add);

        return totalPrice;
    }
}
