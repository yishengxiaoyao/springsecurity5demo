package com.edu.hibernateintergration.service.impl;

import java.util.List;

import com.edu.hibernateintergration.dao.UserProfileDao;
import com.edu.hibernateintergration.model.UserProfile;
import com.edu.hibernateintergration.service.UserProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 

 
@Service("userProfileService")
@Transactional
public class UserProfileServiceImpl implements UserProfileService {
     
    @Autowired
    UserProfileDao dao;
     
    public UserProfile findById(int id) {
        return dao.findById(id);
    }
 
    public UserProfile findByType(String type){
        return dao.findByType(type);
    }
 
    public List<UserProfile> findAll() {
        return dao.findAll();
    }
}