package com.example.userauth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.userauth.entity.User;
import com.example.userauth.repository.UserRepository;
import com.shared.utilities.logger.LoggerFactoryProvider;

import org.slf4j.Logger;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private static final Logger log = LoggerFactoryProvider.getLogger(UserDetailsServiceImpl.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("loadUserByUsername called with: {}", username);
        User user = userRepository.findByUsernameOrEmail(username)
                .orElseThrow(() -> {
                    log.debug("User not found: {}", username);
                    return new UsernameNotFoundException("User Not Found: " + username);
                });
        
        log.debug("User found: {}, enabled: {}", user.getUsername(), user.isEnabled());
        String passwordPrefix = null;
        if (user.getPassword() != null) {
            passwordPrefix = user.getPassword().substring(0, Math.min(10, user.getPassword().length()));
        }
        log.debug("User password starts with: {}", passwordPrefix);
        return user;
    }
    
    /**
     * Load user by ID - more efficient when we have the ID from JWT token
     */
    @Transactional
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        log.debug("loadUserById called with: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.debug("User not found by ID: {}", userId);
                    return new UsernameNotFoundException("User Not Found with ID: " + userId);
                });
        
        log.debug("User found: {}, enabled: {}", user.getUsername(), user.isEnabled());
        return user;
    }
}
