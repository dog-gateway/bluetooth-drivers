/**
 * 
 */
package org.doggateway.drivers.bluetooth.ble.gateway;

import org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance;
import org.doggateway.drivers.bluetooth.ble.network.info.BLEDeviceRegistration;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLEDiscoveryListener;
import org.doggateway.drivers.bluetooth.ble.network.interfaces.BLENetwork;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.BluetoothAdapter;
import it.polito.elite.dog.core.library.model.state.DiscoveryState;
import it.polito.elite.dog.core.library.model.state.State;
import it.polito.elite.dog.core.library.model.statevalue.ActiveDiscoveryStateValue;
import it.polito.elite.dog.core.library.model.statevalue.IdleStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;

/**
 * @author bonino
 *
 */
public class BLEAdapterDriverInstance extends BLEDriverInstance
		implements BluetoothAdapter, BLEDiscoveryListener
{

	public BLEAdapterDriverInstance(BLENetwork network,
			ControllableDevice device, LogHelper logger)
	{
		// call the super class constructor stating that the device is actually
		// an adapter, i.e., not a real Bluetooth device. Therefore null is
		// passed as gw address as well as no polling time
		super(network, device, null, -1, logger);

		// create the device state
		this.currentState = new DeviceStatus(device.getDeviceId());

		// initialize the state
		this.initializeStates();

		// add this driver instance as discovery listener
		network.addDiscoveryListener(this);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.polito.elite.dog.core.library.model.StatefulDevice#getState()
	 */
	@Override
	public DeviceStatus getState()
	{
		return this.currentState;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance#
	 * specificConfiguration()
	 */
	@Override
	protected void specificConfiguration()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance#
	 * addToNetworkDriver(org.doggateway.drivers.bluetooth.ble.network.info.
	 * BLEDeviceRegistration)
	 */
	@Override
	protected void addToNetworkDriver(BLEDeviceRegistration bleDevReg)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.doggateway.drivers.bluetooth.ble.network.BLEDriverInstance#
	 * newMessageFromHouse(java.lang.String, java.lang.String, byte[])
	 */
	@Override
	public void newMessageFromHouse(String characteristicUUID,
			String serviceUUID, byte[] value)
	{
		// Nothing to do as the adapter is not a real device.
	}

	@Override
	public void discoveredDevice(String devName, String devAddress, short rssi,
			boolean managed)
	{
		// notify the discovery if not already managed
		if (!managed)
			this.notifyDiscoveredDevice(devName, rssi, devAddress);

	}

	@Override
	public void discoveryEnabled(boolean enabled)
	{
		if (enabled)
		{
			// set the adapter status at discovery active
			if (this.changeState(
					new DiscoveryState(new ActiveDiscoveryStateValue())))
				this.notifyActivatedDiscovery();
		}
		else
		{
			// set the adapter status at discovery idle
			if (this.changeState(new DiscoveryState(new IdleStateValue())))
				this.notifyDeactivatedDiscovery();
		}
	}

	@Override
	public void stopDiscovery()
	{
		// explicit discovery stop
		// TODO: define how to handle manual vs automatic discovery, at now it
		// does nothing
		this.network.stopDiscovery();

	}

	@Override
	public void startDiscovery()
	{
		// explicitly discovery start
		this.network.startDiscovery();

	}

	@Override
	public void notifyActivatedDiscovery()
	{
		((BluetoothAdapter) this).notifyActivatedDiscovery();
	}

	@Override
	public void notifyDeactivatedDiscovery()
	{
		((BluetoothAdapter) this).notifyDeactivatedDiscovery();
	}

	@Override
	public void notifyDiscoveredDevice(String discoveredDeviceName,
			int discoveredDeviceRSSI, String discoveredDeviceMac)
	{
		// log
		/*this.logger.log(LogService.LOG_INFO,
				"Discovered " + discoveredDeviceName + "MAC "
						+ discoveredDeviceMac + " RSSI:"
						+ discoveredDeviceRSSI);*/

		// send the notification
		((BluetoothAdapter) this).notifyDiscoveredDevice(discoveredDeviceName,
				discoveredDeviceRSSI, discoveredDeviceMac);

	}

	@Override
	public void updateStatus()
	{
		((BluetoothAdapter) this).updateStatus();
	}

	private boolean changeState(State newState)
	{
		// the state changed flag
		boolean stateChanged = false;

		// get the current state
		String currentStateValue = "";
		State state = currentState
				.getState(DiscoveryState.class.getSimpleName());

		if (state != null)
			currentStateValue = (String) state.getCurrentStateValue()[0]
					.getValue();

		// check that the state has changed
		if (!currentStateValue
				.equals(newState.getCurrentStateValue()[0].getValue()))
		{
			// update the current state
			this.currentState.setState(DiscoveryState.class.getSimpleName(),
					newState);

			// debug
			logger.log(LogService.LOG_DEBUG,
					"Device " + device.getDeviceId() + " is now "
							+ (newState).getCurrentStateValue()[0].getValue());

			// update the status
			this.updateStatus();

			// updated the state changed flag
			stateChanged = true;
		}

		return stateChanged;
	}

	private void initializeStates()
	{
		// initialize the state
		this.currentState.setState(DiscoveryState.class.getSimpleName(),
				new DiscoveryState(new IdleStateValue()));

	}

}
