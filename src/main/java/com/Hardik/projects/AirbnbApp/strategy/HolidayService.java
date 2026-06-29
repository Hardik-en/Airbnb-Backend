package com.Hardik.projects.AirbnbApp.strategy;

import com.Hardik.projects.AirbnbApp.dto.HolidayDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private final RestClient restClient;

    public boolean isHoliday(LocalDate date, String countryCode) {

        HolidayDto[] holidays = restClient.get()
                .uri("https://date.nager.at/api/v3/PublicHolidays/{year}/{country}",
                        date.getYear(), countryCode)
                .retrieve()
                .body(HolidayDto[].class);

        if (holidays == null) {
            return false;
        }

        return Arrays.stream(holidays)
                .anyMatch(h -> h.getDate().equals(date));
    }
}