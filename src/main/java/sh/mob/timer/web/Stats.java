package sh.mob.timer.web;

import java.time.Instant;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class Stats {

  private static final String SMOKETEST_ROOM_NAME = "testroom-310a9c47-515c-4ad7-a229-ae8efbab7387";
  private final Instant statisticsSince = Instant.now();
  private ConcurrentHashMap<Long, Long> timerCounts = new ConcurrentHashMap<Long, Long>();
  private ConcurrentHashMap<Long, Long> breaktimerCounts = new ConcurrentHashMap<Long, Long>();
  private AtomicLong goalCount = new AtomicLong(0);

  public void incrementTimer(String roomname, long timer) {
    if (Objects.equals(roomname, SMOKETEST_ROOM_NAME)) {
      return;
    }
    Long count = timerCounts.get(timer);
    if (count == null) {
      count = 0L;
    }
    count++;
    timerCounts.put(timer, count);
  }

  public void incrementBreaktimer(String roomname, long breakTimer) {
    if (Objects.equals(roomname, SMOKETEST_ROOM_NAME)) {
      return;
    }
    Long count = breaktimerCounts.get(breakTimer);
    if (count == null) {
      count = 0L;
    }
    count++;
    breaktimerCounts.put(breakTimer, count);
  }

  public void incrementGoalCount(String roomname) {
    if(Objects.equals(roomname, SMOKETEST_ROOM_NAME)){
      return;
    }
    goalCount.incrementAndGet();
  }

  public long getGoalCount() {
    return goalCount.get();
  }

  public SortedMap<Long, Long> getTimer() {
    return new TreeMap<>(timerCounts);
  }

  public SortedMap<Long, Long> getBreaktimer() {
    return new TreeMap<>(breaktimerCounts);
  }

  public Instant getStatisticsSince() {
    return statisticsSince;
  }
}
