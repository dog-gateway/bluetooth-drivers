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

import java.util.Hashtable;

import tinyb.BluetoothGattService;

public class ManagedBluetoothService
{
	// the UUID of the service to which this instance is associated
	private String serviceUUID;

	// the polling time for this service, computed as minimum between polling
	// times of all characteristics associated to this service
	private int pollingTimeMillis;

	// the last time, in milliseconds from the Epoch, at which this service has
	// been polled
	private long lastPollFromEpoch;

	// the device "offering" this service
	private ManagedBluetoothDevice device;

	// the set of characteristics of this service that shall be monitored.
	private Hashtable<String, ManagedBluetoothCharacteristic> characteristics;

	// the low-level service to which this object refers
	private BluetoothGattService lowService;

	/**
	 * Builds a new instance of {@link ManagedBluetoothService} associated with
	 * the actual bluetooth device service identified by the given UUID
	 * 
	 * @param serviceUUID
	 *            The service UUID
	 * @param device
	 *            The device "owning" the service
	 */
	public ManagedBluetoothService(String serviceUUID,
			ManagedBluetoothDevice device)
	{
		super();
		this.serviceUUID = serviceUUID;
		this.device = device;

		// common initialization
		this.init();
	}

	/**
	 * Empty constructor to implement the bean instantiation pattern
	 */
	public ManagedBluetoothService()
	{
		this.init();
	}

	/**
	 * Initializes common data structures
	 */
	private void init()
	{
		// initialize the list of managed characteristics
		this.characteristics = new Hashtable<String, ManagedBluetoothCharacteristic>();

		// set the polling time at the maximum value
		this.pollingTimeMillis = Integer.MAX_VALUE;
	}

	/**
	 * Gets the UUID of the service to which this instance is associated
	 * 
	 * @return the serviceUUID
	 */
	public String getServiceUUID()
	{
		return serviceUUID;
	}

	/**
	 * Sets the UUID of the service to which this instance is associated
	 * 
	 * @param serviceUUID
	 *            the serviceUUID to set
	 */
	public void setServiceUUID(String serviceUUID)
	{
		this.serviceUUID = serviceUUID;
	}

	/**
	 * Gets the polling time with which this service shall be monitored, usually
	 * computed as the minimum between polling times of all associated
	 * characteristics
	 * 
	 * @return the pollingTimeMillis
	 */
	public int getPollingTimeMillis()
	{
		return pollingTimeMillis;
	}

	/**
	 * Sets the polling time with which this service shall be monitored, usually
	 * computed as the minimum between polling times of all associated
	 * characteristics
	 * 
	 * @param pollingTimeMillis
	 *            the pollingTimeMillis to set
	 */
	public void setPollingTimeMillis(int pollingTimeMillis)
	{
		this.pollingTimeMillis = pollingTimeMillis;
	}

	/**
	 * Gets the last time, in milliseconds from the epoch, at which this
	 * service, i.e., one of its characteristics has been polled.
	 * 
	 * @return the lastPollFromEpoch
	 */
	public long getLastPollFromEpoch()
	{
		return lastPollFromEpoch;
	}

	/**
	 * Sets the last time, in milliseconds from the epoch, at which this
	 * service, i.e., one of its characteristics, has been polled
	 * 
	 * @param l
	 *            the lastPollFromEpoch to set
	 */
	public void setLastPollFromEpoch(long l)
	{
		this.lastPollFromEpoch = l;
	}

	/**
	 * Gets the device "offering" this service
	 * 
	 * @return the device
	 */
	public ManagedBluetoothDevice getDevice()
	{
		return device;
	}

	/**
	 * Sets the device "offering this service"
	 * 
	 * @param device
	 *            the device to set
	 */
	public void setDevice(ManagedBluetoothDevice device)
	{
		this.device = device;
	}

	/**
	 * Gets all the characteristics to monitor that are associated to this
	 * service
	 * 
	 * @return the characteristics
	 */
	public Hashtable<String, ManagedBluetoothCharacteristic> getCharacteristics()
	{
		return characteristics;
	}

	public void addManagedBluetoothCharacteristic(
			ManagedBluetoothCharacteristic characteristic)
	{
		// add the characteristic
		this.characteristics.put(characteristic.getCharacteristicUUID(),
				characteristic);

		// TODO: complete the process here, check more precisely what has to be
		// done on insertion / deletion
	}

	public ManagedBluetoothCharacteristic removeManagedBluetoothCharacteristic(
			ManagedBluetoothCharacteristic characteristic)
	{
		// remove the characteristic
		return this.characteristics
				.remove(characteristic.getCharacteristicUUID());

		// TODO: complete the process here, check more precisely what has to be
		// done on insertion / deletion
	}

	/**
	 * Fills this Managed Service with data / references coming from
	 * corresponding service monitoring specification
	 * 
	 * @param serviceSpec
	 */
	public void addServiceSpec(ServiceMonitorSpec serviceSpec)
	{
		// add characteristics
		for (CharacteristicMonitorSpec charSpec : serviceSpec
				.getCharacteristicSpecs())
		{
			// check if the managed service already handles the
			// characteristic, otherwise add it
			ManagedBluetoothCharacteristic managedCharacteristic = this
					.characteristics.get(charSpec.getCharacteristicUUID());

			// check null
			if (managedCharacteristic == null)
			{
				// No characteristic exist with the given UUID, create a
				// new one
				managedCharacteristic = new ManagedBluetoothCharacteristic(
						charSpec.getCharacteristicUUID(),
						charSpec.getMaximumAcceptablePollingTimeMillis(), this);

				//add the characteristic
				 this.characteristics.put(managedCharacteristic.getCharacteristicUUID(), managedCharacteristic);
							

			}

			// just add the char spec to the set of specs satisfied by the
			// managed characteristic
			managedCharacteristic.addCharacteristicMonitorSpec(charSpec);

			// update the computed service polling time
			if (this.pollingTimeMillis > managedCharacteristic
					.getPollingTimeMillis())
				this.pollingTimeMillis = managedCharacteristic.getPollingTimeMillis();
		}
	}

	/**
	 * Removes the service spec associated to this managed service
	 * 
	 * @param serviceSpec
	 * @return
	 */
	public boolean removeServiceSpec(ServiceMonitorSpec serviceSpec)
	{
		boolean removed = false;

		// remove all characteristic specs
		for (CharacteristicMonitorSpec charSpec : serviceSpec
				.getCharacteristicSpecs())
		{
			// check if the managed service handles the
			// characteristic, otherwise there is no need to remove it
			ManagedBluetoothCharacteristic managedCharacteristic = this
					.getCharacteristics().get(charSpec.getCharacteristicUUID());

			// check null
			if (managedCharacteristic != null)
			{
				// remove the spec
				if (managedCharacteristic
						.removeCharacteristicMonitorSpec(charSpec))
				{
					// remove back reference for safety purposes
					charSpec.setManagedCharacteristic(null);

					// check if the managedCharacteristic has still some spec
					// pointing at it, otherwise remove it
					if (managedCharacteristic.getMonitoringSpecs().size() < 1)
					{
						// remove back reference
						managedCharacteristic.setService(null);

						// remove the characteristic
						this.characteristics
								.remove(charSpec.getCharacteristicUUID());
					}

					// set the removed flag at true
					removed = true;
				}
			}
		}

		// recompute the polling time required for this service
		int pollingTime = Integer.MAX_VALUE;
		for (ManagedBluetoothCharacteristic managedChar : this.characteristics
				.values())
		{
			if (pollingTime > managedChar.getPollingTimeMillis())
				pollingTime = managedChar.getPollingTimeMillis();
		}

		// if changed, update it. As this is a removal operation, any change can
		// only lead to higher polling times
		if (pollingTime > this.pollingTimeMillis)
			this.pollingTimeMillis = pollingTime;

		return removed;

	}

	/**
	 * Gets the low-level service described by this managed service instance
	 * 
	 * @return the lowService
	 */
	public BluetoothGattService getLowService()
	{
		return lowService;
	}

	/**
	 * Sets the low-level service to which this instance is referred
	 * 
	 * @param lowService
	 *            the lowService to set
	 */
	public void setLowService(BluetoothGattService lowService)
	{
		this.lowService = lowService;
	}

}