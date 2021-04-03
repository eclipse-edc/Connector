package com.atlas.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

public class PropertyCache
{
  private final Properties configProp = new Properties();

  private PropertyCache()
  {
    //Private constructor to restrict new instances
    InputStream in = this.getClass().getClassLoader().getResourceAsStream("atlas-application.properties");
    System.out.println("Reading all properties from the file");
    try {
      configProp.load(in);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  //Bill Pugh Solution for singleton pattern
  private static class LazyHolder
  {
    private static final PropertyCache INSTANCE = new PropertyCache();
  }

  public static PropertyCache getInstance()
  {
    return LazyHolder.INSTANCE;
  }

  public String getProperty(String key){
    return configProp.getProperty(key);
  }

  public Set<String> getAllPropertyNames(){
    return configProp.stringPropertyNames();
  }

  public boolean containsKey(String key){
    return configProp.containsKey(key);
  }
}