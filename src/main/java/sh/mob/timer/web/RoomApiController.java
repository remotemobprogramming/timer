package sh.mob.timer.web;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
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
import sh.mob.timer.web.Room.TimeLeft;

@RestController
@RequestMapping()
public class RoomApiController {

  private final RoomRepository roomRepository;

  public RoomApiController(RoomRepository roomRepository) {
    this.roomRepository = roomRepository;
  }

  @GetMapping
  @RequestMapping(value = "/{roomId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> subscribeToEvents(
      @PathVariable String roomId, ServerHttpResponse response) {
    response
        .getHeaders()
        .setCacheControl("no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0");
    response.getHeaders().add("X-Accel-Buffering", "no");
    response.getHeaders().setConnection("keep-alive");
    var room = roomRepository.get(roomId);

    var durationBefore = new AtomicReference<Duration>();

    return Flux.interval(Duration.ofMillis(250L))
        .flatMap(
            sequence -> {
              var timeLeft = room.timeLeft();
              var durationLeft = timeLeft.duration();
              boolean isFirstInterval = durationBefore.get() == null;

              var wasNotZeroBefore = isFirstInterval || !durationBefore.get().isZero();
              durationBefore.set(durationLeft);

              if (isFirstInterval && durationLeft.isZero()) {
                return Flux.fromStream(Stream.of(timerUpdate(sequence, timeLeft)));
              } else if (durationLeft.isZero() && wasNotZeroBefore) {
                return Flux.fromStream(
                    Stream.of(timerUpdate(sequence, timeLeft), timerFinished(sequence)));
              } else if (durationLeft.isZero()) {
                return Flux.empty();
              } else {
                return Flux.fromStream(Stream.of(timerUpdate(sequence, timeLeft)));
              }
            });
  }

  private static ServerSentEvent<String> timerUpdate(Long sequence, TimeLeft timeLeft) {
    String data =
        timeLeft.timer() == null
            ? "00:00"
            : String.format(
                "%02d:%02d",
                timeLeft.duration().toMinutesPart(),
                timeLeft.duration().toSecondsPart());
    return ServerSentEvent.<String>builder()
        .id(String.valueOf(sequence))
        .event("TIMER_UPDATE")
        .data(data)
        .build();
  }

  private static ServerSentEvent<String> timerFinished(Long sequence) {
    return ServerSentEvent.<String>builder()
        .id(String.valueOf(sequence))
        .event("TIMER_FINISHED")
        .data("finished")
        .build();
  }

  @PutMapping("/{roomId}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void publishEvent(@PathVariable String roomId, @RequestBody TimerRequest timerRequest) {
    var room = roomRepository.get(roomId);
    room.add(timerRequest.timer(), timerRequest.user());
    System.out.println("timerRequest = " + timerRequest);
  }

  static final class TimerRequest {

    private final Long timer;
    private final String user;

    TimerRequest(Long timer, String user) {
      this.timer = timer;
      this.user = user;
    }

    public Long timer() {
      return timer;
    }

    public String user() {
      return user;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      var that = (TimerRequest) obj;
      return Objects.equals(this.timer, that.timer) && Objects.equals(this.user, that.user);
    }

    @Override
    public int hashCode() {
      return Objects.hash(timer, user);
    }

    @Override
    public String toString() {
      return "TimerRequest[" + "timer=" + timer + ", " + "user=" + user + ']';
    }
  }
}
