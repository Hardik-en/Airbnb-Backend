package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.dto.*;
import com.Hardik.projects.AirbnbApp.entity.Inventory;
import com.Hardik.projects.AirbnbApp.entity.Room;
import com.Hardik.projects.AirbnbApp.entity.User;
import com.Hardik.projects.AirbnbApp.exception.ResourceNotFoundException;
import com.Hardik.projects.AirbnbApp.repository.InventoryRepository;
import com.Hardik.projects.AirbnbApp.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static com.Hardik.projects.AirbnbApp.util.AppUtils.getCurrentUser;

@Slf4j
@RequiredArgsConstructor
@Service
public class InventoryServiceImpl implements InventoryService{
    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;
    private final RoomRepository roomRepository;

    @Override
    public void InitializeRoomForAYear(Room room) {
        LocalDate today=LocalDate.now();
        LocalDate endDate=today.plusYears(1);

        for (;!today.isAfter(endDate);today=today.plusDays(1)){
            Inventory inventory=Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .reservedCount(0)
                    .city(room.getHotel().getCity())
                    .date(today)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventoryRepository.save(inventory);
        }
    }

    @Transactional
    @Override
    public void deleteAllInventories(Room room) {
       inventoryRepository.deleteByRoom(room);
    }

    @Override
    public Page<HotelPriceDto> searchHotels(HotelSearchRequestDto hotelSearchRequestDto) {
        Pageable pageable= PageRequest.of(hotelSearchRequestDto.getPage(),hotelSearchRequestDto.getSize());
        Long dateCount = ChronoUnit.DAYS.between(
                hotelSearchRequestDto.getStartDate(),
                hotelSearchRequestDto.getEndDate()
        ) + 1;

        Page<HotelPriceDto> hotelPage=inventoryRepository.searchAvailableHotelPrices(
                hotelSearchRequestDto.getCity().trim(),
                hotelSearchRequestDto.getStartDate(),
                hotelSearchRequestDto.getEndDate(),
                hotelSearchRequestDto.getRoomsCount(),
                dateCount,
                pageable
        );

        return hotelPage;
    }

    @Override
    public List<InventoryDto> getAllInventoriesByRoom(Long roomId) {
        log.info("Gettng all inventory for the room with id:{}",roomId);
         Room room=roomRepository.findById(roomId)
                 .orElseThrow(()->new ResourceNotFoundException("Inventory doe not exist wth this room id:"+roomId));

         User user=getCurrentUser();

        if (!user.getId().equals(room.getHotel().getOwner().getId())) {
            throw new AccessDeniedException(
                    "You are not the owner of room with id: " + roomId);
        }

        return inventoryRepository.findByRoomOrderByDate(room).stream()
                .map((element)->modelMapper.map(element,
                        InventoryDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateInventory(Long roomId, UpdateInventoryDto updateInventoryDto) {
        log.info("Updating inventory by room for room with id:{} between date range:{} - {}",roomId);
        Room room=roomRepository.findById(roomId)
                .orElseThrow(()->new ResourceNotFoundException("Inventory doe not exist wth this room id:"+roomId));

        User user= getCurrentUser();
        if (!user.getId().equals(room.getHotel().getOwner().getId())) {
            throw new AccessDeniedException(
                    "You are not the owner of room with id: " + roomId);
        }

        inventoryRepository.findInventoryAndLockBeforeUpdate(roomId,updateInventoryDto.getStartDate(),updateInventoryDto.getEndDate());

        inventoryRepository.updateInventory(roomId,updateInventoryDto.getStartDate(),updateInventoryDto.getEndDate()
                ,updateInventoryDto.getClosed(),updateInventoryDto.getSurgeFactor());


    }
}
