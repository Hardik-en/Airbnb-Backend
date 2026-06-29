package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.dto.HotelDto;
import com.Hardik.projects.AirbnbApp.dto.RoomDto;
import com.Hardik.projects.AirbnbApp.entity.Hotel;
import com.Hardik.projects.AirbnbApp.entity.Inventory;
import com.Hardik.projects.AirbnbApp.entity.Room;
import com.Hardik.projects.AirbnbApp.entity.User;
import com.Hardik.projects.AirbnbApp.exception.ResourceNotFoundException;
import com.Hardik.projects.AirbnbApp.exception.UnAuthorisedException;
import com.Hardik.projects.AirbnbApp.repository.HotelRepository;
import com.Hardik.projects.AirbnbApp.repository.InventoryRepository;
import com.Hardik.projects.AirbnbApp.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.Hardik.projects.AirbnbApp.util.AppUtils.getCurrentUser;
import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService{
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;

    @Override
    public RoomDto createNewRoon(Long hotelId, RoomDto roomDto) {
        log.info("Creating a new room in hotel with id:{}",hotelId);
        Hotel hotel=hotelRepository
                .findById(hotelId)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:"+hotelId));

        User user= (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user is not owner of hotel with id:"+user.getId());
        }

        Room room=modelMapper.map(roomDto, Room.class);
        room.setHotel(hotel);
        room=roomRepository.save(room);

        if (hotel.isActive()){
            inventoryService.InitializeRoomForAYear(room);
        }

        return  modelMapper.map(room,RoomDto.class);
    }

    @Transactional
    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Get all rooms in a hotel with id:{}",hotelId);
        Hotel hotel=hotelRepository
                .findById(hotelId)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:"+hotelId));

        User user= (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user is not owner of hotel with id:"+user.getId());
        }

        return hotel.getRooms()
                .stream()
                .map((element)->modelMapper
                        .map(element,RoomDto.class)).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void DeleteRoomById(Long RoomId) {
        log.info("Delete a room in a hotel with id:{}",RoomId);
        Room room =roomRepository
                .findById(RoomId)
                .orElseThrow(()->new ResourceNotFoundException("Room not found with id:"+RoomId));

        User user= (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!user.equals(room.getHotel().getOwner())){
            throw new UnAuthorisedException("This user is not owner of hotel with id:"+user.getId());
        }

        //Todo : remove all future inventories for this room
        inventoryService.deleteAllInventories(room);
        roomRepository.deleteById(RoomId);
    }

    @Override
    public RoomDto getRoomById(Long RoomId) {
        log.info("Get a room in a hotel with RoomId:{}",RoomId);
       Room room =roomRepository
                .findById(RoomId)
                .orElseThrow(()->new ResourceNotFoundException("Room not found with id:"+RoomId));
       return modelMapper.map(room,RoomDto.class);
    }


    @Override
    @Transactional
    public RoomDto updateRoomById(RoomDto roomDto, Long roomId) {

        log.info("Updating room with id: {}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Room not found with id: " + roomId));

        User user = getCurrentUser();

        if (!user.getId().equals(room.getHotel().getOwner().getId())) {
            throw new UnAuthorisedException(
                    "This user is not owner of hotel with id: " + user.getId());
        }

        // Store old values before updating
        BigDecimal oldBasePrice = room.getBasePrice();
        Integer oldTotalCount = room.getTotalCount();

        // Update room
        modelMapper.map(roomDto, room);
        room.setId(roomId);

        boolean priceChanged =
                !oldBasePrice.equals(room.getBasePrice());

        boolean inventoryChanged =
                !oldTotalCount.equals(room.getTotalCount());

        if (priceChanged || inventoryChanged) {

            List<Inventory> inventories =
                    inventoryRepository.findFutureInventoryByRoomId(roomId);
            for (Inventory inventory : inventories) {

                if (inventoryChanged) {
                    inventory.setTotalCount(room.getTotalCount());
                }

                if (priceChanged) {
                    inventory.setPrice(
                            room.getBasePrice()
                                    .multiply(inventory.getSurgeFactor())
                    );
                }
            }
            inventoryRepository.saveAll(inventories);
        }
        room = roomRepository.save(room);

        return modelMapper.map(room, RoomDto.class);
    }
}
