package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.dto.HotelPriceDto;
import com.Hardik.projects.AirbnbApp.entity.Hotel;
import com.Hardik.projects.AirbnbApp.entity.HotelMinPrice;
import com.Hardik.projects.AirbnbApp.entity.Inventory;
import com.Hardik.projects.AirbnbApp.repository.HotelMinPriceRepository;
import com.Hardik.projects.AirbnbApp.repository.HotelRepository;
import com.Hardik.projects.AirbnbApp.repository.InventoryRepository;
import com.Hardik.projects.AirbnbApp.strategy.PricingService;
import jakarta.persistence.ManyToOne;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingUpdateService {
    //Schedular to update the inventory and hotelMinPrice tables every hour
    private final HotelMinPriceRepository hotelMinPriceRepository;
    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;
    private final PricingService pricingService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void updatePrice(){
        int page=0;
        int batchSize=100;

        while (true){
            Page<Hotel> hotelPage=hotelRepository.findAll(PageRequest.of(page,batchSize));
            if (hotelPage.isEmpty()){
                break;
            }
            hotelPage.getContent().forEach(this::updateHotelPrices);
            page++;
        }
    }

    @Transactional
    public void updateHotelPrices(Hotel hotel){

        log.info("Updating hotel prices");

        LocalDate startDate=LocalDate.now();
        LocalDate endDate=LocalDate.now().plusYears(1);

        List<Inventory> inventoryList=inventoryRepository.findByHotelAndDateBetween(hotel,startDate,endDate);
        updateInventoryPrices(inventoryList);

        updateHotelMinPrice(hotel,inventoryList,startDate,endDate);
    }

    @Transactional
    public void updateHotelMinPrice(Hotel hotel, List<Inventory> inventoryList, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate,BigDecimal> dailyMinPrices=inventoryList.stream()
                .collect(Collectors.groupingBy(
                        Inventory::getDate,
                        Collectors.mapping(Inventory::getPrice,Collectors.minBy(Comparator.naturalOrder()))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,e->e.getValue().orElse(BigDecimal.ZERO)));

        List<HotelMinPrice> hotelPrices=new ArrayList<>();
        dailyMinPrices.forEach((date,price)->{
            HotelMinPrice hotelMinPrice=hotelMinPriceRepository.findByHotelAndDate(hotel,date)
                    .orElse(new HotelMinPrice(hotel,date));
            hotelMinPrice.setPrice(price);
            hotelPrices.add(hotelMinPrice);
        });

        hotelMinPriceRepository.saveAll(hotelPrices);
    }

    @Transactional
    public void updateInventoryPrices(List<Inventory>inventoryList){
        inventoryList.forEach(inventory ->{
            BigDecimal dynamicPrice=pricingService.calculateDynamicPricing(inventory);
            inventory.setPrice(dynamicPrice);

            log.info("Inventory Date: {}, Old Price: {}, New Price: {}",
                    inventory.getDate(),
                    inventory.getPrice(),
                    dynamicPrice);

        });
        inventoryRepository.saveAll(inventoryList);
    }
}
