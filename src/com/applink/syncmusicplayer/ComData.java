package com.applink.syncmusicplayer;

import java.util.ArrayList;
import java.util.Vector;

public class ComData {
	
	public static ComData mComData;
	public int mAutoCorrId = 101;
	public boolean mbSaveWave;
	/*public boolean[] isVehicleDataSubscribed = new boolean[VehicleDataType
			.values().length];
	public ArrayList<RecordMessage> mListRecordMessage = new ArrayList<RecordMessage>();
	public static Vector<SoftButton> mListSoftButton = new Vector<SoftButton>();
	*/
	public static ComData getInstance() {
		if (mComData == null) {
			mComData = new ComData();
		}

		return mComData;
	}
	
	public String[] vehicleDataTypeNames() {
		final String[] vehicleDataTypeNames = new String[] { "Speed",
				"Engine RPM", "fuel Level", "fuel Level State",
				"instant Fuel consumption", "External Temp", "VIN", "Prndl",
				"Tire Pressure", "Odometer", "BeltStatus", "Body Information",
				"Device Status", "Driver Braking" };

		return vehicleDataTypeNames;
	}
}
