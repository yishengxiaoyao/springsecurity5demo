package com.edu.hibernateintergration.dao;


import com.edu.hibernateintergration.model.User;

import java.util.List;

public interface UserDao {
 
    User findById(int id);
     
    User findByUsername(String userName);

    void save(User user);

    void deleteByUsername(String username);

    List<User> findAllUsers();
     
}