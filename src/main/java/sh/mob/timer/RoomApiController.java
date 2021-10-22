package sh.mob.timer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
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
  @RequestMapping(value = "/{roomId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> subscribeToEvents(@PathVariable String roomId) {
    var room = roomRepository.get(roomId);

    AtomicReference<Duration> durationBefore = new AtomicReference<>();

    return Flux.interval(Duration.ofMillis(250L))
        .flatMap(
            sequence -> {
              var durationLeft = room.timeLeft();
              boolean isFirstInterval = durationBefore.get() == null;

              var wasNotZeroBefore = isFirstInterval || !durationBefore.get().isZero();
              durationBefore.set(durationLeft);

              if (isFirstInterval && durationLeft.isZero()) {
                return Flux.fromStream(Stream.of(timerUpdate(sequence, durationLeft)));
              } else if (durationLeft.isZero() && wasNotZeroBefore) {
                System.out.println("DURATION ZERO");
                return Flux.fromStream(
                    Stream.of(timerUpdate(sequence, durationLeft), timerFinished(sequence)));
              } else if (durationLeft.isZero()) {
                return Flux.empty();
              } else {
                return Flux.fromStream(Stream.of(timerUpdate(sequence, durationLeft)));
              }
            });
  }

  private static ServerSentEvent<String> timerUpdate(Long sequence, Duration durationLeft) {
    return ServerSentEvent.<String>builder()
        .id(String.valueOf(sequence))
        .event("TIMER_UPDATE")
        .data("" + durationLeft)
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

  record TimerRequest(Long timer, String user) {}
}
