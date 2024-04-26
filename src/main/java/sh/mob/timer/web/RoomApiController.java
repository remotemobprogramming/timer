package sh.mob.timer.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import sh.mob.timer.web.Room.TimerRequest;

@RestController
@RequestMapping()
public class RoomApiController {

  private static final Logger log = LoggerFactory.getLogger(RoomApiController.class);
  private final RoomRepository roomRepository;
  private final Clock clock;
  private final Stats stats;

  public RoomApiController(RoomRepository roomRepository, Clock clock, Stats stats) {
    this.roomRepository = roomRepository;
    this.clock = clock;
    this.stats = stats;
  }

  @GetMapping
  @RequestMapping(
      value = "/{roomId:[A-Za-z0-9-_]+}/events",
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
        room.timerRequestSink()
            .asFlux()
            .map(
                timerRequest ->
                    ServerSentEvent.builder().event("TIMER_REQUEST").data(timerRequest).build());
    var goalRequestFlux =
        room.goalRequestSink()
            .asFlux()
            .map(
                goalRequest ->
                    ServerSentEvent.builder().event("GOAL_REQUEST").data(goalRequest).build());
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

    return Flux.concat(initialHistory, keepAliveFlux.mergeWith(timerRequestFlux).mergeWith(goalRequestFlux));
  }

  @PutMapping("/{roomId:[A-Za-z0-9-_]+}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void publishEvent(@PathVariable String roomId, @RequestBody PutTimerRequest timerRequest) {
    var room = roomRepository.get(roomId);
    if (timerRequest.timer() != null) {
      long timer = truncateTooLongTimers(timerRequest.timer());
      room.addTimer(
          timer, timerRequest.user(), Instant.now(clock));
      log.info(
          "Add timer {} by user {} for room {}",
          timerRequest.timer,
          timerRequest.user,
          room.name());
      stats.incrementTimer(room.name(), timer);
    } else if (timerRequest.breaktimer() != null) {
      long breaktimer = truncateTooLongTimers(timerRequest.breaktimer());
      room.addBreaktimer(breaktimer, timerRequest.user());
      log.info(
          "Add break timer {} by user {} for room {}",
          timerRequest.breaktimer(),
          timerRequest.user,
          room.name());
      stats.incrementBreaktimer(room.name(), breaktimer);
    } else {
      log.warn("Could not understand PUT request for room {}", roomId);
    }
  }

  @PutMapping("/{roomId:[A-Za-z0-9-_]+}/goal")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void putGoal(@PathVariable String roomId, @RequestBody PutGoalRequest goalRequest) {
    var room = roomRepository.get(roomId);
    if (goalRequest.goal() != null) {
      String goal = truncateTooLongGoal(goalRequest.goal());
      room.setGoal(
              goal, goalRequest.user(), Instant.now(clock));
      log.info(
              "Add goal \"{}\" by user {} for room {}",
              goalRequest.goal(),
              goalRequest.user(),
              room.name());
      stats.incrementGoalCount(room.name());
    } else {
      log.warn("Could not understand PUT goal request for room {}", roomId);
    }
  }

  @DeleteMapping("/{roomId:[A-Za-z0-9-_]+}/goal")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void deleteGoal(@PathVariable String roomId, @RequestBody DeleteGoalRequest deleteGoalRequest) {
    var room = roomRepository.get(roomId);
    room.deleteGoal(deleteGoalRequest.user(), Instant.now(clock));
  }

  @GetMapping("/{roomId:[A-Za-z0-9-_]+}/goal")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<GoalResponse> getGoal(@PathVariable String roomId) {
    var room = roomRepository.get(roomId);
    if (!room.hasGoal()){
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ofNullable(GoalResponse.of(room.currentGoal()));
  }

  private static long truncateTooLongTimers(Long timer) {
    return Math.min(60 * 24, Math.max(0, timer));
  }

  private static String truncateTooLongGoal(String goal) {
    return goal.length() > 256 ? goal.substring(0,256-1-3) + "...": goal;
  }

  public record GoalResponse(String goal){
    public static GoalResponse of(Room.Goal goal){
      return new GoalResponse(goal.goal());
    }
  }

  public record PutGoalRequest(String goal, String user){}
  public record DeleteGoalRequest(String user){}
  public record PutTimerRequest(Long timer, Long breaktimer, String user){}
}
