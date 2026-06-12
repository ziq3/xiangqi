package com.xiangqi.game.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xiangqi.game.model.Room;

import jakarta.persistence.LockModeType;

public interface RoomRepository extends JpaRepository<Room, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.roomId = :roomId")
    Optional<Room> findByRoomIdForUpdate(@Param("roomId") String roomId);

    @Query("select r from Room r where (r.hostId = :userId or r.guestId = :userId) and r.status = com.xiangqi.game.model.Room$Status.FINISHED")
    List<Room> findRoomByUserId(@Param("userId") String userId);

}
