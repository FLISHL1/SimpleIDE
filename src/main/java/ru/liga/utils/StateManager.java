package ru.liga.utils;

import ru.liga.config.PropertyState;

public class StateManager {
    private final PropertyState propertyState;

    public StateManager() {
        propertyState = new PropertyState();
    }

    public void saveLastOpenedFile(String filePath) {
        propertyState.setProperty("lastFile", filePath);
        propertyState.setStore("Application State");
        propertyState.updateProperties();
    }
    public void saveAppState(Integer fontSize) {
        propertyState.setProperty("fontSize", String.valueOf(fontSize));

        propertyState.setStore("Application State");
        propertyState.updateProperties();
    }

    public String getFontSize(){
        return propertyState.getProperty("fontSize");
    }

    public String getLastFile(){
        return propertyState.getProperty("lastFile");
    }

    public String getProperty(String key) {
        return propertyState.getProperty(key);
    }
}
