package com.edu.hibernateintergration.service;

import com.edu.hibernateintergration.model.User;

import java.util.List;


public interface UserService {

    User findById(int id);
     
    User findByUsername(String userName);

    void saveUser(User user);

    void updateUser(User user);

    void deleteUserByUsername(String username);

    List<User> findAllUsers();

    boolean isUserUsernameUnique(Integer id, String username);
}
