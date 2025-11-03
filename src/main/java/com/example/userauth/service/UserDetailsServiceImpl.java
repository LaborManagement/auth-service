package com.example.userauth.service;

import com.example.userauth.entity.User;
import com.example.userauth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("DEBUG: loadUserByUsername called with: " + username);
        User user = userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> {
                    System.out.println("DEBUG: User not found: " + username);
                    return new UsernameNotFoundException("User Not Found: " + username);
                });
        
        System.out.println("DEBUG: User found: " + user.getUsername() + ", enabled: " + user.isEnabled());
        System.out.println("DEBUG: User password starts with: " + (user.getPassword() != null ? user.getPassword().substring(0, 10) : "null"));
        return user;
    }
    
    /**
     * Load user by ID - more efficient when we have the ID from JWT token
     */
    @Transactional
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        System.out.println("DEBUG: loadUserById called with: " + userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    System.out.println("DEBUG: User not found by ID: " + userId);
                    return new UsernameNotFoundException("User Not Found with ID: " + userId);
                });
        
        System.out.println("DEBUG: User found: " + user.getUsername() + ", enabled: " + user.isEnabled());
        return user;
    }
}
