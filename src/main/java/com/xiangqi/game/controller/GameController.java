package com.xiangqi.game.controller;

import com.xiangqi.game.model.Room;
import com.xiangqi.game.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
public class GameController {
    RoomService roomService;

    public GameController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/")
    public String home() {
        return "index"; // Chỉ làm nhiệm vụ trả về file giao diện
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/h2h")
    public String humanVsHuman() {
        return "h2h";
    }

    @PostMapping("/api/room/create")
    @ResponseBody
    public RoomStateResponse createRoom(@RequestParam(required = false) String hostName, Principal principal) {
        String resolvedHost = resolvePlayerName(hostName, principal);
        Room room = roomService.createRoom(resolvedHost);
        return RoomStateResponse.from(room);

    }

    @PostMapping("/api/room/{roomId}/join")
    @ResponseBody
    public RoomStateResponse joinRoom(@PathVariable String roomId,
                                      @RequestParam(required = false) String playerName,
                                      Principal principal) {
        String resolvedPlayer = resolvePlayerName(playerName, principal);
        Room room = roomService.joinRoom(roomId, resolvedPlayer);
        return RoomStateResponse.from(room);
    }

    @GetMapping("/api/room/{roomId}")
    @ResponseBody
    public RoomStateResponse getRoom(@PathVariable String roomId) {
        Room room = roomService.getRoom(roomId);
        return RoomStateResponse.from(room);
    }

    @PostMapping("/api/room/{roomId}/move")
    @ResponseBody
    public RoomStateResponse applyMove(@PathVariable String roomId,
                                       @RequestParam String move,
                                       Principal principal) {
        Room room = roomService.applyMove(roomId, move);
        return RoomStateResponse.from(room);
    }

    private String resolvePlayerName(String providedName, Principal principal) {
        if (providedName != null && !providedName.isBlank()) {
            return providedName.trim();
        }
        if (principal != null) {
            return principal.getName().trim();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerName is required");
    }

    private record RoomStateResponse(
            String roomId,
            String hostName,
            String guestName,
            String turn,
            String status,
            String moveHistory,
            String boardStartFen) {
        private static RoomStateResponse from(Room room) {
            return new RoomStateResponse(
                    room.getRoomId(),
                    room.getHostName(),
                    room.getGuestName(),
                    room.getTurn().name(),
                    room.getStatus().name(),
                    room.getMoveHistory(),
                    "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1");
        }
    }

}