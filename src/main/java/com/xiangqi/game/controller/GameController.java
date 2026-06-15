package com.xiangqi.game.controller;

import com.xiangqi.game.dto.RoomStateResponse;
import com.xiangqi.game.model.Room;
import com.xiangqi.game.service.EngineService;
import com.xiangqi.game.service.RoomEventService;
import com.xiangqi.game.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;
import java.util.List;

@RestController
public class GameController {
    RoomService roomService;
    RoomEventService roomEventService;
    EngineService engineService;

    public GameController(RoomService roomService, RoomEventService roomEventService, EngineService engineService) {
        this.roomService = roomService;
        this.roomEventService = roomEventService;
        this.engineService = engineService;
    }

    @PostMapping("/api/room/create")
    public RoomStateResponse createRoom(@RequestParam(required = false) String hostName, Principal principal) {
        String resolvedHost = resolvePlayerName(hostName, principal);
        String id = null;
        if (principal instanceof JwtAuthenticationToken jwtToken) {
            id = jwtToken.getName();
        }
        Room room = roomService.createRoom(resolvedHost, id);
        return RoomStateResponse.from(room, roomService);
    }

    @PostMapping("/api/room/{roomId}/join")
    public RoomStateResponse joinRoom(@PathVariable String roomId,
            @RequestParam(required = false) String playerName,
            Principal principal) {
        String resolvedPlayer = resolvePlayerName(playerName, principal);
        String id = null;
        if (principal instanceof JwtAuthenticationToken jwtToken) {
            id = jwtToken.getName();
        }
        Room room = roomService.joinRoom(roomId, resolvedPlayer, id);
        return broadcast(room);
    }

    @GetMapping("/api/room/{roomId}")
    public RoomStateResponse getRoom(@PathVariable String roomId) {
        Room room = roomService.getRoom(roomId);
        return RoomStateResponse.from(room, roomService);
    }

    @GetMapping("/api/user/listmatch")
    public List<RoomStateResponse> listMatch(Principal principal) {
        return roomService.listMatch(principal.getName())
                .stream()
                .map(room -> RoomStateResponse.from(room, roomService))
                .toList();
    }

    /**
     * Server-Sent Events stream of room state. Replaces client-side polling:
     * clients receive the current state on connect, then on every change and on
     * the server heartbeat.
     */
    @GetMapping(value = "/api/room/{roomId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRoom(@PathVariable String roomId) {
        return roomEventService.subscribe(roomId);
    }

    @PostMapping("/api/room/{roomId}/start")
    public RoomStateResponse startRoom(@PathVariable String roomId) {
        Room room = roomService.startRoom(roomId);
        return broadcast(room);
    }

    public record MoveRequest(String fen, String move, Boolean checkmate) {
    }

    @PostMapping("/api/room/{roomId}/move")
    public RoomStateResponse applyMove(
            @PathVariable String roomId,
            @RequestBody MoveRequest payload,
            Principal principal) {

        Room room = roomService.applyMove(roomId, payload.fen(), payload.move(),
                Boolean.TRUE.equals(payload.checkmate()));
        return broadcast(room);
    }

    @GetMapping(value = "/api/engine/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEngine(@RequestParam String fen) {
        return engineService.streamAnalysis(fen);
    }

    /** Builds the response and pushes it to every SSE subscriber of the room. */
    private RoomStateResponse broadcast(Room room) {
        RoomStateResponse state = RoomStateResponse.from(room, roomService);
        roomEventService.publish(room.getRoomId(), state);
        return state;
    }

    private String resolvePlayerName(String providedName, Principal principal) {
        if (principal instanceof JwtAuthenticationToken jwtToken) {
            java.util.Map<String, Object> userMetadata = (java.util.Map<String, Object>) jwtToken.getTokenAttributes()
                    .get("user_metadata");

            return userMetadata.get("username").toString().trim();
        }

        if (providedName != null && !providedName.isBlank()) {
            return providedName.trim();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "You must be logged in or provide a guest name to play");
    }

}