/**
 * 
 */
package org.doggateway.drivers.bluetooth.ble.device;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.doggateway.drivers.bluetooth.ble.gateway.interfaces.BLEAdapterDriver;
import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.info.BLEInfo;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceCostants;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.util.LogHelper;

/**
 * @author bonino
 *
 */
public abstract class BLEDeviceDriver implements Driver, ManagedService
{
	// The OSGi framework context
	protected BundleContext context;

	// System logger
	protected LogHelper logger;

	// a reference to the network driver
	private AtomicReference<BLENetwork> network;

	// a reference to the gateway driver
	private AtomicReference<BLEAdapterDriver> gateway;

	// the list of instances controlled / spawned by this driver
	protected Hashtable<String, BLEDriverInstance> managedInstances;

	// the registration object needed to handle the life span of this bundle in
	// the OSGi framework (it is a ServiceRegistration object for use by the
	// bundle registering the service to update the service's properties or to
	// unregister the service).
	private ServiceRegistration<?> regDriver;

	// the filter query for listening to framework events relative to the
	// to the ZWave gateway driver
	String filterQuery = String.format("(%s=%s)", Constants.OBJECTCLASS,
			BLEAdapterDriver.class.getName());

	// what are the on/off device categories that can match with this driver?
	protected Set<String> deviceCategories;

	// the driver instance class from which extracting the supported device
	// categories
	protected Class<?> driverInstanceClass;

	// the device glass used for auto-configuration
	protected String deviceMainClass;

	// milliseconds between two update of the device status, from configuration
	// file
	protected int updateTimeMillis;

	/**
	 * 
	 */
	public BLEDeviceDriver()
	{
		// intialize atomic references
		this.gateway = new AtomicReference<BLEAdapterDriver>();
		this.network = new AtomicReference<BLENetwork>();

		// initialize the connected drivers list
		this.managedInstances = new Hashtable<String, BLEDriverInstance>();

		// initialize the set of implemented device categories
		this.deviceCategories = new HashSet<String>();

	}

	/**
	 * Handle the bundle activation
	 */
	public void activate(BundleContext bundleContext)
	{
		// init the logger
		this.logger = new LogHelper(bundleContext);

		// store the context
		this.context = bundleContext;

		// fill the device categories
		this.properFillDeviceCategories(this.driverInstanceClass);

		// try registering the driver
		this.registerBLEDeviceDriver();

	}

	public void deactivate()
	{
		// remove the service from the OSGi framework
		this.unRegisterBLEDeviceDriver();
	}

	// ------- Handle dynamic service binding -------------------

	/**
	 * Called when an {@link BLEAdapterDriver} becomes available and can be
	 * exploited by this driver
	 * 
	 * @param gatewayDriver
	 */
	public void gatewayAdded(BLEAdapterDriver gatewayDriver)
	{
		this.gateway.set(gatewayDriver);
	}

	/**
	 * Called whe the given {@link BLEAdapterDriver} ceases to exist in the
	 * framework; it triggers a disposal of corresponding references
	 * 
	 * @param gatewayDriver
	 */
	public void gatewayRemoved(BLEAdapterDriver gatewayDriver)
	{
		if (this.gateway.compareAndSet(gatewayDriver, null))
			// unregisters this driver from the OSGi framework
			unRegisterBLEDeviceDriver();
	}

	/**
	 * Called when a {@link BLENetwork} service becomes available and can be
	 * exploited by this driver.
	 * 
	 * @param networkDriver
	 */
	public void networkAdded(BLENetwork networkDriver)
	{
		this.network.set(networkDriver);
	}

	/**
	 * Called when the given {@link BLENetwork} services is no more available in
	 * the OSGi framework; triggers removal of any reference to the service.
	 * 
	 * @param networkDriver
	 */
	public void networkRemoved(BLENetwork networkDriver)
	{
		if (this.network.compareAndSet(networkDriver, null))
			// unregisters this driver from the OSGi framework
			unRegisterBLEDeviceDriver();
	}

	/**
	 * Registers this driver in the OSGi framework, making its services
	 * available to all the other bundles living in the same or in connected
	 * frameworks.
	 */
	private void registerBLEDeviceDriver()
	{
		if ((this.gateway.get() != null) && (this.network.get() != null)
				&& (this.context != null) && (this.regDriver == null))
		{
			// create a new property object describing this driver
			Hashtable<String, Object> propDriver = new Hashtable<String, Object>();
			// add the id of this driver to the properties
			propDriver.put(DeviceCostants.DRIVER_ID, this.getClass().getName());
			// register this driver in the OSGi framework
			regDriver = context.registerService(Driver.class.getName(), this,
					propDriver);
		}
	}

	/**
	 * Handle the bundle de-activation
	 */
	protected void unRegisterBLEDeviceDriver()
	{
		// TODO DETACH allocated Drivers
		if (regDriver != null)
		{
			regDriver.unregister();
			regDriver = null;
		}
	}

	/**
	 * Fill a set with all the device categories whose devices can match with
	 * this driver. Automatically retrieve the device categories list by reading
	 * the implemented interfaces of its DeviceDriverInstance class bundle.
	 */
	public void properFillDeviceCategories(Class<?> cls)
	{
		if (cls != null)
		{
			for (Class<?> devCat : cls.getInterfaces())
			{
				this.deviceCategories.add(devCat.getName());
			}
		}

	}

	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException
	{
		if (properties != null)
		{
			// try to get the baseline polling time
			String updateTimeAsString = (String) properties
					.get(BLEInfo.POLLING_TIME_MILLIS);

			// trim leading and trailing spaces
			updateTimeAsString = updateTimeAsString.trim();

			// check not null
			if (updateTimeAsString != null)
			{
				// parse the string
				updateTimeMillis = Integer.valueOf(updateTimeAsString);
			}
			else
			{
				throw new ConfigurationException(BLEInfo.POLLING_TIME_MILLIS,
						BLEInfo.POLLING_TIME_MILLIS
								+ " not defined in configuraton file for "
								+ BLEDeviceDriver.class.getName());
			}

			// register driver
			this.registerBLEDeviceDriver();
		}

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
		String manifacturer = (String) reference
				.getProperty(DeviceCostants.MANUFACTURER);

		// get the gateway to which the device is connected
		String gateway = (String) reference.getProperty(DeviceCostants.GATEWAY);

		// compute the matching score between the given device and
		// this driver
		if (deviceCategory != null)
		{
			if (manifacturer != null && (gateway != null)
					&& (manifacturer.equals(BLEInfo.MANUFACTURER))
					&& (this.deviceCategories.contains(deviceCategory))
					&& (this.gateway.get() != null)
					&& (this.gateway.get().isGatewayAvailable(gateway)))
			{
				matchValue = Controllable.MATCH_MANUFACTURER
						+ Controllable.MATCH_TYPE;
			}

		}

		return matchValue;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public String attach(ServiceReference reference) throws Exception
	{
		// get the referenced device
		ControllableDevice device = ((ControllableDevice) context
				.getService(reference));

		// get the gateway to which the device is connected
		String gateway = (String) reference.getProperty(DeviceCostants.GATEWAY);

		// get the gateway mac address
		String gwMacAddress = this.gateway.get().getGatewayMacAddress(gateway);

		// check if not already attached
		if (!this.managedInstances.containsKey(device.getDeviceId()))
		{
			// create a new driver instance
			BLEDriverInstance driverInstance = this.createBLEDriverInstance(
					this.network.get(), device, gwMacAddress, updateTimeMillis,
					context);

			// connect this driver instance with the device
			device.setDriver(driverInstance);

			// store a reference to the connected driver
			synchronized (this.managedInstances)
			{
				this.managedInstances.put(device.getDeviceId(), driverInstance);
			}
		}

		return null;

	}

	public abstract BLEDriverInstance createBLEDriverInstance(
			BLENetwork bleNetwork, ControllableDevice device,
			String gwMacAddress, int pollingTimeMillis, BundleContext context);
}
