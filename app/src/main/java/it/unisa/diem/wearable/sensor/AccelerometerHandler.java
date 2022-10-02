package it.unisa.diem.wearable.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class AccelerometerHandler extends AbstractSensorHandler {

    public AccelerometerHandler(SensorManager sensorManager, SensorEventListener sensorEventListener) {
        super(sensorManager, sensorEventListener);
    }

    @Override
    public Sensor registerListener() {
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(accelerometerSensor != null)
            sensorManager.registerListener(sensorEventListener, accelerometerSensor,
                    SENSOR_DELAY);
        return accelerometerSensor;
    }

    @Override
    public void unregisterListener() {
        sensorManager.unregisterListener(sensorEventListener);
    }
}
