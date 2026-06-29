package com.Hardik.projects.AirbnbApp.service;

import com.Hardik.projects.AirbnbApp.entity.Booking;

public interface PaymentCheckOutService {

    String getCheckOutSession(Booking booking, String successUrl, String failureUrl);
}
