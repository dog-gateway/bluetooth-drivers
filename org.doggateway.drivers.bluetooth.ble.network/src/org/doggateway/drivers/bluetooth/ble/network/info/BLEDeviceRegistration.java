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

import java.util.HashSet;
import java.util.Set;

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
import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.BLENetworkDriverImpl;

import tinyb.BluetoothDevice;

public class BLEDeviceRegistration
{
	// the MAC address of the device to which this device-info object refers, in
	// the XX:XX:...:XX format.
	private String deviceMacAddress;

	// the MAC address of the adapter used to connect to the device, in the
	// XX:XX:...:XX format.
	private String adapterMacAddress;

	// the {@link BLEDriverInstance} generating this device info object
	private BLEDriverInstance bleDriverInstance;

	// the set of service monitoring specifications associated to this device
	// info, identifies the services that the driver requires to be monitored.
	// They may be a subset of the services actually offered by the real device.
	private Set<ServiceMonitorSpec> serviceSpecs;

	// the ManagedBluetoothDevice responding to the needs expressed by this
	// device info object
	private ManagedBluetoothDevice managedDevice;

	/**
	 * Create a new {@link BLEDeviceRegistration} instance given the minimum
	 * required information, i.e., the MAC address of the device to "access",
	 * the MAC address of the adapter to be exploited and the driver originating
	 * the request.
	 * 
	 * @param deviceMacAddress
	 * @param adapterMacAddress
	 * @param bleDriverInstance
	 */
	public BLEDeviceRegistration(String deviceMacAddress,
			String adapterMacAddress, BLEDriverInstance bleDriverInstance)
	{
		// store instance variables
		this.deviceMacAddress = deviceMacAddress;
		this.adapterMacAddress = adapterMacAddress;
		this.bleDriverInstance = bleDriverInstance;

		// build internal data structures
		this.serviceSpecs = new HashSet<ServiceMonitorSpec>();
	}

	/**
	 * Get the MAC address of the device to which this registration object is
	 * referred
	 * 
	 * @return the device MAC address in the XX:XX:...:XX format.
	 */
	public String getDeviceMacAddress()
	{
		return deviceMacAddress;
	}

	/**
	 * Sets the MAC address of the device to which this registration object
	 * refers
	 * 
	 * @param deviceMacAddress
	 *            the device MAC address in the XX:XX:...:XX format. the
	 *            deviceMacAddress to set
	 */
	public void setDeviceMacAddress(String deviceMacAddress)
	{
		this.deviceMacAddress = deviceMacAddress;
	}

	/**
	 * Gets the MAC address of the adapter "preferred" to access the device
	 * referred by this registration object.
	 * 
	 * @return the adapter MAC address in the XX:XX:...:XX format.
	 */
	public String getAdapterMacAddress()
	{
		return adapterMacAddress;
	}

	/**
	 * Sets the MAC address of the adapter that should be preferred for
	 * accessing the device for which this registration is created.
	 * 
	 * @param adapterMacAddress
	 *            the adapter MAC address to set, in the XX:XX:...:XX format
	 */
	public void setAdapterMacAddress(String adapterMacAddress)
	{
		this.adapterMacAddress = adapterMacAddress;
	}

	/**
	 * Get a reference to the {@link BLEDriverInstance} generating this
	 * registration
	 * 
	 * @return the bleDriverInstance The BLEDriverInstance generating this
	 *         registration.
	 */
	public BLEDriverInstance getBleDriverInstance()
	{
		return bleDriverInstance;
	}

	/**
	 * Sets the {@link BLEDriverInstance} that generated this device
	 * registration
	 * 
	 * @param bleDriverInstance
	 *            the bleDriverInstance "generating" this registration object
	 */
	public void setBleDriverInstance(BLEDriverInstance bleDriverInstance)
	{
		this.bleDriverInstance = bleDriverInstance;
	}

	/**
	 * Get the service monitoring specifications defined in this device
	 * registration object. Each service monitoring spec identifies which
	 * characteristics shall be monitored and with which frequency.
	 * 
	 * @return the serviceSpecs The service monitoring specifications defined by
	 *         this registration object, i.e., the monitoring needs of the
	 *         originating driver instance.
	 */
	public Set<ServiceMonitorSpec> getServiceSpecs()
	{
		return serviceSpecs;
	}

	/**
	 * Sets the service monitoring specifications defined in this device
	 * registration object. Each service monitoring spec identifies which
	 * characteristics shall be monitored and with which frequency.
	 * 
	 * @param serviceSpecs
	 *            The service monitoring specifications defined by this
	 *            registration object, i.e., the monitoring needs of the
	 *            originating driver instance.
	 */
	public void setServiceSpecs(Set<ServiceMonitorSpec> serviceSpecs)
	{
		this.serviceSpecs = serviceSpecs;
	}

	/**
	 * Add a single service monitoring specification to this device registration
	 * object
	 * 
	 * @param spec
	 *            The service monitoring specification to add to this
	 *            registration object.
	 */
	public void addServiceSpec(ServiceMonitorSpec spec)
	{
		this.serviceSpecs.add(spec);
	}

	/**
	 * Removes the given service monitoring specification from this device
	 * registration
	 * 
	 * @param spec
	 *            the service monitoring specification to remove.
	 */
	public boolean removeServiceSpec(ServiceMonitorSpec spec)
	{
		return this.serviceSpecs.remove(spec);
	}
	
	/**
	 * Retrieves the {@link ServiceMonitorSpec} associated to the given service UUID, if any available
	 * @param serviceUUID The UUID of the service for which the monitoring spec shall be retrieved
	 * @return The service monitoring spec, if exists, null otherwise
	 */
	public ServiceMonitorSpec getServiceSpec(String serviceUUID)
	{
		ServiceMonitorSpec spec = null;
		
		//search for the given UUID
		for(ServiceMonitorSpec cSpec : this.serviceSpecs)
		{
			//check UUID
			if(cSpec.getServiceUUID().equals(serviceUUID))
			{
				//store the specification to return
				spec = cSpec;
				
				//break the cycle for time efficiency
				break;
			}
		}
		
		return spec;
	}

	/**
	 * Get a reference to the driver-level device representation matching this
	 * device registration object
	 * 
	 * @return the managedDevice The driver-level managed device (wrapping the
	 *         actual {@link BluetoothDevice} to which this registration refers.
	 */
	public ManagedBluetoothDevice getManagedDevice()
	{
		return managedDevice;
	}

	/**
	 * Sets the reference to the driver-level device representation matching
	 * this device registration object. This method is typically called by the
	 * {@link BLENetworkDriverImpl} when accepting this device registration
	 * object.
	 * 
	 * @param managedDevice
	 *            the managedDevice to set
	 */
	public void setManagedDevice(ManagedBluetoothDevice managedDevice)
	{
		this.managedDevice = managedDevice;
	}

}