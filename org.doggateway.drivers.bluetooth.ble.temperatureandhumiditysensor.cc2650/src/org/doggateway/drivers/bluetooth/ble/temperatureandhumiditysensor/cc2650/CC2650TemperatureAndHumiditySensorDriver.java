/**
 * 
 */
package org.doggateway.drivers.bluetooth.ble.temperatureandhumiditysensor.cc2650;

import org.doggateway.drivers.bluetooth.ble.device.BLEDeviceDriver;
import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.osgi.framework.BundleContext;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.TemperatureAndHumiditySensor;

/**
 * @author bonino
 *
 */
public class CC2650TemperatureAndHumiditySensorDriver extends BLEDeviceDriver
{

	/**
	 * 
	 */
	public CC2650TemperatureAndHumiditySensorDriver()
	{
		// call the superclass constructor
		super();

		// set the driver instance class
		this.driverInstanceClass = CC2650TemperatureAndHumiditySensorDriverInstance.class;

		// set the main device class
		this.deviceMainClass = TemperatureAndHumiditySensor.class
				.getSimpleName();
	}

	@Override
	public BLEDriverInstance createBLEDriverInstance(BLENetwork bleNetwork,
			ControllableDevice device, String gwMacAddress,
			int pollingTimeMillis, BundleContext context)
	{
		// TODO Auto-generated method stub
		return new CC2650TemperatureAndHumiditySensorDriverInstance(bleNetwork,
				device, gwMacAddress, pollingTimeMillis, context);
	}

}
