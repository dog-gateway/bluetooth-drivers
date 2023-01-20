/*
 * Dog - Bluetooth Low Energy driver for the ISMB Energy Harvesting Temperature and Humidity sensor
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
package org.doggateway.drivers.bluetooth.ble.ismb.temperatureandhumiditysensor;

import java.nio.ByteBuffer;

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
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public class ISMBTemperatureAndHumiditySensorDriverInstance
		extends BLEDriverInstance implements TemperatureAndHumiditySensor
{

	private static final String TEMPERATURE_UUID = "00002a1c-0000-1000-8000-00805f9b34fb";
	private static final String HUMIDITY_UUID = "00002a6f-0000-1000-8000-00805f9b34fb";

	public ISMBTemperatureAndHumiditySensorDriverInstance(BLENetwork network,
			ControllableDevice device, String gwMacAddress,
			int pollingTimeMillis, BundleContext context)
	{
		// call the super class constructor
		super(network, device, gwMacAddress, pollingTimeMillis,
				new LogHelper(context));

		// initialize the device status
		this.initializeStates();
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
	public void deleteGroup(Integer groupID)
	{
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyLeftGroup(Integer groupNumber)
	{
		((TemperatureAndHumiditySensor) this.device)
				.notifyLeftGroup(groupNumber);

	}

	@Override
	public void notifyChangedRelativeHumidity(Measure<?, ?> relativeHumidity)
	{
		((TemperatureAndHumiditySensor) this.device)
				.notifyChangedRelativeHumidity(relativeHumidity);
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

	}

	@Override
	public void newMessageFromHouse(String characteristicUUID,
			String serviceUUID, byte[] value)
	{
		if (characteristicUUID.equals(
				ISMBTemperatureAndHumiditySensorDriverInstance.TEMPERATURE_UUID))
		{
			// 32bit IEEE floating point
			byte[] tempraw = { value[1], value[2], value[3], value[4] };

			// wrap the buffer
			ByteBuffer buffer = ByteBuffer.wrap(tempraw);
			// interpret the value
			float tempValue = buffer.getFloat();

			// update status and notify
			this.updateAndNotifyTemperature((double) tempValue);

		}
		else if (characteristicUUID.equals(
				ISMBTemperatureAndHumiditySensorDriverInstance.HUMIDITY_UUID))
		{
			// TODO check byte order to perform more direct and esy to
			// understand unwrapping!!

			// unpack / interpret the humidity value (uint 16)
			byte[] rawHumidity = { value[1], value[2] };

			// wrap the buffer
			ByteBuffer buffer = ByteBuffer.wrap(rawHumidity);

			// get the value as a signed short
			short signedHumidityValue = buffer.getShort();

			// get the unsigned value
			int uHumidityValue = 0x0000ffff & ((int) signedHumidityValue);

			//get the actual humidity value
			float humidityValue = (float) uHumidityValue * 0.01f;
			
			// update status and notify
			this.updateAndNotifyHumidity((double) humidityValue);
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
	private void updateAndNotifyTemperature(Double value)
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

	private void updateAndNotifyHumidity(Double value)
	{

		// treat the temperature as a measure
		DecimalMeasure<?> humidity = DecimalMeasure
				.valueOf(String.format("%.2f", value) + " %");

		// update the current state
		this.currentState
				.getState(HumidityMeasurementState.class.getSimpleName())
				.getCurrentStateValue()[0].setValue(humidity);

		// update the status (Monitor Admin)
		this.updateStatus();

		// notify the change
		this.notifyChangedRelativeHumidity(humidity);

		// log
		this.logger.log(LogService.LOG_INFO, "Device " + device.getDeviceId()
				+ " humidity " + humidity.toString());
	}
}
