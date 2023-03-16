package sh.mob.timer.web;

import static java.time.temporal.ChronoUnit.*;
import static java.util.function.Predicate.not;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;
import sh.mob.timer.web.Room.TimerRequest.TimerType;

final class Room {

  private static final Logger log = LoggerFactory.getLogger(Room.class);

  public static final TimerRequest NULL_TIMER_REQUEST =
      new TimerRequest(0L, null, null, null, null);
  private final String name;
  private final List<TimerRequest> timerRequests = new CopyOnWriteArrayList<>();
  private final Sinks.Many<TimerRequest> sink =
      Sinks.many().replay().latestOrDefault(NULL_TIMER_REQUEST);
  private Goal currentGoal = Goal.NO_GOAL;

  private final Sinks.Many<Goal> goalRequestSink =
      Sinks.many().replay().latestOrDefault(Goal.NO_GOAL);

  Room(String name) {
    this.name = name;
  }

  public void addTimer(Long timer, String user, Instant requested) {
    var nextUser = findNextUser(user);
    var timerRequest = new TimerRequest(timer, requested, user, nextUser, TimerType.TIMER);
    timerRequests.add(timerRequest);
    sink.tryEmitNext(timerRequest);
  }

  public void setGoal(String text, String user, Instant requested) {
    var newGoal = new Goal(text, requested, user);
    currentGoal = newGoal ;
    goalRequestSink.tryEmitNext(newGoal);
  }

  public void deleteGoal(String user) {
    if(currentGoal != Goal.NO_GOAL){
      currentGoal = Goal.NO_GOAL;
      goalRequestSink.tryEmitNext(Goal.NO_GOAL);
      log.info(
              "Delete current goal by user {} for room {}",
              user,
              name);
    } else {
      log.info("Try to delete current goal by user {} for room {}, but there is no current goal.", user, name);
    }
  }

  private String findNextUser(String user) {
    if (timerRequests.isEmpty()) {
      return null;
    }

    var users =
        timerRequests.stream()
            .filter(timerRequest -> timerRequest.type == TimerType.TIMER)
            .map(TimerRequest::getUser)
            .filter(Objects::nonNull)
            .filter(not(String::isBlank))
            .collect(Collectors.toList());

    while (!users.isEmpty() && users.lastIndexOf(user) == users.size() - 1) {
      users.remove(users.size() - 1);
    }

    if (users.isEmpty()) {
      return null;
    }

    int nextIndexCandidate = users.lastIndexOf(user) + 1;
    return users.get(nextIndexCandidate);
  }

  public void addBreaktimer(Long breaktimer, String user) {
    TimerRequest timerRequest =
        new TimerRequest(
            breaktimer,
            Instant.now(),
            user,
            lastTimerRequest().map(TimerRequest::getNextUser).orElse(null),
            TimerType.BREAKTIMER);
    timerRequests.add(timerRequest);
    sink.tryEmitNext(timerRequest);
  }

  public Sinks.Many<TimerRequest> timerRequestSink() {
    return sink;
  }

  public Sinks.Many<Goal> goalRequestSink() {
    return goalRequestSink;
  }

  Optional<TimerRequest> lastTimerRequest() {
    if (timerRequests.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(timerRequests.get(timerRequests.size() - 1));
  }

  public void removeOldTimerRequests() {
    var now = Instant.now();
    this.timerRequests.removeIf(
        timerRequest -> now.minus(24, HOURS).isAfter(timerRequest.getRequested()));
    if (timerRequests.isEmpty()) {
      sink.tryEmitNext(NULL_TIMER_REQUEST);
      log.info("Emptied room {}", name);
    }
  }

  public String name() {
    return name;
  }

  public Goal currentGoal() {
    return currentGoal;
  }

  public List<TimerRequest> historyWithoutLatest() {
    if (timerRequests.isEmpty()) {
      return List.of();
    }

    return timerRequests.subList(0, timerRequests.size() - 1);
  }

  public boolean isTimerActive(Instant now) {
    return lastTimerRequest().filter(timerRequest -> isTimerActive(timerRequest, now)).isPresent();
  }

  private static boolean isTimerActive(TimerRequest timerRequest, Instant now) {
    return timerRequest.getTimer() != null
        && timerRequest.getTimer() > 0
        && timerRequest.getRequested() != null
        && timerRequest.getRequested().plus(timerRequest.getTimer(), MINUTES).isAfter(now);
  }

  public record Goal(String goal, Instant requested, String user){
    public static final Goal NO_GOAL = new Goal(null, null, null);
  }

  public static final class TimerRequest {

    enum TimerType {
      TIMER,
      BREAKTIMER
    }

    private final Long timer;
    private final Instant requested;
    private final String user;
    private final String nextUser;
    private final TimerType type;

    TimerRequest(Long timer, Instant requested, String user, String nextUser, TimerType type) {
      this.timer = timer;
      this.requested = requested;
      this.user = user;
      this.nextUser = nextUser;
      this.type = type;
    }

    public Long getTimer() {
      return timer;
    }

    public Instant getRequested() {
      return requested;
    }

    public String getUser() {
      return user;
    }

    public String getNextUser() {
      return nextUser;
    }

    public TimerType getType() {
      return type;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) return true;
      if (obj == null || obj.getClass() != this.getClass()) return false;
      var that = (TimerRequest) obj;
      return Objects.equals(this.timer, that.timer)
          && Objects.equals(this.requested, that.requested)
          && Objects.equals(this.user, that.user)
          && Objects.equals(this.nextUser, that.nextUser)
          && this.type == that.type;
    }

    @Override
    public int hashCode() {
      return Objects.hash(timer, requested, user, nextUser, type);
    }

    @Override
    public String toString() {
      return "TimerRequest["
          + "timer="
          + timer
          + ", "
          + "requested="
          + requested
          + ", "
          + "user="
          + user
          + ", "
          + "nextUser="
          + nextUser
          + ", "
          + "type="
          + type
          + ']';
    }
  }
}
