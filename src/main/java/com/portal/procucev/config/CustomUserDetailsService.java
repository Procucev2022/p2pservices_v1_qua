package com.portal.procucev.config;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.portal.procucev.dao.UserDao;
import com.portal.procucev.model.User;
@Service
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
    private UserDao userRepository;

    public CustomUserDetailsService(UserDao userRepository) {
        this.userRepository = userRepository;
    }


    public UserDetails loadUserByUsernameAndPhone(String username, String phone) {
        User user = userRepository.findByUsernameAndPhoneAndActive(username, phone, true);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with username and phone");
        }
        else {
        	System.out.println("username ===>"+user.getUsername());
        	System.out.println("phone ===>"+user.getPhone());
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                new ArrayList<>()
        );
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // You may not want to support this if you always use phone + username
    	  // Not used in your flow; throw or implement fallback
        throw new UsernameNotFoundException("Username-only login is not supported. Use phone number too.");
    }

       
}
