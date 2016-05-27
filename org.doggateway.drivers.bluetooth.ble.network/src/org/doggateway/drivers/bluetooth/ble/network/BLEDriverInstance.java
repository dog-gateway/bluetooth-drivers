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
package org.doggateway.drivers.bluetooth.ble.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.doggateway.drivers.bluetooth.ble.network.info.BLEDeviceRegistration;
import org.doggateway.drivers.bluetooth.ble.network.info.BLEInfo;
import org.doggateway.drivers.bluetooth.ble.network.info.CharacteristicMonitorSpec;
import org.doggateway.drivers.bluetooth.ble.network.info.ServiceMonitorSpec;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.core.library.model.CNParameters;
import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.StatefulDevice;
import it.polito.elite.dog.core.library.util.ElementDescription;
import it.polito.elite.dog.core.library.util.LogHelper;

/**
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public abstract class BLEDriverInstance implements StatefulDevice
{
	// a reference to the network driver
	protected BLENetwork network;

	// the device registration corresponding to the device managed by the driver
	// TODO: this must be extended/amended for drivers exploiting more than one
	// physical device at time
	protected BLEDeviceRegistration bleDevReg;

	// the state of the device associated to this driver
	protected DeviceStatus currentState;

	// the device associated to the driver
	protected ControllableDevice device;

	// the gateway mac address
	protected String gwAddress;
	
	// the managed device mac
	protected String macAddress;

	// the driver polling time (each implementation might override the default
	// behavior)
	protected int pollingTimeMillis;

	// the logger
	protected LogHelper logger;

	// the set of notifications associated to the driver
	protected HashMap<String, CNParameters> notifications;

	// the set of commands associated to the driver
	protected HashMap<String, CNParameters> commands;

	// the adapter flag
	private boolean isAdapter = false;

	/**
	 * Class constructor, takes a reference to the network driver to exploit for
	 * communication and to the Dog device instance to handle.
	 * 
	 * TODO: all driver instance constructors have the same structure/code,
	 * define a generics-enabled definition in the library
	 * 
	 * @param network
	 *            The network driver service to use
	 * @param device
	 *            The Dog device to handle
	 * @param gwMacAddress
	 *            The MAC address of the adapter to be used by this driver
	 *            instance (mainly for future use)
	 */
	public BLEDriverInstance(BLENetwork network, ControllableDevice device,
			String gwMacAddress, int pollingTimeMillis, LogHelper logger)
	{
		// store a reference to the network driver
		this.network = network;

		// store a reference to the associated device
		this.device = device;

		// store the gateway address
		this.gwAddress = gwMacAddress;

		// compute the adapter flag: no GW MAC -> adapter
		this.isAdapter = (this.gwAddress == null);

		// store the logger
				this.logger = logger;
		
		// store the polling time millis
		this.pollingTimeMillis = pollingTimeMillis;

		System.err.println("Fill config polling time "+pollingTimeMillis);

		// initialize datastructures
		this.notifications = new HashMap<String, CNParameters>();
		this.commands = new HashMap<String, CNParameters>();

		// fill the data structures depending on the specific device
		// configuration parameters
		this.fillConfiguration();

		// call the specific configuration method, if needed
		this.specificConfiguration();

		// associate the device-specific driver to the network driver...
		// not for adapters
		if (!this.isAdapter)
			this.addToNetworkDriver(this.bleDevReg);
	}

	/**
	 * Extending classes might implement this method to provide driver-specific
	 * configurations to be done during the driver creation process, before
	 * associating the device-specific driver to the network driver
	 */
	protected abstract void specificConfiguration();

	/**
	 * Abstract method to be implemented by extending classes; performs the
	 * association between the device-specific driver and the underlying network
	 * driver using the appliance data as binding.
	 * 
	 * @param serial
	 */
	protected abstract void addToNetworkDriver(BLEDeviceRegistration bleDevReg);

	private void fillConfiguration()
	{
		// gets the properties shared by almost all Bluetooth Low Energy
		// devices, i.e. the
		// MAC address
		Map<String, Set<String>> deviceConfigurationParams = this.device
				.getDeviceDescriptor().getSimpleConfigurationParams();

		// check not null
		if (deviceConfigurationParams != null)
		{
			// get the device mac
			Set<String> macAddresses = deviceConfigurationParams
					.get(BLEInfo.MAC);

			// get the "single" mac
			// TODO: change here to handle multiple devices associated to the
			// same device driver
			if ((macAddresses != null) && (macAddresses.size() == 1))
			{
				// the mac address
				this.macAddress = macAddresses.iterator().next();

				// mandatory information available
				// adapter is null at this stage and may be later filled with
				// gateway-level information
				// TODO: check if it sounds good
				BLEDeviceRegistration devReg = new BLEDeviceRegistration(
						macAddress, this.gwAddress, this);

				// get remaining configuration parameters

				// gets the properties associated to each device
				// commmand/notification,
				// if any. E.g.,
				// the unit of measure associated to meter functionalities.

				// get parameters associated to each device command (if any)
				Set<ElementDescription> commandsSpecificParameters = this.device
						.getDeviceDescriptor().getCommandSpecificParams();

				// get parameters associated to each device notification (if
				// any)
				Set<ElementDescription> notificationsSpecificParameters = this.device
						.getDeviceDescriptor().getNotificationSpecificParams();

				// --------------- Handle command specific parameters
				// generic handling, all parameters are accepted and retained,
				// specific driver implementations might "decide" how to exploit
				// parameters.
				for (ElementDescription parameter : commandsSpecificParameters)
				{

					// the parameter map
					Map<String, String> params = parameter.getElementParams();
					if ((params != null) && (!params.isEmpty()))
					{
						// the name of the command associated to this device...
						String commandName = params.get(BLEInfo.COMMAND_NAME);

						if (commandName != null)
							// store the parameters associated to the command
							this.commands.put(commandName,
									new CNParameters(commandName, params));

					}
				}

				// --------------- Handle notification specific parameters
				// generic handling, all parameters are accepted and retained,
				// specific driver implementations might "decide" how to exploit
				// parameters.
				for (ElementDescription parameter : notificationsSpecificParameters)
				{
					// the parameter map
					Map<String, String> params = parameter.getElementParams();
					if ((params != null) && (!params.isEmpty()))
					{
						// the name of the command associated to this device...
						String notificationName = params
								.get(BLEInfo.NOTIFICATION_NAME);

						if (notificationName != null)
							// store the parameters associated to the command
							this.notifications.put(notificationName,
									new CNParameters(notificationName, params));

						// the service UUID associated to the notification
						String serviceUUID = params.get(BLEInfo.SERVICE_UUID);
						String characteristicUUID = params
								.get(BLEInfo.CHARACTERISTIC_UUID);

						if ((serviceUUID != null) && (!serviceUUID.isEmpty())
								&& (characteristicUUID != null)
								&& (!characteristicUUID.isEmpty()))
						{
							// update the bleDevReg field
							this.updateDeviceRegistration(devReg, serviceUUID,
									characteristicUUID);
						}
					}

				}

				// store the device registration
				this.bleDevReg = devReg;
			}
		}
	}

	private void updateDeviceRegistration(BLEDeviceRegistration devReg,
			String serviceUUID, String characteristicUUID)
	{
		// check not null
		if (devReg != null)
		{
			// get the service
			ServiceMonitorSpec serviceSpec = devReg.getServiceSpec(serviceUUID);

			// if null, create a new one
			if (serviceSpec == null)
			{
				// create the spec
				serviceSpec = new ServiceMonitorSpec(serviceUUID, devReg);

				// add the spec to the device registration
				devReg.addServiceSpec(serviceSpec);
			}

			// get the characteristic monitoring spec
			CharacteristicMonitorSpec charSpec = serviceSpec
					.getCharacteristicSpec(characteristicUUID);

			// if the spec is not null... than don't perform any operation and
			// warn...
			if (charSpec == null)
			{
				// create the spec
				charSpec = new CharacteristicMonitorSpec(characteristicUUID,
						this.pollingTimeMillis, serviceSpec);

				// add the spec to the service
				serviceSpec.addCharacteristicSpec(charSpec);
			}
			else
			{
				// log the warning
				if (this.logger != null)
				{
					this.logger.log(LogService.LOG_WARNING,
							"Attempt to set a characteristic monitorin spec for an already monitored characteristic (UUID:"
									+ characteristicUUID + ")");
				}
			}
		}

	}

	public abstract void newMessageFromHouse(String characteristicUUID,
			String serviceUUID, byte[] value);

	public String getDeviceMacAddress()
	{
		return this.macAddress;
	}
}
