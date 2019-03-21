package com.edu.hibernateintergration.service;

import java.util.List;

import com.edu.hibernateintergration.model.UserProfile;

 
public interface UserProfileService {
 
    UserProfile findById(int id);
 
    UserProfile findByType(String type);
     
    List<UserProfile> findAll();
     
}