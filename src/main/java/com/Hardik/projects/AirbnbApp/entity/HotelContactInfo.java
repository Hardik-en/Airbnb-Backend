package com.Hardik.projects.AirbnbApp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class HotelContactInfo {

    @Column(nullable = false)
    private String completeAddress;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String email;

    private String phoneNumber;


}
