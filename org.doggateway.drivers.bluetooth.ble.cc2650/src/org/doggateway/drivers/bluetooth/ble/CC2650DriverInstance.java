package org.doggateway.drivers.bluetooth.ble;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.info.BLEDeviceRegistration;
import org.doggateway.drivers.bluetooth.ble.network.info.CharacteristicMonitorSpec;
import org.doggateway.drivers.bluetooth.ble.network.info.ServiceMonitorSpec;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.CC2650SensorTag;
import it.polito.elite.dog.core.library.model.state.ContinuousState;
import it.polito.elite.dog.core.library.model.state.HumidityMeasurementState;
import it.polito.elite.dog.core.library.model.state.LightIntensityState;
import it.polito.elite.dog.core.library.model.state.MultipleOnOffState;
import it.polito.elite.dog.core.library.model.state.MultipleTemperatureState;
import it.polito.elite.dog.core.library.model.state.PressureState;
import it.polito.elite.dog.core.library.model.state.TridimensionalAccelerationState;
import it.polito.elite.dog.core.library.model.state.TridimensionalGyroscopeState;
import it.polito.elite.dog.core.library.model.state.TridimensionalMagnetometerState;
import it.polito.elite.dog.core.library.model.statevalue.AccelerationStateValue;
import it.polito.elite.dog.core.library.model.statevalue.DiscreteValue;
import it.polito.elite.dog.core.library.model.statevalue.GyroscopeStateValue;
import it.polito.elite.dog.core.library.model.statevalue.HumidityStateValue;
import it.polito.elite.dog.core.library.model.statevalue.LevelStateValue;
import it.polito.elite.dog.core.library.model.statevalue.MagnetometerStateValue;
import it.polito.elite.dog.core.library.model.statevalue.MultipleTemperatureStateValue;
import it.polito.elite.dog.core.library.model.statevalue.OffStateValue;
import it.polito.elite.dog.core.library.model.statevalue.PressureStateValue;
import it.polito.elite.dog.core.library.model.statevalue.StateValue;
import it.polito.elite.dog.core.library.model.statevalue.TemperatureStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;

public class CC2650DriverInstance extends BLEDriverInstance
		implements CC2650SensorTag
{
	// constants for direct configuration of sensors
	public static final String MOVEMENT_SENSOR_SERVICE_UUID = "f000aa80-0451-4000-b000-000000000000";
	public static final String MOVEMENT_SENSOR_CHAR_UUID = "000aa81-0451-4000-b000-000000000000";
	public static final String MOVEMENT_SENSOR_CONFIG_UUID = "f000aa82-0451-4000-b000-000000000000";
	public static final byte[] MOVEMENT_SENSOR_CONFIG = { (byte) 0xff,
			(byte) 0x02 };

	public static final String IR_SENSOR_SERVICE_UUID = "f000aa00-0451-4000-b000-000000000000";
	public static final String IR_SENSOR_CHAR_UUID = "f000aa01-0451-4000-b000-000000000000";
	public static final String IR_SENSOR_CONFIG_UUID = "f000aa02-0451-4000-b000-000000000000";
	public static final byte[] IR_SENSOR_CONFIG = { (byte) 0x01 };

	public static final String HUMIDITY_SENSOR_SERVICE_UUID = "f000aa20-0451-4000-b000-000000000000";
	public static final String HUMIDITY_SENSOR_CHAR_UUID = "f000aa21-0451-4000-b000-000000000000";
	public static final String HUMIDITY_SENSOR_CONFIG_UUID = "f000aa22-0451-4000-b000-000000000000";
	public static final byte[] HUMIDITY_SENSOR_CONFIG = { (byte) 0x01 };

	public static final String PRESSURE_SENSOR_SERVICE_UUID = "f000aa40-0451-4000-b000-000000000000";
	public static final String PRESSURE_SENSOR_CHAR_UUID = "f000aa41-0451-4000-b000-000000000000";
	public static final String PRESSURE_SENSOR_CONFIG_UUID = "f000aa42-0451-4000-b000-000000000000";
	public static final byte[] PRESSURE_SENSOR_CONFIG = { (byte) 0x01 };

	public static final String OPTICAL_SENSOR_SERVICE_UUID = "f000aa70-0451-4000-b000-000000000000";
	public static final String OPTICAL_SENSOR_CHAR_UUID = "f000aa71-0451-4000-b000-000000000000";
	public static final String OPTICAL_SENSOR_CONFIG_UUID = "f000aa72-0451-4000-b000-000000000000";
	public static final byte[] OPTICAL_SENSOR_CONFIG = { (byte) 0x01 };

	// the movement polling rate
	private int movementPollingTimeMillis;

	public CC2650DriverInstance(BLENetwork network, ControllableDevice device,
			String gwMacAddress, int pollingTimeMillis,
			int movementPollingTimeMillis, BundleContext context)
	{
		super(network, device, gwMacAddress, pollingTimeMillis,
				new LogHelper(context));

		this.movementPollingTimeMillis = movementPollingTimeMillis;

		// initialize the device status
		this.initializeStates();
	}

	@Override
	public Measure<?, ?> getPressure()
	{
		return (Measure<?, ?>) this.currentState
				.getState(PressureState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
	}

	@Override
	public DeviceStatus getState()
	{
		return this.currentState;
	}

	@Override
	public Measure<?, ?> getLuminance()
	{
		return (Measure<?, ?>) this.currentState
				.getState(LightIntensityState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
	}

	@Override
	public Measure<?, ?> getRelativeHumidity()
	{
		return (Measure<?, ?>) this.currentState
				.getState(HumidityMeasurementState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
	}

	@Override
	public Measure<?, ?> getTemperatureFrom(String sensorURI)
	{
		Measure<?, ?> tValue = null;
		// get all the temperature values
		TemperatureStateValue values[] = (TemperatureStateValue[]) this.currentState
				.getState(MultipleTemperatureStateValue.class.getSimpleName())
				.getCurrentStateValue();

		// iterate to find the right one
		boolean found = false;
		for (int i = 0; (i < values.length) && (!found); i++)
		{
			String featureName = (String) values[i].getFeatures()
					.get("sensorID");
			if ((featureName != null) && (!featureName.isEmpty())
					&& (featureName.equals(sensorURI)))
			{
				// get the value
				tValue = (Measure<?, ?>) values[i].getValue();

				// break the cycle
				found = true;
			}
		}
		return tValue;
	}

	@Override
	public void notifyNew3DAccelerationValue(Measure<?,?> accZ, Measure<?,?> accX,
			Measure<?,?> accY)
	{
		((CC2650SensorTag) this).notifyNew3DAccelerationValue(accZ, accX, accY);
	}

	@Override
	public void notifyReleased(String buttonID)
	{
		((CC2650SensorTag) this).notifyReleased(buttonID);
	}

	@Override
	public void notifyNew3DMagnetometerValue(Measure<?,?> magY, Measure<?,?> magZ,
			Measure<?,?> magX)
	{
		((CC2650SensorTag) this).notifyNew3DMagnetometerValue(magY, magZ, magX);
	}

	@Override
	public void notifyChangedRelativeHumidity(Measure<?, ?> relativeHumidity)
	{
		((CC2650SensorTag) this)
				.notifyChangedRelativeHumidity(relativeHumidity);
	}

	@Override
	public void notifyNewPressureValue(Measure<?, ?> pressureValue)
	{
		((CC2650SensorTag) this).notifyNewPressureValue(pressureValue);
	}

	@Override
	public void notifyNew3DGyroscopeValue(Measure<?,?> gyroX, Measure<?,?> gyroY,
			Measure<?,?> gyroZ)
	{
		((CC2650SensorTag) this).notifyNew3DGyroscopeValue(gyroX, gyroY, gyroZ);
	}

	@Override
	public void notifyChangedTemperatureAt(Measure<?, ?> temperatureValue,
			String sensorID)
	{
		((CC2650SensorTag) this).notifyChangedTemperatureAt(temperatureValue,
				sensorID);
	}

	@Override
	public void notifyPressed(String buttonID)
	{
		((CC2650SensorTag) this).notifyPressed(buttonID);
	}

	@Override
	public void notifyNewLuminosityValue(Measure<?, ?> luminosityValue)
	{
		((CC2650SensorTag) this).notifyNewLuminosityValue(luminosityValue);
	}

	@Override
	public void updateStatus()
	{
		((CC2650SensorTag) this).updateStatus();
	}

	@Override
	protected void specificConfiguration()
	{
		// prepare the device state map
		this.currentState = new DeviceStatus(device.getDeviceId());

		// check if a dedicated movement time millis has been specified
		if (this.movementPollingTimeMillis != this.pollingTimeMillis)
		{
			// set-up movement polling time in ble device registrations
			ServiceMonitorSpec movementSpec = this.bleDevReg.getServiceSpec(
					CC2650DriverInstance.MOVEMENT_SENSOR_SERVICE_UUID);

			// check not null
			if (movementSpec != null)
			{
				// iterate over char specs
				for (CharacteristicMonitorSpec charSpec : movementSpec
						.getCharacteristicSpecs())
				{
					charSpec.setMaximumAcceptablePollingTimeMillis(
							movementPollingTimeMillis);
				}
			}
		}
	}

	@Override
	protected void addToNetworkDriver(BLEDeviceRegistration bleDevReg)
	{
		// add the ble registration to the driver
		this.network.addDeviceRegistration(bleDevReg);

		// try turning on the notification at ble level
		// can be done on the sole basis of configuration information
		// but since this is a device-specific driver, we can use explicit
		// information here!?!?
		this.enableCharacteristicsNotification();
	}

	@Override
	public void newMessageFromHouse(String characteristicUUID,
			String serviceUUID, byte[] value)
	{
		// message shall interpreted differently depending on service /
		// characteristic UUID
		switch (characteristicUUID)
		{
			case IR_SENSOR_CHAR_UUID:
			{
				this.handleIRSensorData(value);
				break;
			}
			case HUMIDITY_SENSOR_CHAR_UUID:
			{
				this.handleHumidityData(value);
				break;
			}
			case MOVEMENT_SENSOR_CHAR_UUID:
			{
				this.handleMovementData(value);
				break;
			}
		}

	}

	private void initializeStates()
	{
		// add alias for %
		Unit.ONE.alternate("%");
		// update the CELSIUS notation
		UnitFormat uf = UnitFormat.getInstance();
		uf.label(SI.CELSIUS, "C");
		uf.alias(SI.CELSIUS, "C");

		// the initial temperature value
		MultipleTemperatureStateValue tValueObject = new MultipleTemperatureStateValue();
		tValueObject.setFeature("sensorID", "IRObject");
		tValueObject
				.setValue(DecimalMeasure.valueOf("0 " + SI.CELSIUS.toString()));

		MultipleTemperatureStateValue tValueAmbient = new MultipleTemperatureStateValue();
		tValueAmbient.setFeature("sensorID", "IRAmbient");
		tValueAmbient
				.setValue(DecimalMeasure.valueOf("0 " + SI.CELSIUS.toString()));

		MultipleTemperatureStateValue tValueHumidity = new MultipleTemperatureStateValue();
		tValueHumidity.setFeature("sensorID", "Humidity");
		tValueHumidity
				.setValue(DecimalMeasure.valueOf("0 " + SI.CELSIUS.toString()));

		MultipleTemperatureStateValue tValuePressure = new MultipleTemperatureStateValue();
		tValuePressure.setFeature("sensorID", "Pressure");
		tValuePressure
				.setValue(DecimalMeasure.valueOf("0 " + SI.CELSIUS.toString()));

		// the initial state
		MultipleTemperatureState tState = (MultipleTemperatureState) new ContinuousState(
				(StateValue) tValueObject, (StateValue) tValueAmbient,
				(StateValue) tValueHumidity, (StateValue) tValuePressure);

		// set the current temperature state
		this.currentState.setState(
				MultipleTemperatureState.class.getSimpleName(), tState);

		// set the humidity state
		this.currentState.setState(
				HumidityMeasurementState.class.getSimpleName(),
				new HumidityMeasurementState(new HumidityStateValue()));

		// set the pressure state
		this.currentState.setState(PressureState.class.getSimpleName(),
				new PressureState(new PressureStateValue()));

		// set the luminosity state
		this.currentState.setState(LightIntensityState.class.getSimpleName(),
				new LightIntensityState(new LevelStateValue()));

		// set the button state
		DiscreteValue value[] = new DiscreteValue[2];

		for (int i = 0; i < 2; i++)
		{
			// handle multiple structurally equal states

			// build the default off state value
			OffStateValue offValue = new OffStateValue();

			// set the button id parameter
			offValue.setFeature("buttonID", i + 1);

			value[i] = offValue;
		}

		// build the i-th button state
		MultipleOnOffState offState = new MultipleOnOffState(value);

		// store the state
		this.currentState.setState(MultipleOnOffState.class.getSimpleName(),
				offState);

		// set the 3D acceleration state
		AccelerationStateValue accX = new AccelerationStateValue();
		accX.setFeature("axisID", "x");
		accX.setValue(DecimalMeasure.valueOf("0 " + NonSI.G));

		AccelerationStateValue accY = new AccelerationStateValue();
		accY.setFeature("axisID", "y");
		accY.setValue(DecimalMeasure.valueOf("0 " + NonSI.G));

		AccelerationStateValue accZ = new AccelerationStateValue();
		accZ.setFeature("axisID", "z");
		accZ.setValue(DecimalMeasure.valueOf("0 " + NonSI.G));

		this.currentState.setState(
				TridimensionalAccelerationState.class.getSimpleName(),
				new TridimensionalAccelerationState(accX, accY, accZ));

		// set the 3D gyroscope state
		GyroscopeStateValue gyroX = new GyroscopeStateValue();
		gyroX.setFeature("axisID", "x");
		gyroX.setValue(DecimalMeasure.valueOf("0 " + NonSI.DEGREE_ANGLE));

		GyroscopeStateValue gyroY = new GyroscopeStateValue();
		gyroY.setFeature("axisID", "y");
		gyroY.setValue(DecimalMeasure.valueOf("0 " + NonSI.DEGREE_ANGLE));

		GyroscopeStateValue gyroZ = new GyroscopeStateValue();
		gyroZ.setFeature("axisID", "z");
		gyroZ.setValue(DecimalMeasure.valueOf("0 " + NonSI.DEGREE_ANGLE));

		this.currentState.setState(
				TridimensionalGyroscopeState.class.getSimpleName(),
				new TridimensionalGyroscopeState(gyroX, gyroY, gyroZ));

		// set the 3D magnetometer state
		MagnetometerStateValue magX = new MagnetometerStateValue();
		magX.setFeature("axisID", "x");
		magX.setValue(DecimalMeasure.valueOf("0 " + SI.MICRO(SI.TESLA)));

		MagnetometerStateValue magY = new MagnetometerStateValue();
		magY.setFeature("axisID", "y");
		magY.setValue(DecimalMeasure.valueOf("0 " + SI.MICRO(SI.TESLA)));

		MagnetometerStateValue magZ = new MagnetometerStateValue();
		magZ.setFeature("axisID", "z");
		magZ.setValue(DecimalMeasure.valueOf("0 " + SI.MICRO(SI.TESLA)));

		this.currentState.setState(
				TridimensionalMagnetometerState.class.getSimpleName(),
				new TridimensionalMagnetometerState(magX, magY, magZ));
	}

	private void enableCharacteristicsNotification()
	{
		// for all sensors on the device, write values to enable notifications
		// on the given sensor characteristic
		this.network.writeValue(this.getDeviceMacAddress(),
				CC2650DriverInstance.MOVEMENT_SENSOR_SERVICE_UUID,
				CC2650DriverInstance.MOVEMENT_SENSOR_CONFIG_UUID,
				CC2650DriverInstance.MOVEMENT_SENSOR_CONFIG);
		this.network.writeValue(this.getDeviceMacAddress(),
				CC2650DriverInstance.IR_SENSOR_SERVICE_UUID,
				CC2650DriverInstance.IR_SENSOR_CONFIG_UUID,
				CC2650DriverInstance.IR_SENSOR_CONFIG);
		this.network.writeValue(this.getDeviceMacAddress(),
				CC2650DriverInstance.HUMIDITY_SENSOR_SERVICE_UUID,
				CC2650DriverInstance.HUMIDITY_SENSOR_CONFIG_UUID,
				CC2650DriverInstance.HUMIDITY_SENSOR_CONFIG);
		this.network.writeValue(this.getDeviceMacAddress(),
				CC2650DriverInstance.OPTICAL_SENSOR_SERVICE_UUID,
				CC2650DriverInstance.PRESSURE_SENSOR_CONFIG_UUID,
				CC2650DriverInstance.PRESSURE_SENSOR_CONFIG);
		this.network.writeValue(this.getDeviceMacAddress(),
				CC2650DriverInstance.OPTICAL_SENSOR_SERVICE_UUID,
				CC2650DriverInstance.OPTICAL_SENSOR_CONFIG_UUID,
				CC2650DriverInstance.OPTICAL_SENSOR_CONFIG);
	}

	private void handleIRSensorData(byte[] value)
	{
		// check if notification is enabled
		byte[] config = this.network.readValue(getDeviceMacAddress(),
				CC2650DriverInstance.IR_SENSOR_SERVICE_UUID,
				CC2650DriverInstance.IR_SENSOR_CONFIG_UUID);

		// if enabled do nothing
		if (!(config[0] == CC2650DriverInstance.IR_SENSOR_CONFIG[0]))
		{
			this.network.writeValue(this.getDeviceMacAddress(),
					CC2650DriverInstance.IR_SENSOR_SERVICE_UUID,
					CC2650DriverInstance.IR_SENSOR_CONFIG_UUID,
					CC2650DriverInstance.IR_SENSOR_CONFIG);
		}
		else
		{
			// interpret the value
			int ambientTempRaw = (0x0000ffff) & (value[2] + (value[3] << 8));
			float ambientTempCelsius = ambientTempRaw / 128f;

			// update the status and notify
			this.updateAndNotifyTemperature("IRAmbient", ambientTempCelsius);

			// interpret the value
			int objectTempRaw = (0x0000ffff) & (value[0] + (value[1] << 8));
			float objectTempCelsius = objectTempRaw / 128f;

			// update the status and notify
			this.updateAndNotifyTemperature("IRObject", objectTempCelsius);
		}
	}

	private void handleHumidityData(byte[] value)
	{
		// check if notification is enabled
		byte[] config = this.network.readValue(getDeviceMacAddress(),
				CC2650DriverInstance.HUMIDITY_SENSOR_SERVICE_UUID,
				CC2650DriverInstance.HUMIDITY_SENSOR_CONFIG_UUID);

		// if enabled do nothing
		if (!(config[0] == CC2650DriverInstance.HUMIDITY_SENSOR_CONFIG[0]))
		{
			this.network.writeValue(this.getDeviceMacAddress(),
					CC2650DriverInstance.HUMIDITY_SENSOR_SERVICE_UUID,
					CC2650DriverInstance.HUMIDITY_SENSOR_CONFIG_UUID,
					CC2650DriverInstance.HUMIDITY_SENSOR_CONFIG);
		}
		else
		{
			// interpret the value
			int temperatureValueRaw = (0x0000ffff)
					& (value[0] + (value[1] << 8));
			int humidityValueRaw = (0x0000ffff) & (value[2] + (value[3] << 8));

			float temperatureCelsius = (temperatureValueRaw / 65536f) * 165f
					- 40f;
			float humidityPercent = (humidityValueRaw / 65536f) * 100;

			// update and notify
			this.updateAndNotifyTemperature("Humidity", temperatureCelsius);

			this.updateAndNotifyHumidity(humidityPercent);
		}

	}

	private void handleMovementData(byte[] value)
	{// check if notification is enabled
		byte[] config = this.network.readValue(getDeviceMacAddress(),
				CC2650DriverInstance.MOVEMENT_SENSOR_SERVICE_UUID,
				CC2650DriverInstance.MOVEMENT_SENSOR_CONFIG_UUID);
		if ((config[0] != CC2650DriverInstance.MOVEMENT_SENSOR_CONFIG[0])
				|| (config[1] != CC2650DriverInstance.MOVEMENT_SENSOR_CONFIG[1]))
		{
			this.network.writeValue(this.getDeviceMacAddress(),
					CC2650DriverInstance.MOVEMENT_SENSOR_SERVICE_UUID,
					CC2650DriverInstance.MOVEMENT_SENSOR_CONFIG_UUID,
					CC2650DriverInstance.MOVEMENT_SENSOR_CONFIG);
		}
		else
		{
			// interpret the data
			int gyroXRaw = (value[0] + (value[1] << 8)); // signed integer
			int gyroYRaw = (value[2] + (value[3] << 8));
			int gyroZRaw = (value[4] + (value[5] << 8));
			int accXRaw = (value[6] + (value[7] << 8));
			int accYRaw = (value[8] + (value[9] << 8));
			int accZRaw = (value[10] + (value[11] << 8));
			int magXRaw = (value[12] + (value[13] << 8));
			int magYRaw = (value[14] + (value[15] << 8));
			int magZRaw = (value[16] + (value[18] << 8));

			// convert data
			this.updateAndNotifyGyroscope(this.gyroConvert(gyroXRaw),
					this.gyroConvert(gyroYRaw), this.gyroConvert(gyroZRaw));
		}
	}

	private float gyroConvert(int value)
	{
		return (value * 1.0f) / (65536 / 500);
	}

	private void updateAndNotifyTemperature(String sensorId, float value)
	{
		// treat the temperature as a measure
		DecimalMeasure<?> temperature = DecimalMeasure
				.valueOf(String.format("%.2f", value) + " " + SI.CELSIUS);

		// update the current state
		StateValue values[] = this.currentState
				.getState(MultipleTemperatureState.class.getSimpleName())
				.getCurrentStateValue();

		// search for the right value
		boolean found = false;
		for (int i = 0; (i < values.length) && (!found); i++)
		{
			if (values[i].getFeatures().get("sensorID").equals(sensorId))
			{
				// update the value
				values[i].setValue(temperature);

				// found
				found = true;
			}
		}

		// if found, update the status and notify
		// update the status (Monitor Admin)
		this.updateStatus();

		// notify
		this.notifyChangedTemperatureAt(temperature, sensorId);

		// log
		this.logger.log(LogService.LOG_INFO, "Device " + device.getDeviceId()
				+ "[" + sensorId + "] temperature " + temperature.toString());
	}

	private void updateAndNotifyHumidity(float humidityValue)
	{
		// treat the temperature as a measure
		DecimalMeasure<?> humidity = DecimalMeasure
				.valueOf(String.format("%.2f", humidityValue) + " %");

		// update the current state
		this.currentState
				.getState(HumidityMeasurementState.class.getSimpleName())
				.getCurrentStateValue()[0].setValue(humidity);

		// update the status (Monitor Admin)
		this.updateStatus();

		// notify
		this.notifyChangedRelativeHumidity(humidity);

		// log
		this.logger.log(LogService.LOG_INFO, "Device: " + device.getDeviceId()
				+ " Humidity: " + humidity.toString());
	}

	private void updateAndNotifyGyroscope(float gyroX, float gyroY, float gyroZ)
	{
		DecimalMeasure<?> gyroXDeg = DecimalMeasure
				.valueOf(gyroX + " " + NonSI.DEGREE_ANGLE);
		DecimalMeasure<?> gyroYDeg = DecimalMeasure
				.valueOf(gyroY + " " + NonSI.DEGREE_ANGLE);
		DecimalMeasure<?> gyroZDeg = DecimalMeasure
				.valueOf(gyroZ + " " + NonSI.DEGREE_ANGLE);

		// set the 3D gyroscope state
		GyroscopeStateValue gyroXValue = new GyroscopeStateValue();
		gyroXValue.setFeature("axisID", "x");
		gyroXValue.setValue(gyroXDeg);

		GyroscopeStateValue gyroYValue = new GyroscopeStateValue();
		gyroYValue.setFeature("axisID", "y");
		gyroYValue.setValue(gyroYDeg);

		GyroscopeStateValue gyroZValue = new GyroscopeStateValue();
		gyroZValue.setFeature("axisID", "z");
		gyroZValue.setValue(gyroZDeg);

		this.currentState.setState(
				TridimensionalGyroscopeState.class.getSimpleName(),
				new TridimensionalGyroscopeState(gyroXValue, gyroYValue,
						gyroZValue));

		// update the status (Monitor Admin)
		this.updateStatus();
		
		//notify
		this.notifyNew3DGyroscopeValue(gyroXDeg, gyroYDeg, gyroZDeg);
		
		//log
		this.logger.log(LogService.LOG_INFO, "Device: " + device.getDeviceId()
		+ " gyroX: " + gyroXDeg.toString()+ " gyroY: "+gyroYDeg.toString()+" gyroZ: "+gyroZDeg.toString());
	}
}
