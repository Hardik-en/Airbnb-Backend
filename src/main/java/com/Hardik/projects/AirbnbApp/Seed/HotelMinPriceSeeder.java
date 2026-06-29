package com.Hardik.projects.AirbnbApp.Seed;

import com.Hardik.projects.AirbnbApp.entity.Hotel;
import com.Hardik.projects.AirbnbApp.entity.HotelMinPrice;
import com.Hardik.projects.AirbnbApp.entity.Room;
import com.Hardik.projects.AirbnbApp.repository.HotelMinPriceRepository;
import com.Hardik.projects.AirbnbApp.repository.HotelRepository;
import com.Hardik.projects.AirbnbApp.repository.InventoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotelMinPriceSeeder {

    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
    private final HotelMinPriceRepository hotelMinPriceRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void generateHotelMinPrice() {

        if (hotelMinPriceRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);

        List<HotelMinPrice> hotelMinPrices = new ArrayList<>();

        List<Hotel> hotels = hotelRepository.findAll();

        for (Hotel hotel : hotels) {

            LocalDate currentDate = today;

            while (!currentDate.isAfter(endDate)) {

                BigDecimal minPrice =
                        inventoryRepository.findMinPriceByHotelAndDate(
                                hotel.getId(),
                                currentDate
                        );

                hotelMinPrices.add(
                        HotelMinPrice.builder()
                                .hotel(hotel)
                                .date(currentDate)
                                .price(minPrice)
                                .build()
                );

                currentDate = currentDate.plusDays(1);
            }
        }

        hotelMinPriceRepository.saveAll(hotelMinPrices);

        log.info(
                "Generated {} HotelMinPrice records",
                hotelMinPrices.size()
        );
    }
}
