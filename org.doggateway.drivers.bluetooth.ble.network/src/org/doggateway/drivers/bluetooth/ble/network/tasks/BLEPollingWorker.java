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

import org.doggateway.drivers.bluetooth.ble.network.BLENetworkDriverImpl;
import org.doggateway.drivers.bluetooth.ble.network.info.ManagedBluetoothCharacteristic;
import org.doggateway.drivers.bluetooth.ble.network.info.ManagedBluetoothDevice;
import org.doggateway.drivers.bluetooth.ble.network.info.ManagedBluetoothService;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public class BLEPollingWorker extends Thread
{
	// the driver
	private BLENetworkDriverImpl theDriver;

	// the running flag
	private boolean canRun;

	/**
	 * @param theDriver
	 */
	public BLEPollingWorker(BLENetworkDriverImpl theDriver)
	{
		// store a reference to the owning driver
		this.theDriver = theDriver;

		// initially can run
		this.canRun = true;
	}

	/**
	 * Changes the "runnable" state of the worker thread, if the thread is
	 * running, setting this at false forces the thread to exit gracefully after
	 * completing the last tasks.
	 * 
	 * @param canRun
	 */
	public void setRunnable(boolean canRun)
	{
		this.canRun = canRun;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		while ((!this.isInterrupted()) && canRun)
		{
			// iterate over all managed devices
			for (String deviceMac : this.theDriver
					.getManagedBluetoothDeviceAddresses())
			{
				// quick stop
				if ((!this.isInterrupted()) && canRun)
				{
					// this shall be atomic...
					ManagedBluetoothDevice device = this.theDriver
							.getManagedDevice(deviceMac);

					// check if available
					if (device.getLowDevice() != null)
					{
						// TODO: check if this makes any sense
						synchronized (device)
						{
							// use this time for all checks to preserve
							// coherence of
							// checks
							long time = System.currentTimeMillis();

							if (time - device.getLastPollFromEpoch() >= device
									.getPollingTimeMillis())
							{
								// device shall be polled
								for (ManagedBluetoothService currentService : device
										.getServices())
								{
									
									// re-check for polling need
									if (time - currentService
											.getLastPollFromEpoch() >= currentService
													.getPollingTimeMillis())
									{
										// this service shall be polled
										for (ManagedBluetoothCharacteristic currentCharacteristic : currentService
												.getCharacteristics().values())
										{
											// check if needs polling
											if (time - currentCharacteristic
													.getLastPollFromEpoch() >= currentCharacteristic
															.getPollingTimeMillis())
											{
												long eTime = System.currentTimeMillis();
												
												// poll the characteristic
												byte[] value = this.theDriver
														.readValue(device,
																currentService
																		.getServiceUUID(),
																currentCharacteristic
																		.getCharacteristicUUID());
												
												this.theDriver.getLogger().log(LogService.LOG_INFO,"Elapsed: "+(System.currentTimeMillis()-eTime));

												// dispatch the results
												// TODO check if shall be done
												// in a
												// separate thread??
												this.theDriver.notifyNewValue(
														currentCharacteristic
																.getCharacteristicUUID(),
														value,
														currentService
																.getServiceUUID(),
														currentCharacteristic
																.getMonitoringSpecs());
												
												//adjust last read time stamp
												currentCharacteristic.setLastPollFromEpoch(time);
												currentService.setLastPollFromEpoch(time);
												device.setLastPollFromEpoch(time);
											}
										}
									}

								}
							}
						}
					}
				}
				Thread.yield();
			}

			// sleep for the current polling time
			try
			{
				this.theDriver.getLogger().log(LogService.LOG_INFO,"Sleeping for: "+theDriver.getActualPollingTimeMillis());
				Thread.sleep(theDriver.getActualPollingTimeMillis());
			}
			catch (InterruptedException e)
			{
				this.interrupt();
			}
		}
	}

}
