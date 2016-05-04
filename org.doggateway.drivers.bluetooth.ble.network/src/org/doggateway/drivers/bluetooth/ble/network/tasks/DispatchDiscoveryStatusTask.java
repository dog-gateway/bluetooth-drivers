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

/**
 * @author bonino
 *
 */
public class DispatchDiscoveryStatusTask implements Callable<Void>
{
	// the discovery state to dispatch
	private boolean discoveryState;
	
	//the discovery listeners
	private HashSet<BLEDiscoveryListener> listeners;
	
	/**
	 * create the discovery status task
	 * @param listeners listeners to notify
	 * @param discoveryState status to propagate
	 */
	public DispatchDiscoveryStatusTask(HashSet<BLEDiscoveryListener> listeners, boolean discoveryState)
	{
		this.discoveryState = discoveryState;
	}
	@Override
	public Void call() throws Exception
	{
		synchronized (listeners)
		{
			for (BLEDiscoveryListener listener : this.listeners)
			{
				//dispatch the discovery information
				listener.discoveryEnabled(discoveryState);
			}
		}
		return null;
	}

}
