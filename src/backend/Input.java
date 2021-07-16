package src.backend;

import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.time.Instant;
import src.Display;

import java.time.Duration;

public class Input implements KeyListener{
  Display display = new Display();
  /**
   * Checks if the any key is released
   * Overrides the default KeyListener method
   * @param e The key release
   */
  @Override
  public void keyReleased(KeyEvent e){}

  /**
   * Checks if the any key is pressed
   * Overrides the default KeyListener method
   * @param e The key press
   */
  @Override
  public void keyPressed(KeyEvent e){
    if(e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE){
      display.paused = !display.paused;
      if(display.paused){
        display.start_pause = Instant.now();
      } else {
          if(display.start_study == null){
            display.start_study = Instant.now();
            display.start_pause = Instant.now();
            display.studying = true;
          }
          long dt = Duration.between(display.start_pause, Instant.now()).getSeconds();
          display.pause_time = display.pause_time.plusSeconds(dt);
      }
    }
    if(e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_Q){
      display.quit();
    }
  }

  /**
   * Checks if the any key is typed
   * Overrides the default KeyListener method
   * @param e The key typed event
   */
  @Override
  public void keyTyped(KeyEvent e){}
}
