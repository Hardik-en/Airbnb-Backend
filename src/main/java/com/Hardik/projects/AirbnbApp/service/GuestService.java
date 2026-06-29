package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.dto.GuestDto;

import java.util.List;

public interface GuestService {
    GuestDto addNewGuest(GuestDto guestDto);

    List<GuestDto> getAllGuests();

    void updateGuest(Long guestId, GuestDto guestDto);

    void deleteGuest(Long guestId);
}
