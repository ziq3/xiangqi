package com.xiangqi.game.service;

import java.security.SecureRandom;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xiangqi.game.model.Room;
import com.xiangqi.game.model.Room.Status;
import com.xiangqi.game.model.Room.Turn;
import com.xiangqi.game.repository.RoomRepository;

@Service
public class RoomService {
  private static final String ROOM_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int ROOM_ID_LENGTH = 8;

  private final SecureRandom random = new SecureRandom();
  private final RoomRepository roomRepository;

  public RoomService(RoomRepository roomRepository) {
    this.roomRepository = roomRepository;
  }

  @Transactional
  public Room createRoom(String hostName) {

    Room room = new Room();
    room.setRoomId(generateRoomId());
    room.setHostName(hostName);

    return roomRepository.save(room);
  }

  @Transactional
  public Room joinRoom(String roomId, String guestName) {
    Room room = getRoomForUpdate(roomId);

    if (!room.canJoin()) {
      throw new IllegalStateException("Room is full");
    }
    if (!guestName.equals(room.getHostName())) {
      room.setGuestName(guestName);
      room.setStatus(Status.PLAYING);
    }
    return room;
  }

  @Transactional
  public Room applyMove(String roomId,String move) {

    Room room = getRoomForUpdate(roomId);

    boolean hostTurn = room.getTurn() == Turn.HOST;
    String history = room.getMoveHistory();
    room.setMoveHistory(history.isBlank() ? move : history + " " + move);
    room.setTurn(hostTurn ? Turn.GUEST : Turn.HOST);
    return room;
  }

  @Transactional(readOnly = true)
  public Room getRoom(String roomId) {
    return roomRepository.findById(normalizeRoomId(roomId))
        .orElseThrow(() -> new IllegalStateException("Room not found"));
  }

  private Room getRoomForUpdate(String roomId) {
    return roomRepository.findByRoomIdForUpdate(normalizeRoomId(roomId))
        .orElseThrow(() -> new IllegalStateException("Room not found"));
  }

  private String normalizeRoomId(String roomId) {
    if (roomId == null || roomId.isBlank()) {
      throw new IllegalStateException("Room ID is required");
    }
    return roomId.toUpperCase(Locale.ROOT);
  }

  private String generateRoomId() {
    String id;
    do {
      StringBuilder sb = new StringBuilder(ROOM_ID_LENGTH);
      for (int i = 0; i < ROOM_ID_LENGTH; i++) {
        int idx = random.nextInt(ROOM_ID_CHARS.length());
        sb.append(ROOM_ID_CHARS.charAt(idx));
      }
      id = sb.toString();
    } while (roomRepository.existsById(id));
    return id;
  }
}