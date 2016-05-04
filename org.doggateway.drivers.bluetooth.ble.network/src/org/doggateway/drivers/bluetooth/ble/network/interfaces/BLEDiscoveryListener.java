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

/**
 * An interface to be notified about the discovery of new devices at the network
 * level
 * 
 * @author bonino
 *
 */
public interface BLEDiscoveryListener
{
	/**
	 * Notifies the discovery of a new device
	 * 
	 * @param devName
	 *            The device name
	 * @param devAddress
	 *            The device MAC address
	 * @param rssi
	 *            The current RSSI level for the device
	 * @param managed
	 *            A flag indicating whether the device is already managed by Dog
	 *            or not
	 */
	public void discoveredDevice(String devName, String devAddress, short rssi,
			boolean managed);
	
	/**
	 * Notifies the current discovery status
	 * @param enabled
	 */
	public void discoveryEnabled(boolean enabled);
}
