/**
 * 
 */
package org.doggateway.drivers.bluetooth.ble.temperaturesensor;

import org.doggateway.drivers.bluetooth.ble.device.BLEDeviceDriver;
import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.osgi.framework.BundleContext;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.SingleTemperatureSensor;

/**
 * @author bonino
 *
 */
public class CC2650TemperatureSensorDriver extends BLEDeviceDriver
{
	/**
	 * 
	 */
	public CC2650TemperatureSensorDriver()
	{
		// call the superclass constructor
		super();

		// set the driver instance class
		this.driverInstanceClass = CC2650TemperatureSensorDriverInstance.class;

		// set the main device class
		this.deviceMainClass = SingleTemperatureSensor.class.getSimpleName();
	}

	@Override
	public BLEDriverInstance createBLEDriverInstance(BLENetwork bleNetwork,
			ControllableDevice device, String gwMacAddress,
			int pollingTimeMillis, BundleContext context)
	{
		return new CC2650TemperatureSensorDriverInstance(bleNetwork, device, gwMacAddress, pollingTimeMillis, context);
	}

}
