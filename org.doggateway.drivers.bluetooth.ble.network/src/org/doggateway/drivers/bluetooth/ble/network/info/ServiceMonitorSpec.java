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

public class ServiceMonitorSpec
{
	// The UUID of the service to which this monitoring specification refers
	private String serviceUUID;

	// The device registration object "owning" this service monitoring
	// specification
	private BLEDeviceRegistration deviceInfo;

	// The set of characteristics to be monitored for this service (ideally can
	// be more than one)
	private Set<CharacteristicMonitorSpec> characteristicSpecs;

	/**
	 * Class constructor, builds an instance of ServiceMonitorSpec referring to
	 * the device service having the given UUID
	 * 
	 * @param serviceUUID
	 *            The service UUID as a String
	 */
	public ServiceMonitorSpec(String serviceUUID)
	{
		// store the instance variables
		this.serviceUUID = serviceUUID;

		// completed initialization
		this.init();
	}

	/**
	 * Class constructor, builds an instance of ServiceMonitorSpec referring to
	 * the device service having the given UUID and being part of the devInfo
	 * device registration.
	 * 
	 * @param serviceUUID
	 *            The UUID of the service to which this monitoring specification
	 *            refers
	 * @param deviceInfo
	 *            The device registration object of which this spec is part
	 */
	public ServiceMonitorSpec(String serviceUUID,
			BLEDeviceRegistration deviceInfo)
	{
		// store the instance variables
		this.serviceUUID = serviceUUID;
		this.deviceInfo = deviceInfo;

		// complete initialization
		this.init();
	}

	/**
	 * Initializes inner datastructures
	 */
	private void init()
	{
		// initialize inner data structures
		this.characteristicSpecs = new HashSet<CharacteristicMonitorSpec>();
	}

	/**
	 * Get the UUID of the Bluetooth Gatt Service to which this monitoring
	 * specification refers
	 * 
	 * @return the serviceUUID The UUID of the service as a String
	 */
	public String getServiceUUID()
	{
		return serviceUUID;
	}

	/**
	 * Sets the UUID of the Bluetooth Gatt Service to which this monitoring
	 * specification must refer
	 * 
	 * @param serviceUUID
	 *            the serviceUUID to which the specification shall refer
	 */
	public void setServiceUUID(String serviceUUID)
	{
		this.serviceUUID = serviceUUID;
	}

	/**
	 * Gets the device registration object to which this service monitoring
	 * specification belongs
	 * 
	 * @return the deviceInfo
	 */
	public BLEDeviceRegistration getDeviceInfo()
	{
		return deviceInfo;
	}

	/**
	 * Sets the device registration object to which this service monitoring
	 * specification belongs
	 * 
	 * @param deviceInfo
	 *            the device registration to set as owner of this monitoring
	 *            specification
	 */
	public void setDeviceInfo(BLEDeviceRegistration deviceInfo)
	{
		this.deviceInfo = deviceInfo;
	}

	/**
	 * Get the set of characteristic monitoring specifications associated to the
	 * service referred by this service-level specification
	 * 
	 * @return the characteristicSpecs
	 */
	public Set<CharacteristicMonitorSpec> getCharacteristicSpecs()
	{
		return characteristicSpecs;
	}

	/**
	 * Sets the characteristic monitoring specifications associated to the
	 * service referred by this service-level specification
	 * 
	 * @param characteristicSpecs
	 *            the characteristicSpecs to set
	 */
	public void setCharacteristicSpecs(
			Set<CharacteristicMonitorSpec> characteristicSpecs)
	{
		this.characteristicSpecs = characteristicSpecs;
	}

	/**
	 * Adds one characteristic monitoring specification to this service
	 * monitoring specification instance
	 * 
	 * @param spec
	 *            The characteristic monitoring specification to add
	 */
	public void addCharacteristicSpec(CharacteristicMonitorSpec spec)
	{
		this.characteristicSpecs.add(spec);
	}

	/**
	 * Removes the given characteristic monitoring specification from this
	 * service monitoring specification instance
	 * 
	 * @param spec
	 *            The characteristic monitoring specification to be removed
	 * @return true if successful, false otherwise (e.g, the characteristic does
	 *         not exists)
	 */
	public boolean removeCharacteristicSpec(CharacteristicMonitorSpec spec)
	{
		return this.characteristicSpecs.remove(spec);
	}

	/**
	 * returns the {@link CharacteristicMonitorSpec} associated to the given
	 * characteristic UUID, if available
	 * 
	 * @param characteristicUUID
	 *            The UUID of the characteristic for which the spec shall be
	 *            retrieved
	 * @return The spec, if exists, or null
	 */
	public CharacteristicMonitorSpec getCharacteristicSpec(
			String characteristicUUID)
	{
		CharacteristicMonitorSpec spec = null;

		for (CharacteristicMonitorSpec cSpec : this.characteristicSpecs)
		{
			if (cSpec.getCharacteristicUUID().equals(characteristicUUID))
			{
				// store the spec
				spec = cSpec;

				// break the cycle for time efficiency
				break;
			}
		}

		return spec;
	}
}