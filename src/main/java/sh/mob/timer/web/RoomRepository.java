package sh.mob.timer.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {

  private static final Logger log = LoggerFactory.getLogger(RoomRepository.class);

  private final Map<String, Room> repository;

  private final RoomNameGenerator roomNameGenerator;

  private final int maxRooms;
  private final Duration emptyRoomGrace;
  private final Duration roomRetention;
  private final int maxTimerRequestsPerRoom;
  private final Duration timerRequestRetention;

  public RoomRepository(
      RoomNameGenerator roomNameGenerator,
      @Value("${timer.max-rooms:2000}") int maxRooms,
      @Value("${timer.empty-room-grace:PT10M}") Duration emptyRoomGrace,
      @Value("${timer.room-retention:PT72H}") Duration roomRetention,
      @Value("${timer.max-timer-requests-per-room:500}") int maxTimerRequestsPerRoom,
      @Value("${timer.timer-request-retention:PT24H}") Duration timerRequestRetention) {
    this.roomNameGenerator = roomNameGenerator;
    this.maxRooms = maxRooms;
    this.emptyRoomGrace = emptyRoomGrace;
    this.roomRetention = roomRetention;
    this.maxTimerRequestsPerRoom = maxTimerRequestsPerRoom;
    this.timerRequestRetention = timerRequestRetention;
    this.repository = new ConcurrentHashMap<>();
  }

  Room get(String room) {
    var existingRoom = repository.get(room);
    if (existingRoom != null) {
      return existingRoom;
    }

    if (repository.size() >= maxRooms) {
      log.warn("Rejected creation of room {}: max room limit of {} reached", room, maxRooms);
      throw new TooManyRoomsException();
    }

    return repository.computeIfAbsent(
        room,
        name -> {
          log.info("Created room {}", name);
          return new Room(name, maxTimerRequestsPerRoom, timerRequestRetention);
        });
  }

  void deleteAll(){
    repository.clear();
  }

  @Scheduled(fixedRateString = "PT1M")
  void cleanUpUnusedRooms() {
    cleanUpUnusedRooms(Instant.now());
  }

  void cleanUpUnusedRooms(Instant now) {
    repository.forEach((key, room) -> room.removeOldTimerRequests());
    repository
        .entrySet()
        .removeIf(
            entry -> {
              boolean unused = entry.getValue().isUnused(now, emptyRoomGrace, roomRetention);
              if (unused) {
                log.info("Removed unused room {}", entry.getKey());
              }
              return unused;
            });
  }

  public String newRandomRoomName() {
    String newRandomRoom = roomNameGenerator.randomName();
    if (repository.containsKey(newRandomRoom)) {
      return newRandomRoomName();
    }

    return newRandomRoom;
  }

  public long count() {
    return repository.size();
  }

  public long countConnections() {
    return repository.values().stream()
        .mapToLong(room -> room.timerRequestSink().currentSubscriberCount())
        .sum();
  }

  public long countActiveTimers() {
    var now = Instant.now();
    return repository.values().stream().filter(room -> room.isTimerActive(now)).count();
  }
}
