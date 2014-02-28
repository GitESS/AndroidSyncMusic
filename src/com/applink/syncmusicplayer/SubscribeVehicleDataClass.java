package com.applink.syncmusicplayer;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.rpc.GetVehicleData;
import com.ford.syncV4.proxy.rpc.SubscribeVehicleData;
import com.ford.syncV4.proxy.rpc.UnsubscribeVehicleData;

import android.content.Context;

public class SubscribeVehicleDataClass {
	
	private Context mContext = null;
	private int miType = 0;

	SubscribeVehicleDataClass(Context ctx, int type) {
		mContext = ctx;
		miType = type;
	}

	public static SubscribeVehicleDataClass getInstance(Context ctx, int type) {
		return new SubscribeVehicleDataClass(ctx, type);
	}
	
	public void getVehicleData(){
		if (miType == 0) {
			try {
				SubscribeVehicleData msg = new SubscribeVehicleData();
				msg.setCorrelationID(ProxyService.getInstance().nextCorrID());
				msg.setSpeed(true);
				msg.setRpm(true);
				msg.setFuelLevel(true);
				msg.setFuelLevel_State(true);
				msg.setInstantFuelConsumption(true);
				msg.setExternalTemperature(true);
				// msg.setVIN(checkedVehicleDataTypes[6]);
				msg.setPrndl(true);
				msg.setTirePressure(true);
				msg.setOdometer(true);
				msg.setBeltStatus(true);
				msg.setBodyInformation(true);
				msg.setDeviceStatus(true);
				msg.setDriverBraking(true);

				ProxyService.getProxyInstance().sendRPCRequest(msg);
			} catch (SyncException e) {
			}

		}

		if (miType == 1) {
			try {
				UnsubscribeVehicleData msg = new UnsubscribeVehicleData();
				msg.setCorrelationID(ProxyService.getInstance().nextCorrID());
				msg.setSpeed(true);
				msg.setRpm(true);
				msg.setFuelLevel(true);
				msg.setFuelLevel_State(true);
				msg.setInstantFuelConsumption(true);
				msg.setExternalTemperature(true);
				// msg.setVIN(checkedVehicleDataTypes[6]);
				msg.setPrndl(true);
				msg.setTirePressure(true);
				msg.setOdometer(true);
				msg.setBeltStatus(true);
				msg.setBodyInformation(true);
				msg.setDeviceStatus(true);
				msg.setDriverBraking(true);
				
				ProxyService.getProxyInstance().sendRPCRequest(msg);
			} catch (SyncException e) {
			}

		}

		if (miType == 2) {
			try {
				GetVehicleData msg = new GetVehicleData();
				msg.setCorrelationID(ProxyService.getInstance().nextCorrID());
				msg.setSpeed(false);
				msg.setRpm(false);
				msg.setFuelLevel(false);
				msg.setFuelLevel_State(false);
				msg.setInstantFuelConsumption(false);
				msg.setExternalTemperature(false);
				// msg.setVIN(checkedVehicleDataTypes[6]);
				msg.setPrndl(false);
				msg.setTirePressure(false);
				msg.setOdometer(false);
				msg.setBeltStatus(false);
				msg.setBodyInformation(false);
				msg.setDeviceStatus(false);
				msg.setDriverBraking(false);
				

				ProxyService.getProxyInstance().sendRPCRequest(msg);
			} catch (SyncException e) {
			}

		}
	}

}
