package com.xiangqi.game.service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.xiangqi.game.dto.RoomStateResponse;
import com.xiangqi.game.model.Room;

/**
 * Server-Sent Events hub for room state. Clients open one stream per room and
 * receive the authoritative {@link RoomStateResponse} whenever the room changes
 * (a move/join/start) and on a periodic heartbeat that keeps clocks ticking and
 * finalizes timeouts.
 *
 * <p>The heartbeat only touches rooms that currently have a subscriber, so the
 * cost is one DB read per <em>active room</em> per tick rather than the old
 * two-reads-per-second-per-client polling.
 */
@Service
public class RoomEventService {

    /** How long an idle stream is held open before the client must reconnect. */
    private static final long STREAM_TIMEOUT_MS = 30L * 60L * 1000L;

    private final RoomService roomService;

    private final Map<String, Set<SseEmitter>> emittersByRoom = new ConcurrentHashMap<>();

    /** Last status pushed by the heartbeat, used to push terminal states only once. */
    private final Map<String, Room.Status> lastHeartbeatStatus = new ConcurrentHashMap<>();

    public RoomEventService(RoomService roomService) {
        this.roomService = roomService;
    }

    /** Registers a new stream for {@code roomId} and immediately sends the current state. */
    public SseEmitter subscribe(String roomId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        Set<SseEmitter> emitters = emittersByRoom.computeIfAbsent(roomId, key -> new CopyOnWriteArraySet<>());
        emitters.add(emitter);

        Runnable cleanup = () -> remove(roomId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        // Prime the new stream with the latest known state.
        Room room = roomService.getRoom(roomId);
        send(emitter, RoomStateResponse.from(room, roomService));

        return emitter;
    }

    /** Pushes {@code state} to every stream open for {@code roomId}. */
    public void publish(String roomId, RoomStateResponse state) {
        Set<SseEmitter> emitters = emittersByRoom.get(roomId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            if (!send(emitter, state)) {
                remove(roomId, emitter);
            }
        }
    }

    /**
     * Recomputes and pushes state for every room that has at least one open
     * stream. Runs on a fixed cadence so clocks stay live and timeouts are
     * finalized even when neither player is moving.
     */
    @Scheduled(fixedRate = 1000L)
    public void heartbeat() {
        for (String roomId : emittersByRoom.keySet()) {
            Set<SseEmitter> emitters = emittersByRoom.get(roomId);
            if (emitters == null || emitters.isEmpty()) {
                lastHeartbeatStatus.remove(roomId);
                continue;
            }

            Room room;
            try {
                // getRoom() finalizes a timed-out clock and returns FINISHED.
                room = roomService.getRoom(roomId);
            } catch (RuntimeException ex) {
                continue;
            }

            Room.Status status = room.getStatus();
            boolean playing = status == Room.Status.PLAYING;
            boolean statusChanged = lastHeartbeatStatus.put(roomId, status) != status;

            // While playing, push every tick (the clock is moving). Otherwise only
            // push when the status just changed, so we don't spam a terminal state.
            if (playing || statusChanged) {
                publish(roomId, RoomStateResponse.from(room, roomService));
            }
        }
    }

    private boolean send(SseEmitter emitter, RoomStateResponse state) {
        try {
            // Synchronize per emitter: heartbeat and request threads can race otherwise.
            synchronized (emitter) {
                emitter.send(SseEmitter.event().data(state));
            }
            return true;
        } catch (IOException | IllegalStateException ex) {
            return false;
        }
    }

    private void remove(String roomId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByRoom.get(roomId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByRoom.remove(roomId);
            lastHeartbeatStatus.remove(roomId);
        }
    }
}
