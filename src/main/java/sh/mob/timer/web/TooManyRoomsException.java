package sh.mob.timer.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class TooManyRoomsException extends RuntimeException {

  public TooManyRoomsException() {
    super("The maximum number of rooms has been reached");
  }
}
