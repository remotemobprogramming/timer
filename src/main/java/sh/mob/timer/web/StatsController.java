package sh.mob.timer.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/stats")
public class StatsController {

  private final Stats stats;

  public StatsController(Stats stats) {
    this.stats = stats;
  }

  @GetMapping
  public String stats(Model model) {
    model.addAttribute("stats", stats);
    return "stats";
  }

}
