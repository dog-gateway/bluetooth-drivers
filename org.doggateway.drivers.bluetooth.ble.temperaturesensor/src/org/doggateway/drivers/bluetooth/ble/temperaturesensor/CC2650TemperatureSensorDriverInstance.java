/*
 * Dog - Bluetooth Low Energy Texas Instruments CC2650 Sensor Tag - IR Sensor driver
 * 
 * Copyright (c) 2016 Dario Bonino 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package org.doggateway.drivers.bluetooth.ble.temperaturesensor;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.unit.SI;
import javax.measure.unit.UnitFormat;

import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.info.BLEDeviceRegistration;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.CC2650IRSensor;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.devicecategory.SingleTemperatureSensor;
import it.polito.elite.dog.core.library.model.state.MultipleTemperatureState;
import it.polito.elite.dog.core.library.model.statevalue.StateValue;
import it.polito.elite.dog.core.library.model.statevalue.TemperatureStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;

/**
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public class CC2650TemperatureSensorDriverInstance extends BLEDriverInstance
		implements CC2650IRSensor
{

	public static final String SERVICE_UUID = "f000aa00-0451-4000-b000-000000000000";
	public static final String CHARACTERISTIC_UUID = "f000aa01-0451-4000-b000-000000000000";
	public static final String CONFIG_UUID = "f000aa02-0451-4000-b000-000000000000";

	public CC2650TemperatureSensorDriverInstance(BLENetwork bleNetwork,
			ControllableDevice device, String gwMacAddress,
			int pollingTimeMillis, BundleContext context)
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
	public void storeGroup(Integer groupID)
	{
		// Not yet supported

	}

	@Override
	public void notifyLeftGroup(Integer groupNumber)
	{
		((SingleTemperatureSensor) this.device).notifyLeftGroup(groupNumber);

	}

	@Override
	public void notifyJoinedGroup(Integer groupNumber)
	{
		((SingleTemperatureSensor) this.device).notifyJoinedGroup(groupNumber);

	}

	@Override
	public Measure<?, ?> getTemperatureFrom(String sensorURI)
	{
		Measure<?, ?> temperature = null;
		StateValue[] values = this.currentState
				.getState(MultipleTemperatureState.class.getSimpleName())
				.getCurrentStateValue();

		//search for the temperature value of the sensor identified by the given id
		boolean found = false;
		for (int i = 0; (i < values.length) && (!found); i++)
		{
			if (values[i].getFeatures().get("sensorID").equals(sensorURI))
			{
				found = true;
				temperature = (Measure<?, ?>) values[i].getValue();
			}
		}
		return temperature;
	}

	@Override
	public void notifyChangedTemperatureAt(Measure<?, ?> temperatureValue,
			String sensorID)
	{
		((CC2650IRSensor) this.device)
				.notifyChangedTemperatureAt(temperatureValue, sensorID);

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
				CC2650TemperatureSensorDriverInstance.CHARACTERISTIC_UUID))
		{

			// do not ask any more!?!?

			// interpret the value
			int ambientTempRaw = (0x0000ffff) & (value[2] + (value[3] << 8));
			float ambientTempCelsius = this.convertCelsius(ambientTempRaw);
			
			int objectTempRaw = (0x0000ffff) & (value[0] + (value[1] << 8));
			float objectTempCelsius = this.convertCelsius(objectTempRaw);

			// update status and notify
			this.updateAndNotify((double) ambientTempCelsius, "ambient");
			this.updateAndNotify((double) objectTempCelsius, "object");

		}

	}

	private void initializeStates()
	{
		// update the CELSIUS notation
		UnitFormat uf = UnitFormat.getInstance();
		uf.label(SI.CELSIUS, "C");
		uf.alias(SI.CELSIUS, "C");

		// the initial temperature value
		TemperatureStateValue tValue1 = new TemperatureStateValue();
		tValue1.setValue(DecimalMeasure.valueOf("0 " + SI.CELSIUS.toString()));
		tValue1.setFeature("sensorID", "ambient");
		
		TemperatureStateValue tValue2 = new TemperatureStateValue();
		tValue2.setValue(DecimalMeasure.valueOf("0 " + SI.CELSIUS.toString()));
		tValue2.setFeature("sensorID", "object");

		// the initial state
		MultipleTemperatureState tState = new MultipleTemperatureState(tValue1, tValue2);

		// set the current state
		this.currentState.setState(MultipleTemperatureState.class.getSimpleName(),
				tState);
	}

	private void enableCharacteristicsNotification()
	{
		// WARNING: this probably fails if the device is not in range, we shall
		// probably address a way to enable characteristic notifications!
		byte[] enable = { 0x01 };
		this.network.writeValue(this.getDeviceMacAddress(),
				CC2650TemperatureSensorDriverInstance.SERVICE_UUID,
				CC2650TemperatureSensorDriverInstance.CONFIG_UUID, enable);

	}

	private float convertCelsius(int raw)
	{
		return raw / 128f;
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
	private void updateAndNotify(Double value, String sensorID)
	{

		// treat the temperature as a measure
		DecimalMeasure<?> temperature = DecimalMeasure
				.valueOf(String.format("%.2f", value) + " " + SI.CELSIUS);

		StateValue[] values = this.currentState
				.getState(MultipleTemperatureState.class.getSimpleName())
				.getCurrentStateValue();

		//search for the temperature value of the sensor identified by the given id
		boolean found = false;
		for (int i = 0; (i < values.length) && (!found); i++)
		{
			if (values[i].getFeatures().get("sensorID").equals(sensorID))
			{
				values[i].setValue(temperature);
				found = true;
			}
		}
		// update the status (Monitor Admin)
		this.updateStatus();

		// notify the change
		this.notifyChangedTemperatureAt(temperature, sensorID);

		// log
		this.logger.log(LogService.LOG_INFO, "Device " + device.getDeviceId()
				+ " temperature " + temperature.toString());
	}

}
