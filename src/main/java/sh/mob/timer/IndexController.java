package sh.mob.timer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
public class IndexController {

  @GetMapping
  public String index() {
    System.out.println("INDEX");
    return "index";
  }

  @PostMapping
  public String post(@RequestParam("room") String room) {
    System.out.println("POST");
    return "redirect:/" + room;
  }
}
