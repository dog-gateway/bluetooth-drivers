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

import java.util.HashSet;
import java.util.concurrent.Callable;

import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLEDiscoveryListener;

import tinyb.BluetoothDevice;

/**
 * A task for dispatching discovery data to declared listeners
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public class DispatchDiscoveryTask implements Callable<Void>
{
	//the discovered bluetooth device
	private BluetoothDevice device;
	
	//the discovery listeners
	private HashSet<BLEDiscoveryListener> listeners;
	
	//the managed flag that indicates if the device is already managed by Dog or not
	private boolean managed;

	/**
	 * Class constructor, collects parameters to dispatch, and target listeners
	 * @param lowDevice The discovered bluetooth device
	 * @param listeners The registered listeners
	 * @param managed The managed flag
	 */
	public DispatchDiscoveryTask(BluetoothDevice lowDevice,
			HashSet<BLEDiscoveryListener> listeners, boolean managed)
	{
		// store needed data
		this.device = lowDevice;
		this.listeners = listeners;
		this.managed = managed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Void call() throws Exception
	{
		synchronized (listeners)
		{
			for (BLEDiscoveryListener listener : this.listeners)
			{
				//dispatch the discovery information
				listener.discoveredDevice(this.device.getName(),
						this.device.getAddress(), this.device.getRssi(),
						this.managed);
			}
		}
		return null;
	}

}
