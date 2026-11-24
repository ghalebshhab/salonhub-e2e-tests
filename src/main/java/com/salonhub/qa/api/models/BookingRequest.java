package com.salonhub.qa.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Body of {@code POST /api/bookings}. Dates are ISO strings ({@code 2026-12-01},
 * {@code 10:00:00}) so no Jackson time module is needed. A null
 * {@code paymentMethod} means the salon's default (cash).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingRequest(Long salonId, List<Long> serviceIds, Long assignedEmployeeId,
                             String customerPhoneNumber, String customerLocation,
                             String bookingDate, String bookingTime, String note,
                             String paymentMethod) {

    public BookingRequest(Long salonId, List<Long> serviceIds, Long assignedEmployeeId,
                          String customerPhoneNumber, String customerLocation,
                          String bookingDate, String bookingTime, String note) {
        this(salonId, serviceIds, assignedEmployeeId, customerPhoneNumber, customerLocation,
                bookingDate, bookingTime, note, null);
    }

    public BookingRequest withDate(String date) {
        return new BookingRequest(salonId, serviceIds, assignedEmployeeId, customerPhoneNumber,
                customerLocation, date, bookingTime, note, paymentMethod);
    }

    public BookingRequest withTime(String time) {
        return new BookingRequest(salonId, serviceIds, assignedEmployeeId, customerPhoneNumber,
                customerLocation, bookingDate, time, note, paymentMethod);
    }

    public BookingRequest withServices(List<Long> ids) {
        return new BookingRequest(salonId, ids, assignedEmployeeId, customerPhoneNumber,
                customerLocation, bookingDate, bookingTime, note, paymentMethod);
    }

    public BookingRequest withLocation(String location) {
        return new BookingRequest(salonId, serviceIds, assignedEmployeeId, customerPhoneNumber,
                location, bookingDate, bookingTime, note, paymentMethod);
    }

    public BookingRequest withPaymentMethod(String method) {
        return new BookingRequest(salonId, serviceIds, assignedEmployeeId, customerPhoneNumber,
                customerLocation, bookingDate, bookingTime, note, method);
    }
}
