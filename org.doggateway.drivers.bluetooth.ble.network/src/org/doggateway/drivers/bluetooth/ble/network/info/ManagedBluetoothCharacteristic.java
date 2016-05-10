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

import java.util.HashSet;
import java.util.Set;

import tinyb.BluetoothGattCharacteristic;

public class ManagedBluetoothCharacteristic
{
	// the UUID of the characteristic to which this "management" data refer
	private String chracteristicUUID;

	// the actual polling time in milliseconds computed for this characteristic
	private int pollingTimeMillis;

	// the last poll timestamp in milliseconds from the Epoch (system-specific)
	private int lastPollFromEpoch;

	// the service management data to which this instance belongs
	private ManagedBluetoothService service;

	// the set of characteristic monitoring specifications originating /
	// contributing to this management instance. Used to trace back
	// "requirements" at polling time.
	private Set<CharacteristicMonitorSpec> monitoringSpecs;

	// the low-level characteristic to which this instance refers
	private BluetoothGattCharacteristic lowCharacteristic;

	/**
	 * Builds a new instance of Managed Bluetooth Characteristic given the
	 * characteristic UUID, the actual pooling time to be used for the
	 * characteristics and the Managed Bluetooth service owning the
	 * characteristic.
	 * 
	 * @param chracteristicUUID
	 *            The UUID of the bluetooth characteristic represented by this
	 *            object
	 * @param pollingTimeMillis
	 *            The polling time in milliseconds for getting values of this
	 *            characteristic
	 * @param service
	 *            The service to which the characteristic belongs
	 */
	public ManagedBluetoothCharacteristic(String chracteristicUUID,
			int pollingTimeMillis, ManagedBluetoothService service)
	{
		// store the characteristic UUID
		this.chracteristicUUID = chracteristicUUID;
		// store the polling time in milliseconds
		this.pollingTimeMillis = pollingTimeMillis;
		// store the service owning the characteristic
		this.service = service;

		// init common
		this.init();
	}

	/**
	 * Create an instance of managed bluetooth characteristic given the actual
	 * characteristic UUID
	 * 
	 * @param chracteristicUUID
	 *            The UUID of the bluetooth characteristic represented by this
	 *            object
	 */
	public ManagedBluetoothCharacteristic(String chracteristicUUID)
	{
		// store the characteristic
		this.chracteristicUUID = chracteristicUUID;

		// init common
		this.init();
	}

	/**
	 * Initializes common data structures
	 */
	private void init()
	{
		// initialize the set of monitoring specs associated to this
		// characteristic
		this.monitoringSpecs = new HashSet<CharacteristicMonitorSpec>();

		// set the polling time at the maximum value
		this.pollingTimeMillis = Integer.MAX_VALUE;
	}

	/**
	 * Gets the UUID of the characteristic to which this
	 * {@link ManagedBluetoothCharacteristic} instance is referred
	 * 
	 * @return the chracteristicUUID The UUID of the characteristic
	 */
	public String getCharacteristicUUID()
	{
		return chracteristicUUID;
	}

	/**
	 * Sets the UUID of the characteristic to which this
	 * {@link ManagedBluetoothCharacteristic} instance is referred
	 * 
	 * @param chracteristicUUID
	 *            The UUID of the characteristic to which this instance shall
	 *            refer.
	 */
	public void setChracteristicUUID(String chracteristicUUID)
	{
		this.chracteristicUUID = chracteristicUUID;
	}

	/**
	 * Gets the pollingTimeMillis with which this characteristic must be
	 * monitored, computed as the minimum between all required polling times of
	 * associated monitoring specs.
	 */
	public int getPollingTimeMillis()
	{
		return pollingTimeMillis;
	}

	/**
	 * Stores the polling time with which this characteristic shall be
	 * monitored. In principle shall be lower or equal to the minimum polling
	 * time required by associated monitoring specs.
	 * 
	 * @param pollingTimeMillis
	 *            the pollingTimeMillis to set
	 */
	public void setPollingTimeMillis(int pollingTimeMillis)
	{
		this.pollingTimeMillis = pollingTimeMillis;
	}

	/**
	 * Get the last time at which the characteristic was sampled, in
	 * milliseconds from the Epoch
	 * 
	 * @return the lastPollFromEpoch
	 */
	public int getLastPollFromEpoch()
	{
		return lastPollFromEpoch;
	}

	/**
	 * Sets the last time at which the characteristic was sampled, in
	 * milliseconds from the Epoch
	 * 
	 * @param lastPollFromEpoch
	 *            the lastPollFromEpoch to set
	 */
	public void setLastPollFromEpoch(int lastPollFromEpoch)
	{
		this.lastPollFromEpoch = lastPollFromEpoch;
	}

	/**
	 * Gets a reference to the {@link ManagedBluetoothService} "owning" this
	 * characteristic
	 * 
	 * @return the service
	 */
	public ManagedBluetoothService getService()
	{
		return service;
	}

	/**
	 * Sets a reference to the {@link ManagedBluetoothService} "owning" this
	 * characteristic
	 * 
	 * @param service
	 *            the service to set
	 */
	public void setService(ManagedBluetoothService service)
	{
		this.service = service;
	}

	/**
	 * Get all the monitoring specs associated to this characteristic, as a live
	 * pointer to the inner set of {@link CharacteristicMonitorSpec} instances.
	 * 
	 * @return the monitoringSpecs
	 */
	public Set<CharacteristicMonitorSpec> getMonitoringSpecs()
	{
		return monitoringSpecs;
	}

	/**
	 * adds a monitoring spec to this instance, recomputes all derived
	 * quantities
	 * 
	 * @param spec
	 */
	public void addCharacteristicMonitorSpec(CharacteristicMonitorSpec spec)
	{
		// update the list of specs for the characteristic
		if (!this.monitoringSpecs.contains(spec))
		{
			// add the spec
			this.monitoringSpecs.add(spec);
			// save the back reference
			spec.setManagedCharacteristic(this);

			// update the polling time for the managed
			// characteristic
			if (this.getPollingTimeMillis() > spec
					.getMaximumAcceptablePollingTimeMillis())
				this.setPollingTimeMillis(
						spec.getMaximumAcceptablePollingTimeMillis());
		}

	}

	/**
	 * removes a monitoring spec from this instance, recomputes all derived
	 * quntities. Thread-safe implementation
	 * 
	 * @param spec
	 *            The characteristic monitor spec to remove
	 * @return true if successfully removed, false otherwise
	 */
	public synchronized boolean removeCharacteristicMonitorSpec(
			CharacteristicMonitorSpec charSpec)
	{
		// simply remove the spec
		boolean removed = this.monitoringSpecs.remove(charSpec);

		// if removed successfully, the managed char polling time shall be
		// recomputed
		if (removed)
		{
			// recompute polling time
			int pollingTime = Integer.MAX_VALUE;

			for (CharacteristicMonitorSpec spec : this.monitoringSpecs)
			{
				if (spec.getMaximumAcceptablePollingTimeMillis() < pollingTime)
					pollingTime = spec.getMaximumAcceptablePollingTimeMillis();
			}

			// update the polling time, again as this is a removal, the new
			// polling time can only be higher or equal to the current one
			if (pollingTime > this.pollingTimeMillis)
				this.pollingTimeMillis = pollingTime;
		}

		return removed;
	}

	/**
	 * Get the low-level Gatt characteristic referred by this instance
	 * 
	 * @return the lowCharacteristic
	 */
	public BluetoothGattCharacteristic getLowCharacteristic()
	{
		return lowCharacteristic;
	}

	/**
	 * Sets the low-level Gatt characteristic to which this instance shall refer
	 * 
	 * @param lowCharacteristic
	 *            the lowCharacteristic to set
	 */
	public void setLowCharacteristic(
			BluetoothGattCharacteristic lowCharacteristic)
	{
		this.lowCharacteristic = lowCharacteristic;
	}

}