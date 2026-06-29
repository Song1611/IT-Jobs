package com.itjob.service;

import com.itjob.entity.User;

public interface JwtService {

    String generateAccessToken(User user);

    boolean validateToken(String token);

    String extractUsername(String token);
}
