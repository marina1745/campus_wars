package com.socgame.campuswars_app.Sensor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.model.LatLng;

import java.util.LinkedList;
import java.util.List;


/**
   * Register to this (Singleton) observable to get your location (as Lat Long)
   * To register you must implement the GpsObserver interface
   * Use get Instance, never call a constructor yourself!!!!
   *
   * Updates fairly jittery and often since the raw measurements are bearly filtered
   *
   * written by Jonas
 */
public class GpsListener implements LocationListener
{
    private LocationManager lm;
    private Activity activity;

    //Singleton
    private static GpsListener instance;

    //DO NOT USE THE CONSTRUCTOR!!
    @SuppressLint("MissingPermission")
    private GpsListener(Activity activity)
    {
        if (instance == null)
        {
            instance = this;

            Log.d("GPS", "Initialized GPS Listener");

            lm = (LocationManager) activity.getSystemService(activity.LOCATION_SERVICE);
            this.activity = activity;

            //Permission checks
           if
           (!permission())
           {
               activity.requestPermissions
                       (
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                42069
                       );
           }

            try
            {
                //startlocation
                String provider = LocationManager.GPS_PROVIDER;
                @SuppressLint("MissingPermission") Location lastLoc = lm.getLastKnownLocation(provider);

                //Force location update
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
            }
            catch (Exception e) {Log.e("GPS", "Could not start GPS: " + e);}

            try
            {
                boolean gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean net = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if(!(gps || net))
                    Toast.makeText(activity, "Please enable GPS", Toast.LENGTH_LONG).show();
            }
            catch (Exception e)
            {
                Toast.makeText(activity, "Please check your GPS", Toast.LENGTH_LONG).show();
            }
        }
    }

    //USE THIS TO ACCESS THE CLASS
    //NOTE: I REALLY HATE HAVING TO GIVE AN ACTIVITY HERE! I WANT TO BE ABLE TO GET GPS FROM NORMAL JAVA
    public static GpsListener getInstance(Activity activity)
    {
        if(instance == null)
            instance = new GpsListener(activity);

        return instance;
    }

    //Observer Stuff
    private List<GpsObserver> observers = new LinkedList<GpsObserver>();

    //returns false if called on the wrong object instance
    public boolean register(GpsObserver observer)
    {
        if(this != instance)
            return false;


        //If I am can, try to explicitly get permission
        Activity activity = null;
        Fragment fragment = null;

        try
        {
            activity = (Activity) observer;
        }
        catch (Exception e) {}

        try
        {
            fragment = (Fragment) observer;
        }
        catch (Exception e) {}

        if(activity == null && fragment != null)
            activity = fragment.getActivity();
        if(activity != null)
        {
            activity.requestPermissions
            (
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                42069
            );
        }

        //Register
        observers.add(observer);
        observer.OnLocationUpdate(location);//Initialize

        return true;
    }

    //returns false if called on the wrong object instance
    //it does return true however if the observer wasnt registered anyway
    public boolean unregister(GpsObserver observer)
    {
        if(this != instance)
            return false;

        observers.remove(observer);

        return true;
    }

    //notify all the observers of my new location
    private void update()
    {
        for (GpsObserver observer : observers)
        {
            observer.OnLocationUpdate(location);
        }
    }


    //Actual location logic
    private LatLng location = new LatLng(48.2650,11.6716);//Using campus as default/fallback position


    @Override
    public void onLocationChanged(@NonNull Location location)
    {
        //Important checks done at setup
        //NOTE: smoothing factoring in accuracy would be nice
        LatLng loc = locToLatLng(location);
        this.location = loc;

        Log.d("GPS", "Location changed to " + location.getLatitude() + ", " + location.getLongitude());

        update();
    }

    //NOT SUPPORTED IN THIS ANDROID VERSION
    /*
    @Override
    public void onLocationChanged(@NonNull List<Location> locations)
    {
        double lat = 0;
        double lng = 0;

        for(Location loc : locations)
        {
            //no longer to do factor in accuracy
            lat += loc.getLatitude();
            lng += loc.getLongitude();
        }

        LatLng loc = new LatLng(lat, lng);
        this.location = loc;

        Log.d("GPS", "Location changed to " + lat + ", " + lng);

        update();
    }
     */

    @Override
    public void onProviderDisabled(@NonNull String provider)
    {
        Log.d("GPS", "Could not use provider " + provider);

        Toast.makeText(activity, "Location: " + provider + " disabled. Please turn it on again!", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onProviderEnabled(@NonNull String provider)
    {
        try
        {
            if(permission())
            {
                lm.requestLocationUpdates(provider, 1000, 1, this);

                Toast.makeText(activity, "Location: " + provider + " enabled", Toast.LENGTH_LONG).show();
            }
        }
        catch (Exception e) {}
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        //Needs to be implemented since this deprecated method sometimes gets called anyway
        //Doesnt need to do anything
    }

    private boolean permission()
    {
        return     ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;

    }

    private static LatLng locToLatLng(Location loc)
    {
        return new LatLng(loc.getLatitude(), loc.getLongitude());
    }
}
