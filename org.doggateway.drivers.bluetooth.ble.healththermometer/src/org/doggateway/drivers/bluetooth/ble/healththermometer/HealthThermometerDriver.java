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

import org.doggateway.drivers.bluetooth.ble.device.BLEDeviceDriver;
import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.osgi.framework.BundleContext;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.HealthTemperatureSensor;

/**
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>no
 *
 */
public class HealthThermometerDriver extends BLEDeviceDriver
{

	/**
	 * 
	 */
	public HealthThermometerDriver()
	{
		// call the superclass constructor
		super();

		// set the driver instance class
		this.driverInstanceClass = HealthThermometerDriverInstance.class;

		// set the main device class
		this.deviceMainClass = HealthTemperatureSensor.class.getSimpleName();
	}

	@Override
	public BLEDriverInstance createBLEDriverInstance(BLENetwork bleNetwork,
			ControllableDevice device, String gwMacAddress,
			int pollingTimeMillis, BundleContext context)
	{
		// TODO Auto-generated method stub
		return new HealthThermometerDriverInstance(bleNetwork, device, gwMacAddress, pollingTimeMillis, context);
	}

}
