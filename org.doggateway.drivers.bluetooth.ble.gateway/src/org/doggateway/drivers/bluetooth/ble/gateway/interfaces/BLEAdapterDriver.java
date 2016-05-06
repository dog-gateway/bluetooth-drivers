/**
 * 
 */
package org.doggateway.drivers.bluetooth.ble.gateway.interfaces;

/**
 * @author bonino
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
