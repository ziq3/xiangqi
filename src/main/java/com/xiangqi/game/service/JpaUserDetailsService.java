package com.xiangqi.game.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.xiangqi.game.repository.UserRepository;

@Service
public class JpaUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  public JpaUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    var appUser = userRepository.findByUserName(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    return org.springframework.security.core.userdetails.User
        .withUsername(appUser.getUserName())
        .password(appUser.getPassword())
        .roles("USER")
        .build();
  }
}