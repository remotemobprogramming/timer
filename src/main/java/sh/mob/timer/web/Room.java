package sh.mob.timer.web;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

  public static final TimerRequest NULL_TIMER_REQUEST = new TimerRequest(0L, null, null, null, null);
  private final String name;
  private final List<TimerRequest> timerRequests = new CopyOnWriteArrayList<>();
  private final Sinks.Many<TimerRequest> sink =
      Sinks.many().replay().latestOrDefault(NULL_TIMER_REQUEST);

  Room(String name) {
    this.name = name;
  }

  public void add(Long timer, String user, Instant requested) {
    String nextUser = findNextUser(user);
    TimerRequest timerRequest =
        new TimerRequest(timer, requested, user, nextUser, TimerType.TIMER);
    timerRequests.add(timerRequest);
    sink.tryEmitNext(timerRequest);
  }

  private String findNextUser(String user) {
    if (timerRequests.isEmpty()) {
      return null;
    }

    var users =
        timerRequests.stream()
            .filter(timerRequest -> timerRequest.type == TimerType.TIMER)
            .map(TimerRequest::getUser)
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

  public Sinks.Many<TimerRequest> sink() {
    return sink;
  }

  Optional<TimerRequest> lastTimerRequest() {
    if (timerRequests.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(timerRequests.get(timerRequests.size() - 1));
  }

  public void removeOldTimerRequests() {
    this.timerRequests.removeIf(
        timerRequest ->
            Instant.now().minus(24, ChronoUnit.HOURS).isAfter(timerRequest.getRequested()));
    if (timerRequests.isEmpty()) {
      sink.tryEmitNext(NULL_TIMER_REQUEST);
      log.info("Emptied room {}", name);
    }
  }

  public String name() {
    return name;
  }

  public List<TimerRequest> historyWithoutLatest() {
    if (timerRequests.isEmpty()) {
      return List.of();
    }

    return timerRequests.subList(0, timerRequests.size() - 1);
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
