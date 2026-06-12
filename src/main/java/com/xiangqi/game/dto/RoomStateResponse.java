package com.xiangqi.game.dto;

import com.xiangqi.game.model.Room;
import com.xiangqi.game.service.RoomService;

/**
 * Wire representation of a room's state, shared by the REST controller and the
 * SSE event stream so both push exactly the same shape to clients.
 */
public record RoomStateResponse(
        String roomId,
        String hostName,
        String guestName,
        String turn,
        String status,
        String fen,
        long hostTimeMs,
        long guestTimeMs,
        String endReason,
        String moveHistory) {

    public static RoomStateResponse from(Room room, RoomService roomService) {
        RoomService.ClockView clock = roomService.computeClockView(room);
        return new RoomStateResponse(
                room.getRoomId(),
                room.getHostName(),
                room.getGuestName(),
                room.getTurn().name(),
                room.getStatus().name(),
                room.getFen(),
                clock.hostRemainingMs(),
                clock.guestRemainingMs(),
                room.getEndReason() == null ? null : room.getEndReason().name(),
                room.getMoveHistory());
    }
}
