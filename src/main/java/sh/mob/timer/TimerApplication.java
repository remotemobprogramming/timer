package sh.mob.timer;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TimerApplication {

  public static void main(String[] args) {
    SpringApplication.run(TimerApplication.class, args);
  }

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

}
