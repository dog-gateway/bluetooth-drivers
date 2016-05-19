/**
 * 
 */
package org.doggateway.drivers.bluetooth.ble.temperatureandhumiditysensor.cc2650;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.info.BLEDeviceRegistration;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.devicecategory.TemperatureAndHumiditySensor;
import it.polito.elite.dog.core.library.model.state.HumidityMeasurementState;
import it.polito.elite.dog.core.library.model.state.TemperatureState;
import it.polito.elite.dog.core.library.model.statevalue.HumidityStateValue;
import it.polito.elite.dog.core.library.model.statevalue.TemperatureStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;

/**
 * @author bonino
 *
 */
public class CC2650TemperatureAndHumiditySensorDriverInstance
		extends BLEDriverInstance implements TemperatureAndHumiditySensor
{
	// the device peculiar data
	public static final String TEMPERATURE_HUMIDITY_SERVICE_UUID = "f000aa20-0451-4000-b000-000000000000";
	public static final String TEMPERATURE_HUMIDITY_CHARACTERISTIC_UUID = "f000aa21-0451-4000-b000-000000000000";
	public static final String TEMPERATURE_HUMIDITY_CONFIG_UUID = "f000aa22-0451-4000-b000-000000000000";

	public CC2650TemperatureAndHumiditySensorDriverInstance(
			BLENetwork bleNetwork, ControllableDevice device,
			String gwMacAddress, int pollingTimeMillis, BundleContext context)
	{
		// call the super class constructor
		super(bleNetwork, device, gwMacAddress, pollingTimeMillis,
				new LogHelper(context));

		// initialize the device status
		this.initializeStates();
	}

	@Override
	public void deleteGroup(Integer groupID)
	{
		// Not yet supported
	}

	@Override
	public DeviceStatus getState()
	{
		// provides back the current state of the device
		return this.currentState;
	}

	@Override
	public Measure<?, ?> getTemperature()
	{
		// provides back the current temperature
		return (Measure<?, ?>) this.currentState
				.getState(TemperatureState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
	}

	@Override
	public Measure<?, ?> getRelativeHumidity()
	{
		// provides back the current humidity
		return (Measure<?, ?>) this.currentState
				.getState(HumidityMeasurementState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
	}

	@Override
	public void notifyChangedRelativeHumidity(Measure<?, ?> relativeHumidity)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void storeGroup(Integer groupID)
	{
		// Not yet supported

	}

	@Override
	public void notifyLeftGroup(Integer groupNumber)
	{
		((TemperatureAndHumiditySensor) this.device)
				.notifyLeftGroup(groupNumber);

	}

	@Override
	public void notifyNewTemperatureValue(Measure<?, ?> temperatureValue)
	{
		((TemperatureAndHumiditySensor) this.device)
				.notifyNewTemperatureValue(temperatureValue);
	}

	@Override
	public void notifyJoinedGroup(Integer groupNumber)
	{
		((TemperatureAndHumiditySensor) this.device)
				.notifyJoinedGroup(groupNumber);

	}

	@Override
	public void updateStatus()
	{
		((Controllable) this.device).updateStatus();
	}

	@Override
	protected void specificConfiguration()
	{
		// prepare the device state map
		this.currentState = new DeviceStatus(device.getDeviceId());
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
		// only one characteristic is listened at now
		if (characteristicUUID.equals(
				CC2650TemperatureAndHumiditySensorDriverInstance.TEMPERATURE_HUMIDITY_CHARACTERISTIC_UUID))
		{
			// check if notification is enabled
			byte[] config = this.network.readValue(getDeviceMacAddress(),
					CC2650TemperatureAndHumiditySensorDriverInstance.TEMPERATURE_HUMIDITY_SERVICE_UUID,
					CC2650TemperatureAndHumiditySensorDriverInstance.TEMPERATURE_HUMIDITY_CONFIG_UUID);

			// if enabled do nothing
			if (!(config[0] == 0x01))
				this.enableCharacteristicsNotification();
			else
			{
				// do not ask any more!?!?

				// interpret the value
				int temperatureValueRaw = (0x0000ffff)&(value[0] + (value[1] << 8));
				int humidityValueRaw = (0x0000ffff)&(value[2] + (value[3] << 8));

				float temperatureCelsius = this
						.convertCelsius(temperatureValueRaw);
				float humidityPercent = this
						.convertHumidityPercent(humidityValueRaw);

				// update status and notify
				this.updateAndNotify((double) temperatureCelsius,
						(double) humidityPercent);
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
		TemperatureStateValue tValue = new TemperatureStateValue();
		tValue.setValue(DecimalMeasure.valueOf("0 " + SI.CELSIUS.toString()));

		// the initial state
		TemperatureState tState = new TemperatureState(tValue);

		// set the current state
		this.currentState.setState(TemperatureState.class.getSimpleName(),
				tState);

		// set the humidity state
		this.currentState.setState(
				HumidityMeasurementState.class.getSimpleName(),
				new HumidityMeasurementState(new HumidityStateValue()));
	}

	private void enableCharacteristicsNotification()
	{
		// WARNING: this probably fails if the device is not in range, we shall
		// probably address a way to enable characteristic notifications!
		byte[] enable = { 0x01 };
		this.network.writeValue(this.getDeviceMacAddress(),
				CC2650TemperatureAndHumiditySensorDriverInstance.TEMPERATURE_HUMIDITY_SERVICE_UUID,
				CC2650TemperatureAndHumiditySensorDriverInstance.TEMPERATURE_HUMIDITY_CONFIG_UUID,
				enable);

	}

	private float convertCelsius(int raw)
	{
		return (raw / 65536f)*165f - 40f;
	}

	private float convertHumidityPercent(int raw)
	{
		return (raw / 65536f) * 100;
	}

	/**
	 * Updates the current status of the device handled by this driver instance
	 * and notifies any change
	 * 
	 * @param value
	 *            The temperature value to use for updating the state
	 * @param unit
	 *            The unit of measure, "Celsius" in all currently implemented
	 *            profiles
	 */
	private void updateAndNotify(Double temperatureValue, Double humidityValue)
	{

		// treat the temperature as a measure
		DecimalMeasure<?> temperature = DecimalMeasure.valueOf(
				String.format("%.2f", temperatureValue) + " " + SI.CELSIUS);

		// update the current state
		this.currentState.getState(TemperatureState.class.getSimpleName())
				.getCurrentStateValue()[0].setValue(temperature);

		// treat the temperature as a measure
		DecimalMeasure<?> humidity = DecimalMeasure.valueOf(
				String.format("%.2f", humidityValue) + " %");

		// update the current state
		this.currentState.getState(HumidityMeasurementState.class.getSimpleName())
				.getCurrentStateValue()[0].setValue(humidity);

		// update the status (Monitor Admin)
		this.updateStatus();

		// notify the change
		this.notifyNewTemperatureValue(temperature);
		this.notifyChangedRelativeHumidity(humidity);

		// log
		this.logger.log(LogService.LOG_INFO, "Device: " + device.getDeviceId()
				+ " Temperature: " + temperature.toString() +" Humidity: "+humidity.toString());
	}

}
