package org.doggateway.drivers.bluetooth.ble;

import java.util.Dictionary;

import org.doggateway.drivers.bluetooth.ble.device.BLEDeviceDriver;
import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.devicecategory.CC2650SensorTag;

public class CC2650Driver extends BLEDeviceDriver
{
	private static final String MOVEMENT_POLLING_TIME_MILLIS = "movementPollingTimeMillis";
	// different polling time for movement sensor
	private int movementPollingTimeMillis = 0;

	public CC2650Driver()
	{
		// call the superclass constructor
		super();

		// set the driver instance class
		this.driverInstanceClass = CC2650DriverInstance.class;

		// set the main device class
		this.deviceMainClass = CC2650SensorTag.class.getSimpleName();
	}

	@Override
	public BLEDriverInstance createBLEDriverInstance(BLENetwork bleNetwork,
			ControllableDevice device, String gwMacAddress,
			int pollingTimeMillis, BundleContext context)
	{
		return new CC2650DriverInstance(bleNetwork, device, gwMacAddress,
				pollingTimeMillis,
				(this.movementPollingTimeMillis > 0)
						? this.movementPollingTimeMillis : pollingTimeMillis,
				context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.doggateway.drivers.bluetooth.ble.device.BLEDeviceDriver#updated(java.
	 * util.Dictionary)
	 */
	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException
	{

		if (properties != null)
		{
			// try to get the baseline polling time
			String updateTimeAsString = (String) properties
					.get(CC2650Driver.MOVEMENT_POLLING_TIME_MILLIS);

			// trim leading and trailing spaces
			updateTimeAsString = updateTimeAsString.trim();

			// check not null
			if (updateTimeAsString != null)
			{
				// parse the string
				this.movementPollingTimeMillis = Integer
						.valueOf(updateTimeAsString);
			}
		}

		super.updated(properties);

	}

}
