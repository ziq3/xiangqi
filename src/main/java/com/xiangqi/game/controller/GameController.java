package com.xiangqi.game.controller;

import com.xiangqi.game.model.Room;
import com.xiangqi.game.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;

@RestController
public class GameController {
    RoomService roomService;

    public GameController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/api/room/create")
    public RoomStateResponse createRoom(@RequestParam(required = false) String hostName, Principal principal) {
        String resolvedHost = resolvePlayerName(hostName, principal);
        Room room = roomService.createRoom(resolvedHost);
        return RoomStateResponse.from(room);
    }

    @PostMapping("/api/room/{roomId}/join")
    public RoomStateResponse joinRoom(@PathVariable String roomId,
            @RequestParam(required = false) String playerName,
            Principal principal) {
        String resolvedPlayer = resolvePlayerName(playerName, principal);
        Room room = roomService.joinRoom(roomId, resolvedPlayer);
        return RoomStateResponse.from(room);
    }

    @GetMapping("/api/room/{roomId}")
    public RoomStateResponse getRoom(@PathVariable String roomId) {
        Room room = roomService.getRoom(roomId);
        return RoomStateResponse.from(room);
    }

    @PostMapping("/api/room/{roomId}/start")
    public RoomStateResponse startRoom(@PathVariable String roomId) {
        Room room = roomService.startRoom(roomId);
        return RoomStateResponse.from(room);
    }

    public record MoveRequest(String move) {
    }

    @PostMapping("/api/room/{roomId}/move")
    public RoomStateResponse applyMove(
            @PathVariable String roomId,
            @RequestBody MoveRequest payload,
            Principal principal) {

        Room room = roomService.applyMove(roomId, payload.move());
        return RoomStateResponse.from(room);
    }

    private String resolvePlayerName(String providedName, Principal principal) {
        if (principal instanceof JwtAuthenticationToken jwtToken) {
            java.util.Map<String, Object> userMetadata = (java.util.Map<String, Object>) jwtToken.getTokenAttributes()
                    .get("user_metadata");

            if (userMetadata != null && userMetadata.containsKey("username")) {
                return userMetadata.get("username").toString().trim();
            }

            if (jwtToken.getTokenAttributes().containsKey("email")) {
                return jwtToken.getTokenAttributes().get("email").toString().trim();
            }

            return principal.getName().trim();
        }

        if (providedName != null && !providedName.isBlank()) {
            return providedName.trim();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "You must be logged in or provide a guest name to play");
    }

    private record RoomStateResponse(
            String roomId,
            String hostName,
            String guestName,
            String turn,
            String status,
            String fen) {
        private static RoomStateResponse from(Room room) {
            return new RoomStateResponse(
                    room.getRoomId(),
                    room.getHostName(),
                    room.getGuestName(),
                    room.getTurn().name(),
                    room.getStatus().name(),
                    room.getFen());
        }
    }

}