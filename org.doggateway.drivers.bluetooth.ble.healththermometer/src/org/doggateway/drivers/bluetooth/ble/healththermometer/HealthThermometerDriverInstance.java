/*
 * Dog - Bluetooth Low Energy Texas Instruments Bluetooth Health Thermometer driver
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
package org.doggateway.drivers.bluetooth.ble.healththermometer;

import java.nio.ByteBuffer;

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
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.devicecategory.HealthTemperatureSensor;
import it.polito.elite.dog.core.library.model.devicecategory.SingleTemperatureSensor;
import it.polito.elite.dog.core.library.model.state.TemperatureState;
import it.polito.elite.dog.core.library.model.statevalue.TemperatureStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;

/**
 * Currently NOT WORKING due to missing support for notifications in tinyB library!!!
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>no
 *
 */
public class HealthThermometerDriverInstance extends BLEDriverInstance
		implements HealthTemperatureSensor
{
	public static final String SERVICE_UUID = "00001809-0000-1000-8000-00805f9b34fb";
	public static final String CHARACTERISTIC_UUID = "00002a1c-0000-1000-8000-00805f9b34fb";

	// 0x2902
	public static final String CONFIG_UUID = "00002a1d-0000-1000-8000-00805f9b34fb";

	public HealthThermometerDriverInstance(BLENetwork bleNetwork,
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
	public Measure<?, ?> getTemperature()
	{
		// provides back the current temperature
		return (Measure<?, ?>) this.currentState
				.getState(TemperatureState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
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
	public void notifyNewTemperatureValue(Measure<?, ?> temperatureValue)
	{
		((SingleTemperatureSensor) this.device)
				.notifyNewTemperatureValue(temperatureValue);
	}

	@Override
	public void notifyJoinedGroup(Integer groupNumber)
	{
		((SingleTemperatureSensor) this.device).notifyJoinedGroup(groupNumber);

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
		if (characteristicUUID
				.equals(HealthThermometerDriverInstance.CHARACTERISTIC_UUID))
		{
			// 32bit IEEE floating point
			byte[] tempraw = { value[1], value[2], value[3], value[4] };

			// wrap the buffer
			ByteBuffer buffer = ByteBuffer.wrap(tempraw);
			// interpret the value
			float tempValue = buffer.getFloat();

			// update status and notify
			this.updateAndNotify((double) tempValue);

		}

	}

	private void initializeStates()
	{
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
	}

	private void enableCharacteristicsNotification()
	{
		// WARNING: this probably fails if the device is not in range, we shall
		// probably address a way to enable characteristic notifications!
		byte[] enable = { 0x01 };
		this.network.writeValue(this.getDeviceMacAddress(),
				HealthThermometerDriverInstance.SERVICE_UUID,
				HealthThermometerDriverInstance.CONFIG_UUID, enable);

		// TODO: handle actual notification now that the notifications are supported by TinyB!!
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
	private void updateAndNotify(Double value)
	{

		// treat the temperature as a measure
		DecimalMeasure<?> temperature = DecimalMeasure
				.valueOf(String.format("%.2f", value) + " " + SI.CELSIUS);

		// update the current state
		this.currentState.getState(TemperatureState.class.getSimpleName())
				.getCurrentStateValue()[0].setValue(temperature);

		// update the status (Monitor Admin)
		this.updateStatus();

		// notify the change
		this.notifyNewTemperatureValue(temperature);

		// log
		this.logger.log(LogService.LOG_INFO, "Device " + device.getDeviceId()
				+ " temperature " + temperature.toString());
	}

}
