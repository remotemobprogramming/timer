package sh.mob.timer.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {

  private static final Logger log = LogManager.getLogger();

  private final Map<String, Room> repository = new ConcurrentHashMap<>();

  private final RoomNameGenerator roomNameGenerator;

  public RoomRepository(RoomNameGenerator roomNameGenerator) {
    this.roomNameGenerator = roomNameGenerator;
  }

  Room get(String room) {
    return repository.computeIfAbsent(
        room,
        name -> {
          log.info("Created room {}", name);
          return new Room(name);
        });
  }

  @Scheduled(fixedRateString = "PT1M")
  void cleanUpUnusedRooms() {
    repository.forEach((key, room) -> room.removeOldTimerRequests());
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
        .mapToLong(room -> room.sink().currentSubscriberCount())
        .sum();
  }
}
