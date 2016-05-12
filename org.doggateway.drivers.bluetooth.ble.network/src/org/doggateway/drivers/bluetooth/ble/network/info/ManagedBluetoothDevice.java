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
package org.doggateway.drivers.bluetooth.ble.network.info;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import tinyb.BluetoothDevice;

public class ManagedBluetoothDevice
{

	// the device polling time in milliseconds
	private int pollingTimeMillis;

	// the last time at which the device was polled
	private long lastPollFromEpoch;

	// the MAC address of the device represented by this object
	private String deviceMacAddress;

	// the MAC address of the adapter to use for connecting to the given device
	// TODO: implement multiple-adapter support
	private String adapterMacAddress;

	// The set of "managed", i.e., "monitored" services offered by this managed
	// device
	private Hashtable<String, ManagedBluetoothService> services;

	// The set of {@link BLEDeviceRegistrations} pointing at this managed device
	private Set<BLEDeviceRegistration> deviceInfos;

	// The low-level device to which this object is associated
	private BluetoothDevice lowDevice;

	/**
	 * Builds a new instance of managed device pointing at the actual bluetooth
	 * device having the given MAC address, possibly using the adapter having
	 * the given MAC address.
	 * 
	 * @param deviceMacAddress
	 * @param adapterMacAddress
	 */
	public ManagedBluetoothDevice(String deviceMacAddress,
			String adapterMacAddress)
	{
		// store the device MAC address
		this.deviceMacAddress = deviceMacAddress;
		// store the adapter MAC address
		this.adapterMacAddress = adapterMacAddress;
		// common initialization
		this.init();
	}

	/**
	 * Empty constructor to implement the bean instantiation pattern
	 */
	public ManagedBluetoothDevice()
	{
		// common initialization
		this.init();
	}

	/**
	 * Common initialization
	 */
	private void init()
	{
		// initialize the set of managed services
		this.services = new Hashtable<String, ManagedBluetoothService>();

		// initialize the set of associated device registrations
		this.deviceInfos = new HashSet<BLEDeviceRegistration>();

		// set the polling time at the maximum value
		this.pollingTimeMillis = Integer.MAX_VALUE;
	}

	/**
	 * Gets the polling time with which this device must be sampled, in
	 * milliseconds
	 * 
	 * @return the pollingTimeMillis
	 */
	public int getPollingTimeMillis()
	{
		return pollingTimeMillis;
	}

	/**
	 * Sets the polling time with which this device must be sampled, in
	 * milliseconds
	 * 
	 * @param pollingTimeMillis
	 *            the pollingTimeMillis to set
	 */
	public void setPollingTimeMillis(int pollingTimeMillis)
	{
		this.pollingTimeMillis = pollingTimeMillis;
	}

	/**
	 * Gets the last time, in milliseconds from the epoch, at which this device
	 * has been sampled
	 * 
	 * @return the lastPollFromEpoch
	 */
	public long getLastPollFromEpoch()
	{
		return lastPollFromEpoch;
	}

	/**
	 * Sets the last time, in milliseconds from the epoch, at which this device
	 * has been sampled
	 * 
	 * @param time
	 *            the lastPollFromEpoch to set
	 */
	public void setLastPollFromEpoch(long time)
	{
		this.lastPollFromEpoch = time;
	}

	/**
	 * Gets the MAC address of the device to which this object refers
	 * 
	 * @return the deviceMacAddress
	 */
	public String getDeviceMacAddress()
	{
		return deviceMacAddress;
	}

	/**
	 * Sets the MAC address of the device to which this object refers
	 * 
	 * @param deviceMacAddress
	 *            the deviceMacAddress to set
	 */
	public void setDeviceMacAddress(String deviceMacAddress)
	{
		this.deviceMacAddress = deviceMacAddress;
	}

	/**
	 * Gets the MAC address of the adapter that should, if possible, be used to
	 * access the device represented by this object
	 * 
	 * @return the adapterMacAddress
	 */
	public String getAdapterMacAddress()
	{
		return adapterMacAddress;
	}

	/**
	 * Sets the MAC address of the adapter that should, if possible, be used to
	 * access the device represented by this object
	 * 
	 * @param adapterMacAddress
	 *            the adapterMacAddress to set
	 */
	public void setAdapterMacAddress(String adapterMacAddress)
	{
		this.adapterMacAddress = adapterMacAddress;
	}

	/**
	 * Gets the low-level BluetoothDevice to which this object is associated
	 * 
	 * @return the lowDevice
	 */
	public BluetoothDevice getLowDevice()
	{
		return lowDevice;
	}

	/**
	 * Sets the low-level BluetoothDevice to which this object is associated
	 * 
	 * @param lowDevice
	 *            the lowDevice to set
	 */
	public void setLowDevice(BluetoothDevice lowDevice)
	{
		this.lowDevice = lowDevice;
	}

	/**
	 * Get the set of all services of the represented device which are currently
	 * managed (typically a subset of the actual device services.
	 * 
	 * @return the services
	 */
	public Collection<ManagedBluetoothService> getServices()
	{
		return this.services.values();
	}

	/**
	 * Add the given service to the list of services managed by this device
	 * 
	 * @param service
	 */
	public void addService(ManagedBluetoothService service)
	{
		// TODO add all check and propagation code here
		this.services.put(service.getServiceUUID(), service);
	}

	public ManagedBluetoothService removeService(
			ManagedBluetoothService service)
	{
		// TODO: handle all propagation / logic here
		return this.services.remove(service.getServiceUUID());
	}

	/**
	 * Return the set of device registrations to which this instance is linked
	 * 
	 * @return the deviceInfos
	 */
	public Set<BLEDeviceRegistration> getDeviceRegistrations()
	{
		return deviceInfos;
	}

	public void addBLEDeviceRegistration(BLEDeviceRegistration bleRegistration)
	{
		// check if the registration refers to this device
		if (this.deviceMacAddress
				.equalsIgnoreCase(bleRegistration.getDeviceMacAddress()))
		{
			// add the device registration
			this.deviceInfos.add(bleRegistration);

			// add the reverse link
			bleRegistration.setManagedDevice(this);

			// get the device services
			for (ServiceMonitorSpec serviceSpec : bleRegistration
					.getServiceSpecs())
			{
				ManagedBluetoothService managedService = this.services
						.get(serviceSpec.getServiceUUID());

				// check not null, otherwise create and add
				if (managedService == null)
				{
					// create the managed service
					managedService = new ManagedBluetoothService(
							serviceSpec.getServiceUUID(), this);

					// set the last polling time at now
					managedService
							.setLastPollFromEpoch(System.currentTimeMillis());

					// store the service
					this.services.put(managedService.getServiceUUID(),
							managedService);
				}

				// update the managed service according to the service spec
				managedService.addServiceSpec(serviceSpec);
				
				// update the overall device polling time
				if (this.pollingTimeMillis > managedService
						.getPollingTimeMillis())
					this.pollingTimeMillis = managedService
							.getPollingTimeMillis();
			}
		}
	}

	/**
	 * Removes the given device registration from this
	 * {@link ManagedBluetoothDevice} instance. Thread-safe implementation to
	 * avoid synchronization issues.
	 * 
	 * @param bleRegistration
	 *            The registration to remove
	 * @return true if successfully removed, false otherwise
	 */
	public synchronized boolean removeBLEDeviceRegistration(
			BLEDeviceRegistration bleRegistration)
	{
		boolean removed = false;

		// check if the registration refers to this device
		if (this.deviceMacAddress
				.equalsIgnoreCase(bleRegistration.getDeviceMacAddress()))
		{
			// remove all services spec
			removed = this.deviceInfos.remove(bleRegistration);

			// in principle should not be need, but for safety we remove also
			// back references
			bleRegistration.setManagedDevice(null);

			// the amount of removed services, if 0 no need to recompute the
			// polling time
			int nRemoved = 0;
			// get the spec services and remove all
			for (ServiceMonitorSpec serviceSpec : bleRegistration
					.getServiceSpecs())
			{
				ManagedBluetoothService managedService = this.services
						.get(serviceSpec.getServiceUUID());

				// check not null, otherwise create and add
				if (managedService != null)
				{
					// remove the service spec
					managedService.removeServiceSpec(serviceSpec);

					// update the amount of removed services
					nRemoved++;
				}
			}

			// if at least one service has been removed, update the polling time
			// for the device.
			if (nRemoved > 0)
			{
				// re-compute the polling time
				int pollingTime = Integer.MAX_VALUE;
				for (ManagedBluetoothService service : this.services.values())
				{
					if (pollingTime > service.getPollingTimeMillis())
						pollingTime = service.getPollingTimeMillis();
				}

				// store the new minimum
				this.pollingTimeMillis = pollingTime;
			}
		}
		return removed;
	}

}