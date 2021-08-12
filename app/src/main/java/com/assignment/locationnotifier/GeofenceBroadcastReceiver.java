package com.assignment.locationnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationHelper notificationHelper = new NotificationHelper(context);

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if(geofencingEvent.hasError()){
            Log.d(TAG, "onReceive: Error receiving geofence event");
            return;
        }

        List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
//        Location location = geofencingEvent.getTriggeringLocation();

        for (Geofence geofence:geofenceList) {
            Log.e(TAG, "onReceive: "+geofence.getRequestId());
        }
        int transitionType = geofencingEvent.getGeofenceTransition();

        if(transitionType==Geofence.GEOFENCE_TRANSITION_ENTER){
            notificationHelper.sendHighPriorityNotification("Geofence Notifier","User entered in geofence",MapsActivity.class);
            Toast.makeText(context,"User entered in geofence",Toast.LENGTH_SHORT).show();
        }
        else if(transitionType== Geofence.GEOFENCE_TRANSITION_EXIT){
            notificationHelper.sendHighPriorityNotification("Geofence Notifier","User exited from geofence",MapsActivity.class);
            Toast.makeText(context,"User exited from geofence",Toast.LENGTH_SHORT).show();
        }

    }
}