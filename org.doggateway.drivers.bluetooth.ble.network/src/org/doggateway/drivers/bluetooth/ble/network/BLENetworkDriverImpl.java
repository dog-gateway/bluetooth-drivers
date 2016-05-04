/*
 * Dog - Bluetooth Low Energy Network Driver
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
package org.doggateway.drivers.bluetooth.ble.network;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.doggateway.drivers.bluetooth.ble.network.info.BLEDeviceRegistration;
import org.doggateway.drivers.bluetooth.ble.network.info.CharacteristicMonitorSpec;
import org.doggateway.drivers.bluetooth.ble.network.info.ManagedBluetoothDevice;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLEDiscoveryListener;
import org.doggateway.drivers.bluetooth.ble.network.tasks.BLEDiscoveryWorker;
import org.doggateway.drivers.bluetooth.ble.network.tasks.BLEPollingWorker;
import org.doggateway.drivers.bluetooth.ble.network.tasks.NotifyValueTask;
import org.doggateway.libraries.intel.tinyb.service.BluetoothService;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.core.library.util.LogHelper;
import tinyb.BluetoothDevice;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

/**
 * <p>
 * The Bluetooth Low Energy Network Driver service implementation, offers
 * facilities to easily access and interface EnOcean networks, by exploiting the
 * Eclipse Kura bundles.
 * </p>
 * 
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public class BLENetworkDriverImpl implements BLENetwork, ManagedService
{

	// the polling time configuration parameter and default value
	public static final String LOWEST_VALID_POLLING_TIME_KEY = "lowestAdmissiblePollingTimeMillis";
	public static final int LOWEST_VALID_POLLING_TIME_MILLIS = 1000;
	protected int lowestValidPollingTime;

	// the trials attempted to get a device
	public static final String DEVICE_GET_TRIALS_KEY = "nDeviceGetTrials";
	public static final int DEVICE_GET_TRIALS = 1;
	protected int deviceGetTrials;

	// the device get timeout
	public static final String DEVICE_GET_TIMEOUT_KEY = "deviceGetTimeout";
	public static final int DEVICE_GET_TIMEOUT = 0;
	protected int deviceGetTimeout;

	// the minimum thread sleep time
	private static final int MINIMUM_THREAD_SLEEP_MILLIS = 1;

	// the number of discovery attempts per discovery cycle
	public static final String DISCOVERY_TRIALS_KEY = "nDiscoveryAttemptsPerCycle";
	public static final int DISCOVERY_TRIALS = 5;
	protected int discoveryTrials;

	// the interval between subsequent discovery trials
	public static final String DISCOVERY_INTERVAL_MILLIS_KEY = "IntervalBetweenDiscoveryCyclesMillis";
	public static final int DISCOVERY_INTERVAL_MILLIS = 30000;
	protected int discoveryIntervalMillis;

	// the interval between attempts in one discovery cycle
	public static final String DISCOVERY_CYCLYING_TIME_MILLIS_KEY = "IntervalBetweenDiscoveryAttemptsInCycleMillis";
	public static final int DISCOVERY_CYCLYING_TIME_MILLIS = 4000;
	protected int discoveryCyclingTimeMillis;

	// the bundle context
	private BundleContext bundleContext;

	// the driver logger
	private LogHelper logger;

	// the bluetooth service to use
	private AtomicReference<BluetoothService> bluetooth;

	/*
	 * Polling time: each driver might specify a different "desired" polling
	 * time for each characteristic which is interpreted as the "maximum" delay
	 * accepted between subsequent measures. Actual delivery rate may vary and
	 * is computed as the minimum between all polling times required for the
	 * device to which the characteristic belongs. If the minimum polling time
	 * computed as such is lower than the base polling time parameter for this
	 * driver, a warning is logged and the base polling time is used. If no
	 * polling time, or a negative value is specified, the driver will use the
	 * base polling time.
	 * 
	 * To ensure time consistency (+-10% jitter) the polling thread tries to run
	 * 10 times faster than the minimum required polling time. This parameter
	 * can be set at configuration time.
	 */

	// the maximum allowed time jitter in percentage, cannot be 0. Minimum value
	// accepted is limited by the minimum thread sleep time of 1ms. In case time
	// jitter cannot be satisfied, a warning will be raised.
	public static final String ALLOWED_JITTER_KEY = "jitterPercent";
	public static final int ALLOWED_JITTER_PERCENTAGE = 50;
	private int allowedTimeJitter;

	// the polling time currently used in milliseconds, computed as the minimum
	// between all device polling times.
	private int actualPollingTimeMillis;

	// the set of device registrations required by device-specific drivers, each
	// driver may register more than one device registration
	private ArrayList<BLEDeviceRegistration> activeRegistrations;

	// the set of managed devices
	private Hashtable<String, ManagedBluetoothDevice> managedDevices;

	// TODO: check this again after implementing the full driver logic

	// the set of pending discovery
	private ArrayList<ManagedBluetoothDevice> devicesWaitingForDiscovery;

	// the dispatching Executor service
	private ExecutorService dispatchingService;

	// the polling thread
	private BLEPollingWorker pollingWorker;

	// the discovery thread
	private BLEDiscoveryWorker discoveryWorker;

	/**
	 * Class constructor, builds required data structures
	 */
	public BLENetworkDriverImpl()
	{
		// set default values
		this.lowestValidPollingTime = BLENetworkDriverImpl.LOWEST_VALID_POLLING_TIME_MILLIS;
		this.deviceGetTrials = BLENetworkDriverImpl.DEVICE_GET_TRIALS;
		this.deviceGetTimeout = BLENetworkDriverImpl.DEVICE_GET_TIMEOUT;
		this.discoveryTrials = BLENetworkDriverImpl.DISCOVERY_TRIALS;
		this.discoveryIntervalMillis = BLENetworkDriverImpl.DISCOVERY_INTERVAL_MILLIS;
		this.discoveryCyclingTimeMillis = BLENetworkDriverImpl.DISCOVERY_CYCLYING_TIME_MILLIS;

		// build the atomic references
		this.bluetooth = new AtomicReference<BluetoothService>();

		// create the connected drivers table
		this.activeRegistrations = new ArrayList<BLEDeviceRegistration>();

		// create the managed devices set
		this.managedDevices = new Hashtable<String, ManagedBluetoothDevice>();

		// create the devices waiting for discovery
		this.devicesWaitingForDiscovery = new ArrayList<ManagedBluetoothDevice>();

		// build the dispatching service
		// check if just 1 thread is sufficient, for the time being we select a
		// single
		// thread executor to ensure order of delivery
		this.dispatchingService = Executors.newSingleThreadExecutor();

		// create the polling worker thread
		this.pollingWorker = new BLEPollingWorker(this);

		// create the discovery polling thread
		this.discoveryWorker = new BLEDiscoveryWorker(this);
	}

	public void activate(BundleContext context)
	{
		// store the bundle context
		this.bundleContext = context;

		// initialize the class logger...
		this.logger = new LogHelper(this.bundleContext);

		// start the worker "polling" thread
		this.pollingWorker.setRunnable(true);
		this.pollingWorker.start();

		// start the worker "discovery" thread
		this.discoveryWorker.setRunnable(true);
		this.discoveryWorker.start();

		// debug: signal activation...
		this.logger.log(LogService.LOG_DEBUG, "Activated...");
	}

	public void deactivate()
	{
		// stop the polling worker
		this.pollingWorker.setRunnable(false);
		this.pollingWorker.interrupt();
		

		// stop the discovery worker
		this.discoveryWorker.setRunnable(false);
		this.discoveryWorker.interrupt();
		

		// debug: signal activation...
		this.logger.log(LogService.LOG_DEBUG, "Deactivated...");
	}

	public void addedBluetoothService(BluetoothService bls)
	{
		// store the bluetooth service reference
		this.bluetooth.set(bls);
	}

	public void removedBluetoothService(BluetoothService bls)
	{
		// remove the bluetooth service, if matching
		this.bluetooth.compareAndSet(bls, null);
	}

	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException
	{
		// get the bundle configuration parameters
		if (properties != null)
		{
			// debug log
			logger.log(LogService.LOG_DEBUG,
					"Received configuration properties");

			// lowest valid polling time
			String lowestValidPollingTime = (String) properties
					.get(BLENetworkDriverImpl.LOWEST_VALID_POLLING_TIME_KEY);
			if ((lowestValidPollingTime != null)
					&& (!lowestValidPollingTime.isEmpty()))
			{
				try
				{
					this.lowestValidPollingTime = Integer
							.valueOf(lowestValidPollingTime.trim());
				}
				catch (NumberFormatException e)
				{
					this.logger.log(LogService.LOG_WARNING,
							"wrong format for configuration param "
									+ BLENetworkDriverImpl.LOWEST_VALID_POLLING_TIME_KEY
									+ " should be integer");
				}

			}
			// number of device get trials
			String deviceGetTrials = (String) properties
					.get(BLENetworkDriverImpl.DEVICE_GET_TRIALS_KEY);
			if ((deviceGetTrials != null) && (!deviceGetTrials.isEmpty()))
			{
				try
				{
					this.deviceGetTrials = Integer.valueOf(deviceGetTrials.trim());
				}
				catch (NumberFormatException e)
				{
					this.logger.log(LogService.LOG_WARNING,
							"wrong format for configuration param "
									+ BLENetworkDriverImpl.DEVICE_GET_TRIALS_KEY
									+ " should be integer");
				}
			}

			// device get timeout
			String deviceGetTimeout = (String) properties
					.get(BLENetworkDriverImpl.DEVICE_GET_TIMEOUT_KEY);
			if ((deviceGetTimeout != null) && (!deviceGetTimeout.isEmpty()))
			{
				try
				{
					this.deviceGetTimeout = Integer.valueOf(deviceGetTimeout.trim());
				}
				catch (NumberFormatException e)
				{
					this.logger.log(LogService.LOG_WARNING,
							"wrong format for configuration param "
									+ BLENetworkDriverImpl.DEVICE_GET_TIMEOUT_KEY
									+ " should be integer");
				}
			}

			// number of discovery attempts within one cycle
			String discoveryTrials = (String) properties
					.get(BLENetworkDriverImpl.DISCOVERY_TRIALS_KEY);
			if ((discoveryTrials != null) && (!discoveryTrials.isEmpty()))
			{
				try
				{
					this.discoveryTrials = Integer.valueOf(discoveryTrials.trim());
				}
				catch (NumberFormatException e)
				{
					this.logger.log(LogService.LOG_WARNING,
							"wrong format for configuration param "
									+ BLENetworkDriverImpl.LOWEST_VALID_POLLING_TIME_KEY
									+ " should be integer");
				}
			}
			// interval between discovery cycles
			String discoveryIntervalMillis = (String) properties
					.get(BLENetworkDriverImpl.DISCOVERY_INTERVAL_MILLIS_KEY);
			if ((discoveryIntervalMillis != null)
					&& (!discoveryIntervalMillis.isEmpty()))
			{
				try
				{
					this.discoveryIntervalMillis = Integer
							.valueOf(discoveryIntervalMillis.trim());
				}
				catch (NumberFormatException e)
				{
					this.logger.log(LogService.LOG_WARNING,
							"wrong format for configuration param "
									+ BLENetworkDriverImpl.DISCOVERY_INTERVAL_MILLIS_KEY
									+ " should be integer");
				}
			}

			// time between attempts in a single discovery cycle
			String discoveryCyclingTimeMillis = (String) properties.get(
					BLENetworkDriverImpl.DISCOVERY_CYCLYING_TIME_MILLIS_KEY);
			if ((discoveryCyclingTimeMillis != null)
					&& (!discoveryCyclingTimeMillis.isEmpty()))
			{
				try
				{
					this.discoveryCyclingTimeMillis = Integer
							.valueOf(discoveryCyclingTimeMillis.trim());
				}
				catch (NumberFormatException e)
				{
					this.logger.log(LogService.LOG_WARNING,
							"wrong format for configuration param "
									+ BLENetworkDriverImpl.DISCOVERY_CYCLYING_TIME_MILLIS_KEY
									+ " should be integer");
				}
			}

			// the allowed jitter in polling time
			String jitterPercentage = (String) properties
					.get(BLENetworkDriverImpl.ALLOWED_JITTER_KEY);
			if ((jitterPercentage != null) && (!jitterPercentage.isEmpty()))
			{
				try
				{
					int jitterPercentageInt = Integer.valueOf(jitterPercentage.trim());
					if ((jitterPercentageInt > 0)
							&& (jitterPercentageInt <= 100))
						this.allowedTimeJitter = jitterPercentageInt;
				}
				catch (NumberFormatException e)
				{
					this.logger.log(LogService.LOG_WARNING,
							"wrong format for configuration param "
									+ BLENetworkDriverImpl.ALLOWED_JITTER_KEY
									+ " should be integer");
				}
			}

		}

	}

	@Override
	public void addDeviceRegistration(BLEDeviceRegistration devReg)
	{
		// TODO: this requires careful re-definition of the equals and hash
		// methods on BLEDeviceRegistrations

		// check that the registration has not been already inserted
		if (!this.activeRegistrations.contains(devReg))
		{
			// insert the registration
			this.activeRegistrations.add(devReg);

			// check if a managed device already exists for the given device
			// registration
			ManagedBluetoothDevice device = this.managedDevices
					.get(devReg.getDeviceMacAddress());

			// if null, create the device
			if (device == null)
			{
				// create the device
				device = new ManagedBluetoothDevice(
						devReg.getDeviceMacAddress(),
						devReg.getAdapterMacAddress());

				// store the device
				this.managedDevices.put(device.getDeviceMacAddress(), device);
			}

			// add the registration to the managed device
			device.addBLEDeviceRegistration(devReg);

			// attach the low-level device
			if (!this.attachLowLevelDevice(device))
			{
				synchronized (this.devicesWaitingForDiscovery)
				{
					this.devicesWaitingForDiscovery.add(device);
				}
			}

			// update the polling times
			this.updatePollingTimes();
		}
	}

	@Override
	public void removeAllDriverRegistrations(BLEDriverInstance driverInstance)
	{
		// scan all active registrations and for each registration pointing at
		// the given driver instance, remove specs from the managed devices, if
		// no other spec remain, also delete devices.

		// check not null
		if (driverInstance != null)
		{
			// create the list of registrations that mmust be removed
			ArrayList<BLEDeviceRegistration> toRemove = new ArrayList<BLEDeviceRegistration>();

			// iterate over device registrations
			for (BLEDeviceRegistration deviceReg : this.activeRegistrations)
			{
				// check if the registration originated from the given driver
				if (deviceReg.getBleDriverInstance().equals(driverInstance))
				{
					// the registration shall be removed

					// get the corresponding managed device
					ManagedBluetoothDevice deviceToUpdate = deviceReg
							.getManagedDevice();

					// remove the registration (nulls the pointer to the managed
					// device held by the registration object)
					deviceToUpdate.removeBLEDeviceRegistration(deviceReg);

					// check if the managed device has still specs pointing at
					// it
					if (deviceToUpdate.getDeviceRegistrations().size() < 1)
					{
						// remove the device
						this.managedDevices
								.remove(deviceToUpdate.getDeviceMacAddress());
					}

					// last step add the registration to the list of
					// registrations to remove, as removal cannot be done while
					// iterating over the set
					toRemove.add(deviceReg);
				}
			}

			// remove registrations
			for (BLEDeviceRegistration regToRemove : toRemove)
			{
				this.activeRegistrations.remove(regToRemove);
			}
		}
	}

	@Override
	public byte[] readValue(String deviceMacAddress, String serviceUUID,
			String characteristicUUID)
	{
		// get the managed device corresponding to the given mac address
		ManagedBluetoothDevice device = this.managedDevices
				.get(deviceMacAddress);
		return this.readValue(device, serviceUUID, characteristicUUID);
	}

	public byte[] readValue(ManagedBluetoothDevice device, String serviceUUID,
			String characteristicUUID)
	{
		byte[] value = null;

		// check not null
		if (device != null)
		{
			// get the low-level device
			BluetoothDevice lowDevice = device.getLowDevice();

			// check not null
			if (lowDevice != null)
			{
				// check if connected
				if (!lowDevice.getConnected())
				{
					// try connecting
					if (lowDevice.connect())
					{
						// the device device is connected
						value = this.readFromConnectedDevice(lowDevice,
								serviceUUID, characteristicUUID);
					}
					else
					{
						this.logger.log(LogService.LOG_WARNING,
								"Unable to connect to device "
										+ lowDevice.getName()
										+ " perhaps it is out-of-range or sleeping");
					}
				}
				else
				{
					// the device is connected
					value = this.readFromConnectedDevice(lowDevice, serviceUUID,
							characteristicUUID);
				}
			}
			else
			{
				this.logger.log(LogService.LOG_WARNING,
						"Unfortunately the device has not yet been discovered, please retry later...");
			}
		}
		else
		{
			this.logger.log(LogService.LOG_ERROR,
					"Attempt to write characteristic value to a device not managed by this network driver, perhaps you forgot to add a BLEDeviceRegistration?");
		}

		return value;
	}

	@Override
	public boolean writeValue(String deviceMacAddress, String serviceUUID,
			String characteristicUUID, byte[] value)
	{

		boolean written = false;

		// check value
		if (value != null)
		{
			// get the managed device corresponding to the given mac address
			ManagedBluetoothDevice device = this.managedDevices
					.get(deviceMacAddress);

			// check not null
			if (device != null)
			{
				// get the low-level device
				BluetoothDevice lowDevice = device.getLowDevice();

				// check not null
				if (lowDevice != null)
				{
					// check if connected
					if (!lowDevice.getConnected())
					{
						// try connecting
						if (lowDevice.connect())
						{
							// connected, write the value
							written = this.writeToConnectedDevice(lowDevice,
									serviceUUID, characteristicUUID, value);
						}
						else
						{
							this.logger.log(LogService.LOG_WARNING,
									"Unable to connect to device "
											+ lowDevice.getName()
											+ " perhaps it is out-of-range or sleeping");
						}
					}
					else
					{
						// connected, write the value
						written = this.writeToConnectedDevice(lowDevice,
								serviceUUID, characteristicUUID, value);
					}

				}
				else
				{
					this.logger.log(LogService.LOG_WARNING,
							"Unfortunately the device has not yet been discovered, please retry later...");
				}
			}
			else
			{
				this.logger.log(LogService.LOG_ERROR,
						"Attempt to write characteristic value to a device not managed by this network driver, perhaps you forgot to add a BLEDeviceRegistration?");
			}
		}
		else
		{
			this.logger.log(LogService.LOG_ERROR,
					"Attempt to write a NULL value on a Bluetooth device characteristic");
		}
		return written;
	}

	/**
	 * Provides a list of the mac addresses of devices currently managed, the
	 * list is a copy of the actual "live" structure hosted by this driver class
	 * which allows thread-safe operation where concurrent modifications to the
	 * list of managed devices might occur.
	 * 
	 * @return The list of the mac addresses of currently managed devices
	 */
	public ArrayList<String> getManagedBluetoothDeviceAddresses()
	{
		ArrayList<String> managedDevicesSnapshot = new ArrayList<String>();

		// copy the list of device mac addresses (Thread safe operation
		for (String devMac : this.managedDevices.keySet())
			managedDevicesSnapshot.add(devMac);

		return managedDevicesSnapshot;
	}

	/**
	 * Provides a "live" reference to the managed device associated to the real
	 * device having the given MAC address.
	 * 
	 * @param deviceMacAddress
	 * @return
	 */
	public ManagedBluetoothDevice getManagedDevice(String deviceMacAddress)
	{
		return this.managedDevices.get(deviceMacAddress);
	}

	/**
	 * Provides an immutable list of managed devices waiting for discovery.
	 * 
	 * @return The list of the mac addresses of currently managed devices
	 */
	public final ArrayList<ManagedBluetoothDevice> getManagedDevicesWaitingForDiscovery()
	{
		ArrayList<ManagedBluetoothDevice> managedDevicesSnapshot = new ArrayList<ManagedBluetoothDevice>();

		// copy the list of device mac addresses (Thread safe operation
		for (ManagedBluetoothDevice device : this.devicesWaitingForDiscovery)
			managedDevicesSnapshot.add(device);

		return managedDevicesSnapshot;
	}

	public void discoveredDevice(ManagedBluetoothDevice device)
	{
		// remove the device from the waiting list
		this.devicesWaitingForDiscovery.remove(device);
	}

	/**
	 * Gets the currently valid polling time in milliseconds
	 * 
	 * @return the actualPollingTimeMillis
	 */
	public int getActualPollingTimeMillis()
	{
		return actualPollingTimeMillis;
	}

	/**
	 * Sets the "default" adapter in discovery mode, TODO: extend this method
	 * for multiple adapter support
	 * 
	 * @param enabled
	 */
	public void setDiscovery(boolean enabled)
	{
		if (enabled)
		{
			this.bluetooth.get().getManager().startDiscovery();
		}
		else
		{
			this.bluetooth.get().getManager().stopDiscovery();
		}
	}
	
	

	/* (non-Javadoc)
	 * @see org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork#startDiscovery()
	 */
	@Override
	public void startDiscovery()
	{
		// if the discovery thread exists, start it
		if((this.discoveryWorker!=null)&&(this.discoveryWorker.isAlive()))
		{
			//signal the worker to resume...
			this.discoveryWorker.interrupt();
		}
	}

	/* (non-Javadoc)
	 * @see org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork#stopDiscovery()
	 */
	@Override
	public void stopDiscovery()
	{
		// TODO Define Automatic vs Manual discovery!!! at now do nothing		
	}

	/**
	 * Gets the list of devices available at the Bluetooth service level
	 * 
	 * @return
	 */
	public List<BluetoothDevice> getLowLevelDevices()
	{
		return this.bluetooth.get().getManager().getDevices();
	}

	/**
	 * Gets a reference to the logger service used by the driver (to be adopted
	 * by worker threads)
	 * 
	 * @return
	 */
	public LogHelper getLogger()
	{
		return this.logger;
	}

	@Override
	public void addDiscoveryListener(BLEDiscoveryListener listener)
	{
		this.discoveryWorker.addDiscoveryListener(listener);
	}

	@Override
	public boolean removeDiscoveryListener(BLEDiscoveryListener listener)
	{
		return this.discoveryWorker.removeDiscoveryListener(listener);
	}

	/**
	 * Gets the number of trials to attempt per each discovery cycle
	 * 
	 * @return the discoveryTrials
	 */
	public int getDiscoveryTrials()
	{
		return discoveryTrials;
	}

	/**
	 * Gets the time interval in milliseconds between subsequent discovery
	 * cycles
	 * 
	 * @return the discoveryIntervalMillis
	 */
	public int getDiscoveryIntervalMillis()
	{
		return discoveryIntervalMillis;
	}

	/**
	 * Gets the timeout in milliseconds between subsequent discovery attempts in
	 * a single cycle
	 * 
	 * @return the discoveryCyclingTimeMillis
	 */
	public int getDiscoveryCyclingTimeMillis()
	{
		return discoveryCyclingTimeMillis;
	}

	/**
	 * Notifies drivers of new values, for a given characteristic
	 * 
	 * @param characteristicUUID
	 *            The UUID of the characteristic for which the value has changed
	 * @param value
	 *            the new value
	 * @param targets
	 *            The drivers "listening" to this change
	 */
	public synchronized void notifyNewValue(String characteristicUUID,
			byte value[], String serviceUUID,
			Set<CharacteristicMonitorSpec> targets)
	{
		// iterate over characteristic specs
		for (CharacteristicMonitorSpec spec : targets)
		{
			// get the BLE device registration from the characteristic monitor
			// spec
			BLEDeviceRegistration devRegistration = spec.getServiceSpec()
					.getDeviceInfo();

			// get the driver instance
			BLEDriverInstance drvInstance = devRegistration
					.getBleDriverInstance();

			// dispatch the new value
			this.dispatchingService.submit(new NotifyValueTask(
					characteristicUUID, serviceUUID, value, drvInstance));
		}

	}

	/**
	 * Tries attaching a low-level {@link BluetoothDevice} with the given
	 * {@link ManagedBluetoothDevice}.
	 * 
	 * @param device
	 *            The managed device to attach
	 * @return true if successfully attached, false otherwise
	 */
	private boolean attachLowLevelDevice(ManagedBluetoothDevice device)
	{
		return this.attachLowLevelDevice(device, this.deviceGetTimeout,
				this.deviceGetTrials);
	}

	private boolean attachLowLevelDevice(ManagedBluetoothDevice device,
			int timeout, int nTrials)
	{
		boolean attached = false;

		// check if the device has already been discovered, otherwise
		// add it to the list of devices to be yet discovered
		BluetoothDevice lowDevice = this.bluetooth.get()
				.getDevice(device.getDeviceMacAddress(), timeout, nTrials);

		// if not null, store the device
		if (lowDevice != null)
		{
			// set the device
			device.setLowDevice(lowDevice);

			// set the attached flag at true
			attached = true;

			// services and characteristics will be filled on their first poll.
		}

		return attached;
	}

	private byte[] readFromConnectedDevice(BluetoothDevice device,
			String serviceUUID, String characteristicUUID)
	{
		// initially null
		byte[] value = null;

		// try getting the referred service
		BluetoothGattService service = this.bluetooth.get().getService(device,
				serviceUUID, this.deviceGetTimeout, this.deviceGetTrials);

		// check not null
		if (service != null)
		{
			// try getting the characteristic
			BluetoothGattCharacteristic characteristic = this.bluetooth.get()
					.getCharacteristic(service, characteristicUUID);

			// check not null
			if (characteristic != null)
			{
				// read the value
				value = characteristic.readValue();
			}
			else
			{
				// log the error
				this.logger.log(LogService.LOG_WARNING,
						"Unable to read the value for characteristic ("
								+ characteristicUUID + ") of service ("
								+ serviceUUID + ") of device "
								+ device.getName());
			}
		}
		else
		{
			// log the error
			this.logger.log(LogService.LOG_WARNING,
					"Unable to get service (" + serviceUUID + ") from device "
							+ device.getName() + " within "
							+ this.deviceGetTrials + " trial, each waiting for"
							+ this.deviceGetTimeout);
		}
		return value;
	}

	private boolean writeToConnectedDevice(BluetoothDevice lowDevice,
			String serviceUUID, String characteristicUUID, byte[] value)
	{
		boolean written = false;
		// try getting the referred service
		BluetoothGattService service = this.bluetooth.get().getService(
				lowDevice, serviceUUID, this.deviceGetTimeout,
				this.deviceGetTrials);

		// check not null
		if (service != null)
		{
			// try getting the characteristic
			BluetoothGattCharacteristic characteristic = this.bluetooth.get()
					.getCharacteristic(service, characteristicUUID);

			// check not null
			if (characteristic != null)
			{
				// read the value
				written = characteristic.writeValue(value);
			}
			else
			{
				// log the error
				this.logger.log(LogService.LOG_WARNING,
						"Unable to write the value for characteristic ("
								+ characteristicUUID + ") of service ("
								+ serviceUUID + ") of device "
								+ lowDevice.getName());
			}
		}
		else
		{
			// log the error
			this.logger.log(LogService.LOG_WARNING,
					"Unable to get service (" + serviceUUID + ") from device "
							+ lowDevice.getName() + " within "
							+ this.deviceGetTrials + " trial, each waiting for"
							+ this.deviceGetTimeout);
		}
		return written;
	}

	private void updatePollingTimes()
	{
		// iterate over all device registrations for computing the minum
		// required polling time
		int minimumPollingTimeRequired = Integer.MAX_VALUE;

		for (ManagedBluetoothDevice mDev : this.managedDevices.values())
		{
			if (mDev.getPollingTimeMillis() < minimumPollingTimeRequired)
				minimumPollingTimeRequired = mDev.getPollingTimeMillis();
		}

		// check the polling time against the lowest admissible polling time
		if (minimumPollingTimeRequired < this.lowestValidPollingTime)
		{
			// warn
			this.logger.log(LogService.LOG_WARNING,
					"The minimum polling time required by active device registrations is lower then the lowest admissible polling time, timing requirements will not be respected");

			// use the lowest bound
			minimumPollingTimeRequired = this.lowestValidPollingTime;
		}

		// compute the actual thread timing depending on jitter and device
		// number
		int jitterAwarePollingTime = (minimumPollingTimeRequired
				* allowedTimeJitter) / 100;
		this.actualPollingTimeMillis = (jitterAwarePollingTime > 0)
				? jitterAwarePollingTime
				: BLENetworkDriverImpl.MINIMUM_THREAD_SLEEP_MILLIS;
	}

}
