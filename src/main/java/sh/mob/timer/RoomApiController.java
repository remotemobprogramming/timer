package sh.mob.timer;

import java.time.Duration;
import java.time.Instant;
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

    return Flux.interval(Duration.ofMillis(250L))
        .map(
            sequence ->
                ServerSentEvent.<String>builder()
                    .id(String.valueOf(sequence))
                    .event("TIMER_UPDATE")
                    .data("" + room.timeLeft())
                    .build());
  }

  @PutMapping("/{roomId}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void publishEvent(@PathVariable String roomId, @RequestBody TimerRequest timerRequest) {
    var room = roomRepository.get(roomId);
    room.add(timerRequest.timer());
    System.out.println("timerRequest = " + timerRequest);
  }

  record TimerRequest(Long timer) {}
}
