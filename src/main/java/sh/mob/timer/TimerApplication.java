package sh.mob.timer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TimerApplication {

  public static void main(String[] args) {
    System.out.println("STARTING");
    SpringApplication.run(TimerApplication.class, args);
  }

}
