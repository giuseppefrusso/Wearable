package it.unisa.diem.wearable.sensor;

import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public abstract class AbstractSensorHandler implements SensorHandler {

    protected SensorManager sensorManager;
    protected SensorEventListener sensorEventListener;
    protected static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL;

    public AbstractSensorHandler(SensorManager sensorManager, SensorEventListener sensorEventListener) {
        this.sensorManager = sensorManager;
        this.sensorEventListener = sensorEventListener;
    }
}
