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
package org.doggateway.drivers.bluetooth.ble.network.interfaces;

import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.info.BLEDeviceRegistration;

/**
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public interface BLENetwork
{
	/**
	 * Adds the device described by the given {@link BLEDeviceRegistration}
	 * instance to the set of devices handled by this network driver. The
	 * provided {@link BLEDriverInstance} will be notified about changes in the
	 * device characteristics.
	 * 
	 * @param devInfo
	 *            The {@link BLEDeviceRegistration} describing the device to
	 *            add.
	 */
	public void addDeviceRegistration(BLEDeviceRegistration devReg);

	/**
	 * Removes a given device-specific driver from the set of drivers
	 * "connected" to the network driver. This implies that all devices being
	 * connected to the removed driver only will also be "removed" from the set
	 * managed by the network driver and will not be reachable anymore by other
	 * platform bundles.
	 * 
	 * @param driverInstance
	 *            The driver to remove.
	 */
	public void removeAllDriverRegistrations(BLEDriverInstance driverInstance);

	/**
	 * Writes the given raw value to the given characteristic of the device
	 * associated to the given driver
	 * 
	 * @param driver
	 *            The driver to which the device is associate
	 * @param characteristicUUID
	 *            The UUID of the characteristic to write
	 * @param value
	 *            The value to write.
	 * @return true if successfully written, false otherwise
	 */
	public boolean writeValue(String deviceMacAddress, String serviceUUID,
			String characteristicUUID, byte[] value);

	/**
	 * Performs a direct read on the given characteristic for the device having
	 * the given mac address. This can only be applied if the device is part of
	 * devices currently managed by the network driver and if the device is
	 * connected.
	 * 
	 * @param deviceMacAddress
	 *            The MAC address of the device for which direct read shall be
	 *            performed
	 * @param serviceUUID
	 *            The UUID of the service to which belongs the characteristic to
	 *            be read
	 * @param characteristicUUID
	 *            The UUID of the characteristic to read
	 * @return the value read or null if read operation is not possible
	 */
	public byte[] readValue(String deviceMacAddress, String serviceUUID,
			String characteristicUUID);

	/**
	 * Starts the discovery mode on the default adapter
	 */
	public void startDiscovery();
	
	/**
	 * Stop the discovery mode on the default adapter
	 */
	public void stopDiscovery();

	/**
	 * Adds a discovery listener to the set of listeners to be notified about the discovery of new devices
	 * @param listener The listener to notify
	 */
	public void addDiscoveryListener(BLEDiscoveryListener listener);

	/**
	 * Removes a discovery listener from the set of listeners to be notified about the discovery of new devices
	 * @param listener The listener to remove
	 * @return true if removed, false otherwise
	 */
	public boolean removeDiscoveryListener(BLEDiscoveryListener listener);

}
