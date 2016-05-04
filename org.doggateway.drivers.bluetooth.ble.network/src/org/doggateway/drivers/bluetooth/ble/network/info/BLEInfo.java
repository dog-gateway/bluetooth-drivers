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

import it.polito.elite.dog.core.library.model.ConfigurationConstants;

/**
 * @author bonino
 *
 */
public class BLEInfo extends ConfigurationConstants
{
	// the device ma address
	public static final String MAC = "macAddress";
	
	// the UUID of a service
	public static final String SERVICE_UUID = "serviceUUID";
	
	// the UUID of a characteristic
	public static final String CHARACTERISTIC_UUID = "characteristicUUID";
	
	// the UUID of a notify enabling/disabling characteristic
	public static final String NOTIFY_ENABLE_UUID = "notifyEnableUUID";
	
	// the value to enable notification
	public static final String NOTIFY_ENABLE_VALUE = "notifyEnableValue";
	
	// the value to enable notification
	public static final String NOTIFY_DISABLE_VALUE = "notifyDisableValue";
	
	//one of the standard data types or custom
	public static final String DATA_TYPE = "bluetoothDataType";
	
	// the manufacturer id for bluetooth
	public static final String MANUFACTURER = "Bluetooth+LE";
}
