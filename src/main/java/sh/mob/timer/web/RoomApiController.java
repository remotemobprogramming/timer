package sh.mob.timer.web;

import java.util.Objects;
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

@RestController
@RequestMapping()
public class RoomApiController {

  private final RoomRepository roomRepository;

  public RoomApiController(RoomRepository roomRepository) {
    this.roomRepository = roomRepository;
  }

  @GetMapping
  @RequestMapping(value = "/{roomId:[a-z0-9-]+}/sse2", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<Room.TimerRequest>> subscribeToEvents2(
      @PathVariable String roomId, ServerHttpResponse response) {
    response
        .getHeaders()
        .setCacheControl("no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0");
    response.getHeaders().add("X-Accel-Buffering", "no");
    response.getHeaders().setConnection("keep-alive");
    var room = roomRepository.get(roomId);

    return room.sink()
        .asFlux()
        .map(
            timerRequest ->
                ServerSentEvent.<Room.TimerRequest>builder()
                    .event("TIMER_REQUEST")
                    .data(timerRequest)
                    .build());
  }

  @PutMapping("/{roomId:[a-z0-9-]+}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void publishEvent(@PathVariable String roomId, @RequestBody TimerRequest timerRequest) {
    var room = roomRepository.get(roomId);
    if (timerRequest.timer() != null) {
      room.add(timerRequest.timer(), timerRequest.user());
    } else if (timerRequest.breaktimer() != null) {
      room.addBreaktimer(timerRequest.breaktimer(), timerRequest.user());
    }
  }

  static final class TimerRequest {

    private final Long timer;
    private final Long breaktimer;
    private final String user;

    TimerRequest(Long timer, Long breaktimer, String user) {
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

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      var that = (TimerRequest) obj;
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
      return "TimerRequest["
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
