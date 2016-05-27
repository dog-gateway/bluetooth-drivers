/*
 * Dog - Bluetooth Low Energy Adapter driver
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
package org.doggateway.drivers.bluetooth.ble.gateway;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.doggateway.drivers.bluetooth.ble.gateway.interfaces.BLEAdapterDriver;
import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.info.BLEInfo;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceCostants;
import it.polito.elite.dog.core.library.model.devicecategory.BluetoothAdapter;
import it.polito.elite.dog.core.library.util.LogHelper;

/**
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public class BLEAdapterImpl implements Driver, BLEAdapterDriver
{
	// the driver id
	public static final String DRIVER_ID = "org.doggateway.drivers.bluetooth.ble.gateway";
	// a reference to the current bundle context
	private BundleContext context;

	// the bundle logger
	private LogHelper logger;

	// a reference to the network driver (currently not used by this driver
	// version, in the future it will be used to implement gateway-specific
	// functionalities.
	private AtomicReference<BLENetwork> network;

	// the registration object needed to handle the life span of this bundle in
	// the OSGi framework (it is a ServiceRegistration object for use by the
	// bundle registering the service to update the service's properties or to
	// unregister the service).
	private ServiceRegistration<?> regDriver;

	// register this driver as a gateway used by device-specific drivers
	private ServiceRegistration<?> regBLEAdapter;

	// the set of currently connected gateways... indexed by their device id,
	// currently only one is used.
	private ConcurrentHashMap<String, BLEDriverInstance> connectedAdapters;

	/**
	 * Creates an instance of the Bluetooth LE Adapter driver, which matches all
	 * bluetooth adapter devices in the framework and delegates their handling
	 * to a corresponding BLEAdapterInstance.
	 */
	public BLEAdapterImpl()
	{
		// create the needed atomic references
		this.network = new AtomicReference<BLENetwork>();

		// create the map of currently handled adapters
		this.connectedAdapters = new ConcurrentHashMap<String, BLEDriverInstance>();

		// TODO add support for "automatic instantiation" of known device types
		// It shall be better investigated which feature of the device allow to
		// uniquely identify its type
	}

	/**
	 * Handle the bundle activation, basically stores the context and the logger
	 * references, then calls the service registration methods.
	 */
	public void activate(BundleContext bundleContext)
	{
		// store the context
		this.context = bundleContext;

		// init the logger
		this.logger = new LogHelper(context);

		// register the service, TODO: this shall be called from the updated()
		// method if and when this bundle will become configurable
		this.registerDriver();
	}

	public void deactivate()
	{
		// remove the service from the OSGi framework
		this.unRegisterDriver();
	}

	/**
	 * Store a reference to the BLE Network driver
	 * 
	 * @param network
	 */
	public void addedBLENetwork(BLENetwork network)
	{
		this.network.set(network);
		this.registerDriver();
	}

	/**
	 * Removes the reference to the given BLE Network driver, does nothing if
	 * the current reference is different from the one given
	 * 
	 * @param network
	 */
	public void removedBLENetwork(BLENetwork network)
	{
		this.network.compareAndSet(network, null);
		this.unRegisterDriver();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public int match(ServiceReference reference) throws Exception
	{
		int matchValue = Device.MATCH_NONE;

		// get the given device category
		String deviceCategory = (String) reference
				.getProperty(DeviceCostants.DEVICE_CATEGORY);

		// get the given device manufacturer
		String manufacturer = (String) reference
				.getProperty(DeviceCostants.MANUFACTURER);

		// compute the matching score between the given device and this driver
		if (deviceCategory != null)
		{
			if (manufacturer != null
					&& manufacturer.equals(BLEInfo.MANUFACTURER)
					&& (deviceCategory
							.equals(BluetoothAdapter.class.getName())))
			{
				matchValue = BluetoothAdapter.MATCH_MANUFACTURER
						+ BluetoothAdapter.MATCH_TYPE;
			}

		}
		return matchValue;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public String attach(ServiceReference reference) throws Exception
	{
		if (this.regDriver != null)
		{
			// get the controllable device to attach
			@SuppressWarnings("unchecked")
			ControllableDevice device = (ControllableDevice) this.context
					.getService(reference);

			// get the device id
			String deviceId = device.getDeviceId();

			// add only if the gateway is not registered
			if ((!this.isGatewayAvailable(deviceId)))
			{
				// create a new driver instance
				// a bit different from other devices as the adapter is not a
				// bluetooth device!!
				// TODO: check if this works :-D
				BLEAdapterDriverInstance driverInstance = new BLEAdapterDriverInstance(
						this.network.get(), device, this.logger);

				// associate device and driver
				device.setDriver(driverInstance);

				// store the just created gateway instance
				synchronized (this.connectedAdapters)
				{
					// store a reference to the gateway driver
					this.connectedAdapters.put(device.getDeviceId(),
							driverInstance);
				}

				// modify the service description causing a forcing the
				// framework to send a modified service notification
				final Hashtable<String, Object> propDriver = new Hashtable<String, Object>();
				propDriver.put(DeviceCostants.DRIVER_ID, DRIVER_ID);
				propDriver.put(DeviceCostants.GATEWAY_COUNT,
						this.connectedAdapters.size());

				this.regDriver.setProperties(propDriver);
			}
		}

		return null;

	}

	@Override
	public boolean isGatewayAvailable(String deviceId)
	{
		return this.connectedAdapters.containsKey(deviceId);
	}
	
	@Override
	public String getGatewayMacAddress(String deviceId)
	{
		String mac = null;
		BLEDriverInstance drvInstance = this.connectedAdapters.get(deviceId);
		if(drvInstance!=null)
			mac = drvInstance.getDeviceMacAddress();
		return mac;
	}

	/**
	 * Registers this driver in the OSGi framework with two natures: 1) as
	 * device driver matching bluetooth adapters 2) as adapter service that
	 * provides specific adapter functions
	 */
	private void registerDriver()
	{
		if ((this.network.get() != null) && (this.context != null)
				&& (this.regDriver == null))
		{
			// prepare the driver registration properties
			Hashtable<String, Object> propDriver = new Hashtable<String, Object>();
			propDriver.put(DeviceCostants.DRIVER_ID, BLEAdapterImpl.DRIVER_ID);
			propDriver.put(DeviceCostants.GATEWAY_COUNT,
					connectedAdapters.size());

			// register this class as OSGi device driver
			this.regDriver = context.registerService(Driver.class.getName(),
					this, propDriver);

			// register this class as a service of type BLEAdapter
			this.regBLEAdapter = context.registerService(BLEAdapterDriver.class,
					this, null);
		}

	}

	private void unRegisterDriver()
	{
		// un-registers this driver
		if (this.regDriver != null)
		{
			this.regDriver.unregister();
			this.regDriver = null;
		}
		if (this.regBLEAdapter != null)
		{
			this.regBLEAdapter.unregister();
			this.regBLEAdapter = null;
		}

	}

}
