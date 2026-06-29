package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.dto.HotelDto;
import com.Hardik.projects.AirbnbApp.dto.HotelInfoDto;
import com.Hardik.projects.AirbnbApp.dto.RoomDto;
import com.Hardik.projects.AirbnbApp.entity.Hotel;
import com.Hardik.projects.AirbnbApp.entity.Room;
import com.Hardik.projects.AirbnbApp.entity.User;
import com.Hardik.projects.AirbnbApp.exception.ResourceNotFoundException;
import com.Hardik.projects.AirbnbApp.exception.UnAuthorisedException;
import com.Hardik.projects.AirbnbApp.repository.HotelRepository;
import com.Hardik.projects.AirbnbApp.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.Hardik.projects.AirbnbApp.util.AppUtils.getCurrentUser;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService{
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;
    private final RoomRepository roomRepository;

    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        log.info("creating a new hotel with name{}:",hotelDto.getName());
        Hotel newHotel=modelMapper.map(hotelDto,Hotel.class);

        User user= (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        newHotel.setOwner(user);

        newHotel=hotelRepository.save(newHotel);
        log.info("created a new hotel with id{}:",hotelDto.getId());
        return modelMapper.map(newHotel,HotelDto.class);
    }


    @Override
    public List<HotelDto> getAllHotels() {
        log.info("Get all hotel");

        User user=getCurrentUser();
        List<Hotel> hotels=hotelRepository.findByOwner(user);

        return hotels.stream().map((element)->modelMapper.map(element,HotelDto.class)).collect(Collectors.toList());

    }

    @Override
    public HotelDto getHotelById(Long id) {
        log.info("Get a hotel with id{}:",id);
        Hotel hotel=hotelRepository
                .findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:"+id));

        User user= (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user is not owner of hotel with id:"+user.getId());
        }

        return modelMapper.map(hotel,HotelDto.class);
    }

    @Override
    public HotelDto updateHotelById(Long id,HotelDto hotelDto) {
        log.info("Update a hotel with id{}:",id);
        Hotel hotel=hotelRepository
                .findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:"+id));

        User user= (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user is not owner of hotel with id:"+user.getId());
        }

        modelMapper.map(hotelDto,hotel);
        hotel.setId(id);
        hotel=hotelRepository.save(hotel);
        return modelMapper.map(hotel,HotelDto.class);
    }

    @Override
    @Transactional
    public void deleteHotelById(Long id) {
        log.info("Delete the hotel with Id:{}",id);
        Hotel hotel=hotelRepository
                .findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:"+id));
        //Todo : delete the future inventories
        for (Room room: hotel.getRooms()){
            inventoryService.deleteAllInventories(room);
            roomRepository.deleteById(room.getId());
        }

        User user= (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user is not owner of hotel with id:"+user.getId());
        }

        hotelRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void activateHotel(Long id) {
        log.info("Activating the hotel with Id:{}",id);
        Hotel hotel=hotelRepository
                .findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:"+id));
        hotel.setActive(true);

        User user= (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!user.equals(hotel.getOwner())){
            throw new UnAuthorisedException("This user is not owner of hotel with id:"+user.getId());
        }

        // assuming only do it once
        for (Room room:hotel.getRooms()){
            inventoryService.InitializeRoomForAYear(room);
        }
    }

    @Override
    public HotelInfoDto getHotelInfoById(Long hotelId) {
        Hotel hotel=hotelRepository
                .findById(hotelId)
                .orElseThrow(()->new ResourceNotFoundException("Hotel not found with id:"+hotelId));

        List<RoomDto> rooms=hotel.getRooms()
                .stream()
                .map((element)->modelMapper.map(element,RoomDto.class)).toList();

        return new HotelInfoDto(modelMapper.map(hotel,HotelDto.class),rooms);
    }
}
