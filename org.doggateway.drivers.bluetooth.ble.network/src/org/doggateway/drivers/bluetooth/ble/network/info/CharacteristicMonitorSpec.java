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

public class CharacteristicMonitorSpec
{
	// the UUID of the characteristic to which this monitoring spec is referred
	private String characteristicUUID;

	// the maximum polling time which can be considered "acceptable" according
	// to this monitoring spec
	private int maximumAcceptablePollingTimeMillis;
	// the {@link ManagedBluetoothCharacteristic} to satisfying this monitoring
	// spec
	private ManagedBluetoothCharacteristic managedCharacteristic;

	// the service spec to which this characteristic monitor spec belongs
	private ServiceMonitorSpec serviceSpec;

	/**
	 * Builds a new instance of CharacteristicMonitorSpec that specifies the
	 * maximum acceptable polling time for the given Bluetooth Gatt
	 * characteristic
	 * 
	 * i@param characteristicUUID The characteristic UUID
	 * 
	 * @param maximumAcceptablePollingTimeMillis
	 *            The maximum acceptable polling time in milliseconds
	 */
	public CharacteristicMonitorSpec(String characteristicUUID,
			int maximumAcceptablePollingTimeMillis)
	{
		// store the characteristic UUID
		this.characteristicUUID = characteristicUUID;
		// store the maximum acceptable polling time
		this.maximumAcceptablePollingTimeMillis = maximumAcceptablePollingTimeMillis;
	}

	/**
	 * Builds a new instance of CharacteristicMonitorSpec that specifies the
	 * maximum acceptable polling time for the given Bluetooth Gatt
	 * characteristic, part of the given ServiceMonitorSpec
	 * 
	 * @param characteristicUUID
	 *            The characteristic UUID
	 * @param maximumAcceptablePollingTimeMillis
	 *            The maximum acceptable polling time in milliseconds
	 * @param serviceSpec
	 *            The ServiceMonitorSpec to which the CharacteristicMonitorSpec
	 *            belongs
	 */
	public CharacteristicMonitorSpec(String characteristicUUID,
			int maximumAcceptablePollingTimeMillis,
			ServiceMonitorSpec serviceSpec)
	{
		// store the characteristic UUID
		this.characteristicUUID = characteristicUUID;
		// store the maximum acceptable polling time
		this.maximumAcceptablePollingTimeMillis = maximumAcceptablePollingTimeMillis;
		// store a reference to the service monitoring spec to which this
		// characteristic spec belongs
		this.serviceSpec = serviceSpec;
	}

	/**
	 * Get the UUID of the Bluetooth Gatt characteristic to which this spec is associated
	 * @return the characteristicUUID
	 */
	public String getCharacteristicUUID()
	{
		return characteristicUUID;
	}

	/**
	 * Sets the UUID of the Bluetooth Gatt characteristic to which this spec is associated
	 * @param characteristicUUID
	 *            the characteristicUUID to set
	 */
	public void setCharacteristicUUID(String characteristicUUID)
	{
		this.characteristicUUID = characteristicUUID;
	}

	/**
	 * Get the maximum acceptable polling time for the characteristic referred by this object
	 * @return the maximumAcceptablePollingTimeMillis
	 */
	public int getMaximumAcceptablePollingTimeMillis()
	{
		return maximumAcceptablePollingTimeMillis;
	}

	/**
	 * Sets the maximum acceptable polling time for the characteristic referred by this object
	 * @param maximumAcceptablePollingTimeMillis
	 *            the maximumAcceptablePollingTimeMillis to set
	 */
	public void setMaximumAcceptablePollingTimeMillis(
			int maximumAcceptablePollingTimeMillis)
	{
		this.maximumAcceptablePollingTimeMillis = maximumAcceptablePollingTimeMillis;
	}

	/**
	 * Gets the {@link ManagedBluetoothCharacteristic} satisying this spec
	 * @return the managedCharacteristic
	 */
	public ManagedBluetoothCharacteristic getManagedCharacteristic()
	{
		return managedCharacteristic;
	}

	/**
	 * Sets the {@link ManagedBluetoothCharacteristic} satisying this spec
	 * @param managedCharacteristic
	 *            the managedCharacteristic to set
	 */
	public void setManagedCharacteristic(
			ManagedBluetoothCharacteristic managedCharacteristic)
	{
		this.managedCharacteristic = managedCharacteristic;
	}

	/**
	 * Get the {@link ServiceMonitorSpec} to which this spec belongs
	 * @return the serviceSpec
	 */
	public ServiceMonitorSpec getServiceSpec()
	{
		return serviceSpec;
	}

	/**
	 * Set the {@link ServiceMonitorSpec} to which this spec belongs
	 * @param serviceSpec
	 *            the serviceSpec to set
	 */
	public void setServiceSpec(ServiceMonitorSpec serviceSpec)
	{
		this.serviceSpec = serviceSpec;
	}

}