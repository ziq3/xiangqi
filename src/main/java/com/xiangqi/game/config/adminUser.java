package com.xiangqi.game.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.xiangqi.game.model.User;
import com.xiangqi.game.repository.UserRepository;

@Configuration
public class adminUser {
  @Bean
  CommandLineRunner seedUser(UserRepository repo, PasswordEncoder encoder) {
    return args -> {
      if (!repo.existsByUserName("admin")) {
        User u = new User();
        u.setPlayerId("ADMIN001");
        u.setUserName("admin");
        u.setPassword(encoder.encode("123456"));
        repo.save(u);
      }
    };
  }
}
