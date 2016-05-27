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
package org.doggateway.drivers.bluetooth.ble.gateway.interfaces;

/**
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public interface BLEAdapterDriver
{
	/**
	 * Checks if the device (adapter) having the given device ID is currently
	 * in the set of devices managed by the adapter driver
	 * 
	 * @param deviceId
	 *            The dog-configuration-defined ID of the adapter
	 * @return true if the adapter is part of the set currently managed, false
	 *         otherwise
	 */
	boolean isGatewayAvailable(String deviceId);
	
	/** 
	 * Gets the MAC address of the given gateway, if exists
	 * @param deviceId
	 * @return the gateway mac or null
	 */
	public String getGatewayMacAddress(String deviceId);

}
