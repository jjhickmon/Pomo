import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Settings {
  Properties configFile;
  public Settings(){
    configFile = new Properties();
    try {
      FileInputStream file = new FileInputStream("config.properties");
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
