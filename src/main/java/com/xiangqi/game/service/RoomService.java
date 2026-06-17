package com.xiangqi.game.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

import com.xiangqi.game.model.Room.EndReason;

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
  private final EngineService engineService;

  public RoomService(RoomRepository roomRepository, EngineService engineService) {
    this.roomRepository = roomRepository;
    this.engineService = engineService;
  }

  public record ClockView(long hostRemainingMs, long guestRemainingMs) {
  }

  private long nowMs() {
    return System.currentTimeMillis();
  }

  private void ensureClockStarted(Room room, long nowMs) {
    if (room.getStatus() != Status.PLAYING) {
      return;
    }
    if (room.getTurnStartedAtEpochMs() == 0L) {
      room.setTurnStartedAtEpochMs(nowMs);
    }
    room.setEndReason(null);
  }

  private ClockView computeClockView(Room room, long nowMs) {
    long hostMs = room.getHostRemainingMs();
    long guestMs = room.getGuestRemainingMs();
    if (room.getStatus() != Status.PLAYING) {
      return new ClockView(hostMs, guestMs);
    }
    long startedAt = room.getTurnStartedAtEpochMs();
    if (startedAt <= 0L) {
      return new ClockView(hostMs, guestMs);
    }

    long elapsed = Math.max(0L, nowMs - startedAt);
    if (room.getTurn() == Turn.HOST) {
      hostMs = hostMs - elapsed;
    } else {
      guestMs = guestMs - elapsed;
    }
    return new ClockView(Math.max(0L, hostMs), Math.max(0L, guestMs));
  }

  public ClockView computeClockView(Room room) {
    return computeClockView(room, nowMs());
  }

  private boolean isTimedOut(ClockView view, Turn turnToMove) {
    return turnToMove == Turn.HOST ? view.hostRemainingMs() <= 0L : view.guestRemainingMs() <= 0L;
  }

  private void finalizeTimeout(Room room, Turn timedOutSide) {
    room.setStatus(Status.FINISHED);
    room.setEndReason(timedOutSide == Turn.HOST ? EndReason.TIMEOUT_HOST : EndReason.TIMEOUT_GUEST);
    room.setTurnStartedAtEpochMs(0L);
    if (timedOutSide == Turn.HOST) {
      room.setHostRemainingMs(0L);
    } else {
      room.setGuestRemainingMs(0L);
    }
  }

  private void applyElapsedForTurn(Room room, long nowMs) {
    if (room.getStatus() != Status.PLAYING) {
      return;
    }
    long startedAt = room.getTurnStartedAtEpochMs();
    if (startedAt <= 0L) {
      room.setTurnStartedAtEpochMs(nowMs);
      return;
    }
    ClockView view = computeClockView(room, nowMs);
    if (isTimedOut(view, room.getTurn())) {
      finalizeTimeout(room, room.getTurn());
      return;
    }

    room.setHostRemainingMs(view.hostRemainingMs());
    room.setGuestRemainingMs(view.guestRemainingMs());
    room.setTurnStartedAtEpochMs(nowMs);
  }

  private void addIncrement(Room room, Turn sideThatMoved) {
    long inc = Math.max(0L, room.getClockIncrementMs());
    if (sideThatMoved == Turn.HOST) {
      room.setHostRemainingMs(room.getHostRemainingMs() + inc);
    } else {
      room.setGuestRemainingMs(room.getGuestRemainingMs() + inc);
    }
  }

  @Transactional
  public Room createRoom(String hostName, String id) {

    Room room = new Room();
    room.setRoomId(generateRoomId());
    room.setHostName(hostName);
    room.setHostId(id);
    return roomRepository.save(room);
  }

  @Transactional
  public Room joinRoom(String roomId, String guestName, String id) {
    Room room = getRoomForUpdate(roomId);

    if (!room.canJoin()) {
      throw new IllegalStateException("Room is full");
    }
    if (id == null || !id.equals(room.getHostId())) {
      room.setGuestName(guestName);
      if (id != null) {
        room.setGuestId(id);
      }
      room.setStatus(Status.PLAYING);
      ensureClockStarted(room, nowMs());
    }
    return room;
  }

  @Transactional
  public Room startRoom(String roomId) {
    Room room = getRoomForUpdate(roomId);
    if (room.getStatus() == Status.WAITING) {
      if (room.getGuestName() == null) {
        room.setGuestName("BOT");
        room.setBotGame(true);
      }
      room.setStatus(Status.PLAYING);
      ensureClockStarted(room, nowMs());
    }
    return room;
  }

  private void makeMove(Room room, String fen, String move, long nowMs) {
    String currentHistory = room.getMoveHistory();
    if (currentHistory == null || currentHistory.isBlank()) {
      room.setMoveHistory(move);
    } else {
      room.setMoveHistory(currentHistory + " " + move);
    }
    Turn mover = room.getTurn();
    room.setFen(fen);
    boolean hostTurn = mover == Turn.HOST;
    room.setTurn(hostTurn ? Turn.GUEST : Turn.HOST);
    addIncrement(room, mover);
    room.setTurnStartedAtEpochMs(nowMs);
  }

  @Transactional
  public Room applyMove(String roomId, String fen, String move, boolean isCheckmate, String requestingUserId) {
    Room room = getRoomForUpdate(roomId);
    long now = nowMs();
    ensureClockStarted(room, now);
    applyElapsedForTurn(room, now);
    if (room.getStatus() != Status.PLAYING) {
      return room;
    }

    // Authorization check
    if (room.getTurn() == Turn.HOST) {
      if (room.getHostId() != null && !room.getHostId().equals(requestingUserId)) {
        throw new IllegalStateException("Not your turn or unauthorized");
      }
    } else {
      if (room.getGuestId() != null && !room.getGuestId().equals(requestingUserId)) {
        throw new IllegalStateException("Not your turn or unauthorized");
      }
    }

    // 1. Direct checkmate/stalemate submission with no move (e.g. at the start of a
    // player's turn)
    if (isCheckmate && (move == null || move.isBlank())) {
      room.setStatus(Status.FINISHED);
      room.setEndReason(room.getTurn() == Turn.HOST ? EndReason.CHECKMATE_GUEST : EndReason.CHECKMATE_HOST);
      room.setTurnStartedAtEpochMs(0L);
      return room;
    }

    // 2. Apply player's move
    Turn playerSide = room.getTurn();
    makeMove(room, fen, move, now);

    if (isCheckmate) {
      room.setStatus(Status.FINISHED);
      room.setEndReason(playerSide == Turn.HOST ? EndReason.CHECKMATE_GUEST : EndReason.CHECKMATE_HOST);
      room.setTurnStartedAtEpochMs(0L);
      return room;
    }

    // 3. Apply BOT's move if the opponent is BOT
    if (room.isBotGame()) {
      long botStartNow = nowMs();
      applyElapsedForTurn(room, botStartNow);
      if (room.getStatus() != Status.PLAYING) {
        return room;
      }
      EngineService.EngineMoveResult result = engineService.getFenAfterBestMove(room.getFen());
      if (result != null) {
        if ("(none)".equals(result.move())) {
          room.setStatus(Status.FINISHED);
          room.setEndReason(room.getTurn() == Turn.HOST ? EndReason.CHECKMATE_HOST : EndReason.CHECKMATE_GUEST);
          room.setTurnStartedAtEpochMs(0L);
        } else if (result.fen() != null && result.move() != null) {
          long botEndNow = nowMs();
          makeMove(room, result.fen(), result.move(), botEndNow);

        }
      }
    }
    return room;
  }

  @Transactional
  public Room getRoom(String roomId) {
    Room room = roomRepository.findById(normalizeRoomId(roomId))
        .orElseThrow(() -> new IllegalStateException("Room not found"));

    // If clock has run out, finalize once so clients see FINISHED + endReason.
    if (room.getStatus() == Status.PLAYING && room.getTurnStartedAtEpochMs() > 0L) {
      long now = nowMs();
      ClockView view = computeClockView(room, now);
      if (isTimedOut(view, room.getTurn())) {
        Room locked = getRoomForUpdate(roomId);
        ensureClockStarted(locked, now);
        applyElapsedForTurn(locked, now);
        return locked;
      }
    }

    return room;
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

  public List<Room> listMatch(String userId) {
    return roomRepository.findRoomByUserId(userId);
  }
}