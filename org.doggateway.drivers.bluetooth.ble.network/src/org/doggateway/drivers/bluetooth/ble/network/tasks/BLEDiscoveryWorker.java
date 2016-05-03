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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.doggateway.drivers.bluetooth.ble.network.BLENetworkDriverImpl;
import org.doggateway.drivers.bluetooth.ble.network.info.ManagedBluetoothDevice;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.DiscoveryListener;
import org.osgi.service.log.LogService;

import tinyb.BluetoothDevice;

/**
 * @author bonino
 *
 */
public class BLEDiscoveryWorker extends Thread
{
	// the driver
	private BLENetworkDriverImpl theDriver;

	// the running flag
	private boolean canRun;
	
	// the set of discovery listeners
	private HashSet<DiscoveryListener> listeners;
	
	// the dispatcher service
	private ExecutorService dispatcher;

	/**
	 * Builds a new instance of discovery thread, TODO: evaluate how to handle
	 * newly discovered devices...
	 * 
	 * @param theDriver
	 */
	public BLEDiscoveryWorker(BLENetworkDriverImpl theDriver)
	{
		super();
		//store the driver reference
		this.theDriver = theDriver;
		
		//create the set of discovery listeners
		this.listeners = new HashSet<DiscoveryListener>();
		
		//create the executor service, single threaded,
		this.dispatcher = Executors.newCachedThreadPool();
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run()
	{
		while (!this.isInterrupted() && canRun)
		{
			for (int i = 0; ((i < this.theDriver.getDiscoveryTrials())
					&& (!this.isInterrupted()) && (canRun)); i++)
			{
				// open the adapter in discovery, wait for a while and check if
				// any
				// of
				// the devices listed in the waiting to discover list are
				// available.
				this.theDriver.setDiscovery(true);

				// get the currently available devices
				List<BluetoothDevice> devices = this.theDriver
						.getLowLevelDevices();

				// iterate over the devices waiting for discovery
				ArrayList<ManagedBluetoothDevice> devicesWaitingForDiscovery = this.theDriver
						.getManagedDevicesWaitingForDiscovery();

				for (BluetoothDevice lowDevice : devices)
				{

					// log device
					this.theDriver.getLogger().log(LogService.LOG_INFO,
							"Found: " + lowDevice.getName() + "[MAC: "
									+ lowDevice.getAddress() + "][RSSI: "
									+ lowDevice.getRssi() + "]");
					
					//try to detect managed devices waiting for discovery
					for (ManagedBluetoothDevice deviceToDiscover : devicesWaitingForDiscovery)
					{
						if (deviceToDiscover.getDeviceMacAddress()
								.equalsIgnoreCase(lowDevice.getAddress()))
						{
							// attach the device
							deviceToDiscover.setLowDevice(lowDevice);

							// remove the device from the list of devices
							// waiting
							// for discovery
							this.theDriver.discoveredDevice(deviceToDiscover);

							// should break here...
							break;
						}

					}
					
					//dispatch discovery
					if(this.theDriver.getManagedDevice(lowDevice.getAddress())!=null)
					{
						// notify listeners
						this.dispatcher.submit(new DispatchDiscoveryTask(lowDevice,listeners,true));
					}
					else
					{
						// notify listeners
						this.dispatcher.submit(new DispatchDiscoveryTask(lowDevice,listeners,false));
					}

				}
				// sleep
				try
				{
					Thread.sleep(this.theDriver.getDiscoveryCyclingTimeMillis());
				}
				catch (InterruptedException e)
				{
					// TODO: handle exception
				}
			}
			// stop discovery
			this.theDriver.setDiscovery(false);

			// sleep
			try
			{
				Thread.sleep(this.theDriver.getDiscoveryIntervalMillis());
			}
			catch (InterruptedException e)
			{
				// TODO: handle exception
			}
		}
	}

	/**
	 * Sets/unsets the runnable flag of the thread. If the thread is running and
	 * the flag is set at false, the thread ends gracefully.
	 * 
	 * @param canRun
	 */
	public void setRunnable(boolean canRun)
	{
		this.canRun = canRun;
	}
	
	/**
	 * Adds a discovery listener to the set of listeners to be notified about the discovery of new devices
	 * @param listener The listener to notify
	 */
	public void addDiscoveryListener(DiscoveryListener listener)
	{
		synchronized (this.listeners)
		{
			this.listeners.add(listener);
		}
	}
	
	/**
	 * Removes a discovery listener from the set of listeners to be notified about the discovery of new devices
	 * @param listener The listener to remove
	 * @return true if removed, false otherwise
	 */
	public boolean removeDiscoveryListener(DiscoveryListener listener)
	{
		boolean removed = false;
		synchronized (this.listeners)
		{
			removed = this.listeners.remove(listener);
		}
		
		return removed;
	}

}
