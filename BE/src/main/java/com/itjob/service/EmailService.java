package com.itjob.service;

public interface EmailService {

    void sendVerifyEmail(String email, String otp);

    void sendForgotPasswordOtp(String email, String otp);

    void sendChangePasswordOtp(String email, String otp);
}
