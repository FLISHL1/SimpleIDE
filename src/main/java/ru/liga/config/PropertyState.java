package ru.liga.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyState {
    private final String CONFIG_FILE = "states.properties";
    private Properties PROPERTIES;
    private final String PATH_TO_FILE = "src/main/resources/";

    public PropertyState() {
        PROPERTIES = updateProperties();
    }

    public Properties updateProperties() {
        try (FileInputStream fileInputStream = new FileInputStream(PATH_TO_FILE + CONFIG_FILE)) {
            PROPERTIES = new Properties();
            PROPERTIES.load(fileInputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return PROPERTIES;
    }

    public void setStore(String comment){
        try (FileOutputStream fos = new FileOutputStream(PATH_TO_FILE + CONFIG_FILE)) {
            PROPERTIES.store(fos, comment);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getProperty(String key) {
        return PROPERTIES.getProperty(key);
    }

    public void setProperty(String key, String value) {
        PROPERTIES.setProperty(key, value);
    }
}
