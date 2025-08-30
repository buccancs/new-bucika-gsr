package com.example.thermal_lite.camera.task;

public enum DeviceState {
    OPEN("open", 0),
    CLOSED("closed", 1),
    RESUMED("resumed", 2),
    PAUSED("paused", 3),
    UPDATE_VERSION("closed", 4),
    NONE("none", 5);

    private String value;
    private int id;

    DeviceState(String value, int id) {
        this.id = id;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
