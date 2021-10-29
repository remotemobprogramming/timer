package sh.mob.timer.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Scanner;
import org.springframework.stereotype.Component;

@Component
public class RoomNameGenerator {

  private final Random random = new Random();
  private final List<String> animals = loadCsvFile("animals.csv");
  private final List<String> adjectives = loadCsvFile("adjectives.csv");

  String randomName() {
    return String.join("-", randomAdjective(), randomAnimal(), Integer.toString(randomNumber()));
  }

  private String randomAnimal() {
    return animals.get(random.nextInt(animals.size()));
  }

  private String randomAdjective() {
    return adjectives.get(random.nextInt(adjectives.size()));
  }

  private int randomNumber() {
    return random.nextInt(90) + 10;
  }

  private static List<String> loadCsvFile(String csvFile) {
    var resourceAsStream = RoomNameGenerator.class.getResourceAsStream("/" + csvFile);
    if (resourceAsStream == null) {
      return new ArrayList<>();
    }

    var scanner = new Scanner(resourceAsStream);
    var result = new ArrayList<String>();
    while (scanner.hasNextLine()) {
      result.add(scanner.nextLine().trim().toLowerCase(Locale.ROOT));
    }
    return result;
  }
}
