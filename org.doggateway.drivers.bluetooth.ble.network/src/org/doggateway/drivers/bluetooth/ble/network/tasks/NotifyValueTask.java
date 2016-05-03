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
package org.doggateway.drivers.bluetooth.ble.network.tasks;

import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;

/**
 * @author bonino
 *
 */
public class NotifyValueTask implements Runnable
{
	//the UUID of the characteristic for which the new value shall be dispatched
	private String characteristicUUID;
	
	//the UUID of the service to which the dispatched characteristic belongs
	private String serviceUUID;
	
	//the calue to dispatch
	private byte[] value;
	
	//the sriver instance to which the value shall be dispatched
	private BLEDriverInstance drvInstance;
	
	
	public NotifyValueTask(String characteristicUUID, String serviceUUID,
			byte[] value, BLEDriverInstance drvInstance)
	{
		// store the values
		this.characteristicUUID = characteristicUUID;
		this.serviceUUID = serviceUUID;
		this.value = value;
		this.drvInstance = drvInstance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public void run()
	{
		//dispatch to the usual event handling method on driver instances...
		this.drvInstance.newMessageFromHouse(this.characteristicUUID, this.serviceUUID, this.value);
	}

}
