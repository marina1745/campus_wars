package com.socgame.campuswars_app.Sensor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.model.LatLng;
/*
    Legacy GPS code
    Used nowhere
*/
public class GpsLocationManager
{
    /*


    THIS IS LEGACY CODE!!!!!
    DO NOT USE THIS!!!!!!!!

    ONLY KEPT FOR REFERENCE

     */

    /*
    //Do I have the permission to get the location?
    private static boolean permitted(Context context)
    {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    //Get current postion
    //https://stackoverflow.com/questions/2227292/how-to-get-latitude-and-longitude-of-the-mobile-device-in-android

    //make it possible to call this from outside an acitvity (withoutthe activity and context parameter)
    //for some reason this works in the emulator, but crashes on my real phone
    public static LatLng getPosition(Activity activity, Context context)
    {
         LocationManager lm = (LocationManager) activity.getSystemService(context.LOCATION_SERVICE);

        if(permitted(context))
        {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            activity.requestPermissions
            (
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    42069
            );

            //lets see if I still dont have permission
            if(permitted(context))
                //return dummy, if I dont have it
                return new LatLng(48.2650,11.6716);
        }


        try//Bodging this to not crash my phone
        {
            //Actually get the position

            //supressing warning as i DO check for permission
            @SuppressLint("MissingPermission") Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            double longitude = location.getLongitude();
            double latitude = location.getLatitude();

            return new LatLng(latitude, longitude);
        }
        catch (Exception e)
        {
            return new LatLng(48.2650,11.6716);
        }
    }
     */
}
