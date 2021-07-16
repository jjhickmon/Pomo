package src.backend;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.io.File;

public class Settings {
  Properties configFile;
  public Settings(){
    configFile = new Properties();
    try {
      File config = new File("src/backend/config.properties");
      FileInputStream file = new FileInputStream(config);
      configFile.load(file);
    } catch(Exception eta) {
      eta.printStackTrace();
    }
  }

  public String getProperty(String key){
    String value = this.configFile.getProperty(key);
    return value;
  }
}
