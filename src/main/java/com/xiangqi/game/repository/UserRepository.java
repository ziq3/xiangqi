package com.xiangqi.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.xiangqi.game.model.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUserName(String userName);
    boolean existsByUserName(String userName);
}
