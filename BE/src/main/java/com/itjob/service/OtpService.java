package com.itjob.service;

public interface OtpService {

    String generateAndStore(String email);

    boolean verify(String email, String otp);

    void delete(String email);
}
