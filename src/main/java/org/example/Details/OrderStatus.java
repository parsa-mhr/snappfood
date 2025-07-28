package org.example.Details;

public enum OrderStatus {
    SUBMITTED,//
    WAITING_VENDOR,
    WAITING_RESTAURANT,
    CANCELLED,//کنسل رستوران
    FINDING_COURIER,
    ON_THE_WAY,//تحویل
    COMPLETED,//تحویل نهایی
    ACCEPTED ,//تایید رستوران
    COURIER_ACCEPTED// قبول کردن
}
