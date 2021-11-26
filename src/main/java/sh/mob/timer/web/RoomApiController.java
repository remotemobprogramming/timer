package sh.mob.timer.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import sh.mob.timer.web.Room.TimerRequest;

@RestController
@RequestMapping()
public class RoomApiController {

  private static final Logger log = LoggerFactory.getLogger(RoomApiController.class);

  private final RoomRepository roomRepository;
  private final Clock clock;

  public RoomApiController(RoomRepository roomRepository, Clock clock) {
    this.roomRepository = roomRepository;
    this.clock = clock;
  }

  @GetMapping
  @RequestMapping(
      value = "/{roomId:[a-z0-9-]+}/events",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<Object>> getEventStream(
      @PathVariable String roomId, ServerHttpResponse response) {
    response
        .getHeaders()
        .setCacheControl("no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0");
    response.getHeaders().add("X-Accel-Buffering", "no");
    response.getHeaders().setConnection("keep-alive");
    var room = roomRepository.get(roomId);

    var timerRequestFlux =
        room.sink()
            .asFlux()
            .map(
                timerRequest ->
                    ServerSentEvent.builder().event("TIMER_REQUEST").data(timerRequest).build());
    var keepAliveFlux =
        Flux.interval(Duration.ofSeconds(5L))
            .map(
                second ->
                    ServerSentEvent.builder()
                        .event("KEEP_ALIVE")
                        .data(new TimerRequest(null, null, null, null, null))
                        .build());
    var initialHistory =
        Flux.just(room.historyWithoutLatest())
            .map(list -> ServerSentEvent.builder().event("INITIAL_HISTORY").data(list).build());

    return Flux.concat(initialHistory, keepAliveFlux.mergeWith(timerRequestFlux));
  }

  @PutMapping("/{roomId:[a-z0-9-]+}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void publishEvent(@PathVariable String roomId, @RequestBody PutTimerRequest timerRequest) {
    var room = roomRepository.get(roomId);
    if (timerRequest.timer() != null) {
      room.add(
          truncateTooLongTimers(timerRequest.timer()), timerRequest.user(), Instant.now(clock));
      log.info(
          "Add timer {} by user {} for room {}",
          timerRequest.timer,
          timerRequest.user,
          room.name());
    } else if (timerRequest.breaktimer() != null) {
      room.addBreaktimer(truncateTooLongTimers(timerRequest.breaktimer()), timerRequest.user());
      log.info(
          "Add break timer {} by user {} for room {}",
          timerRequest.breaktimer(),
          timerRequest.user,
          room.name());
    } else {
      log.warn("Could not understand PUT request for room {}", roomId);
    }
  }

  private static long truncateTooLongTimers(Long timer) {
    return Math.min(60 * 24, Math.max(0, timer));
  }

  static final class PutTimerRequest {

    private final Long timer;
    private final Long breaktimer;
    private final String user;

    PutTimerRequest(Long timer, Long breaktimer, String user) {
      this.timer = timer;
      this.user = user;
      this.breaktimer = breaktimer;
    }

    public Long timer() {
      return timer;
    }

    public Long breaktimer() {
      return breaktimer;
    }

    public String user() {
      return user;
    }

    public Long getTimer() {
      return timer;
    }

    public Long getBreaktimer() {
      return breaktimer;
    }

    public String getUser() {
      return user;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      var that = (PutTimerRequest) obj;
      return Objects.equals(this.timer, that.timer)
          && Objects.equals(this.breaktimer, that.breaktimer)
          && Objects.equals(this.user, that.user);
    }

    @Override
    public int hashCode() {
      return Objects.hash(timer, breaktimer, user);
    }

    @Override
    public String toString() {
      return "PutTimerRequest["
          + "timer="
          + timer
          + ", "
          + "breaktimer="
          + breaktimer
          + ", "
          + "user="
          + user
          + ']';
    }
  }
}
