package com.socgame.campuswars_app.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.socgame.campuswars_app.R;
import com.socgame.campuswars_app.Sensor.GpsObserver;
import com.socgame.campuswars_app.communication.BackendCom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class MapsFragment extends Fragment implements GpsObserver {
    /**
     * Displays the world map
     * Uses custom design
     * Displays current position
     * Displays lecture halls
     *
     * written by Jonas
     * Calls by Daniel
     */
    private LatLng position = new LatLng(48.2650, 11.6716);//Using campus as default/fallback position;
    private GoogleMap map;
    private Marker localPos;
    BackendCom bCom;

    private List<Marker> lectureHalls = new LinkedList<Marker>();

    private OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;

            //Make it viusally fit our UI style
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.mapsstyle_json));
            googleMap.setBuildingsEnabled(true);

            updatePositionMarker();

            bCom.roomDetectionGet(roomfinderGetListener(), httpErrorListener());

            /*
            //NICE TO HAVE
            //DRAW BORDERS
            //https://stackoverflow.com/questions/45803711/how-to-draw-a-polygon-like-google-map-in-android-app
            /*
            Polygon polygon = googleMap.addPolygon(new PolygonOptions()
                    .add
                    (
                        new LatLng(position.latitude+(Math.random()*2-1)*range/3, position.longitude+(Math.random()*2-1)*range),
                        new LatLng(position.latitude+(Math.random()*2-1)*range/3, position.longitude+(Math.random()*2-1)*range),
                        new LatLng(position.latitude+(Math.random()*2-1)*range/3, position.longitude+(Math.random()*2-1)*range)
                    )
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.CYAN));
            */
        }
    };


    //Adding proper comments, cause Daniel will have to call this

    /**
     * Add a marker of a lecture hall to the map.
     * Wrapper of identical method which uses the LatLng class for gps pos
     *
     * @param lat     latitude
     * @param lon     longitude
     * @param color   marker color as hex example: "#4275A8"
     * @param name    name of lecture hall
     * @param lecture current lecture title, alternatively any sub-headline
     * @return reference of the Marker. Call Marker.remove() to delete lecture hall
     */
    public Marker addLectureHall(double lat, double lon, String color, String name, String lecture) {
        return addLectureHall(new LatLng(lat, lon), color, name, lecture);
    }

    /**
     * Add a marker of a lecture hall to the map
     *
     * @param pos     gps location
     * @param color   marker color as hex example: "#4275A8"
     * @param name    name of lecture hall
     * @param lecture current lecture title, alternatively any sub-headline
     * @return reference of the Marker. Call Marker.remove() to delete lecture hall
     */
    public Marker addLectureHall(LatLng pos, String color, String name, String lecture)
    {

        MarkerOptions options = customMarker(pos, name, lecture, color, R.drawable.ic_tower_solid, 2);//defaultMarker(pos, name, lecture, color);
        Marker marker = map.addMarker(options);

        lectureHalls.add(marker);//safe reference to marker, so i can delete it later

        return marker;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        this.bCom = BackendCom.getInstance(this.getContext());


        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);

            mapFragment.setMenuVisibility(false);
            mapFragment.setHasOptionsMenu(false);

            register(getActivity());//Also creates instance of gps listener if needed and auto updates itself
        }

    }

    @Override
    public void OnLocationUpdate(LatLng loc) {
        if (loc == null) {
            Log.e("GPS", this + " recieved null location");
        }

        position = loc;

        Log.d("GPS", this + " location updated to " + loc.latitude + ", " + loc.longitude);


        //NOTE: Ideally this should be called at a constant frequency, instead of on GPS change.
        // While this does lead to problems in the emulator, on the target device, it works perfectly
        updateLectureHalls();

        updatePositionMarker();
    }

    private void updatePositionMarker() {
        if (map != null) {
            //Dont always move camera
            Location locCam = latLngToLocation(map.getCameraPosition().target);
            Location loc = latLngToLocation(position);
            float distance = loc.distanceTo(locCam);

            if (distance < 400 || distance > 10000 || localPos == null) {
                //create smooth camera move
                CameraPosition camPos = new CameraPosition.Builder()
                        .target(position)//move camera
                        .zoom(17.0f)//Zoom in
                        .tilt(20.0f)//slight tilt
                        .build();

                //apply
                CameraUpdate applyCam = CameraUpdateFactory.newCameraPosition(camPos);
                map.animateCamera(applyCam);
            }

            //remove outdated marker
            if (localPos != null)
                localPos.remove();

            MarkerOptions options = customMarker(position, "You", "You are here", "#000000", R.drawable.ic_human, 2);
            localPos = map.addMarker(options);
        }
    }

    private Response.Listener<JSONArray> roomfinderGetListener() {
        return new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    clearAllHalls();
                    for (int i = 0; i < response.length(); i++) {
                        //Getting JSONs
                        JSONObject lectureHall = response.getJSONObject(i);
                        JSONObject location = lectureHall.getJSONObject("location");
                        JSONObject occupier = lectureHall.getJSONObject("occupier");

                        //Getting Data
                        double lat = location.getDouble("latitude");
                        double lon = location.getDouble("longitude");

                        String lecture = lectureHall.getString("currentLecture");
                        if(lecture.contains("null")){
                            lecture = "No Lecture";
                        }

                        String name = lectureHall.getString("roomName");

                        String color = occupier.getString("color");

                        //Adding Lecture Hall
                        addLectureHall(lat, lon, color, name, lecture);
                    }
                } catch (JSONException e) {
                    Log.d("roomFinderGetListener: ", e.toString());
                }
            }
        };
    }


    private Response.ErrorListener httpErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Error Handling
                Log.d("HTTP", "Error: " + error.getMessage());
            }
        };
    }

    //Marker (color) logic

    private void clearAllHalls() {
        for (Marker h : lectureHalls) {
            h.remove();
        }
    }

    //NOTE: Ideally this should be called at a constant frequency, instead of on GPS change.
    // While this does lead to problems in the emulator, on the target device, it works perfectly
    private void updateLectureHalls()
    {
        //clearing halls now in callback
        bCom.roomDetectionGet(roomfinderGetListener(), httpErrorListener());
    }

    //NOTE: Does not check for garbage input data (bad parsing from http call)
    private MarkerOptions customMarker(LatLng pos, String title, String desc, String color, int drawable, float scale)
    {
        //Load icon
        BitmapDrawable bitmapDrawable = (BitmapDrawable) getResources().getDrawable(drawable).mutate();
        Bitmap bitmap = bitmapDrawable.getBitmap();

        //Rescale custom marker icon
        //https://stackoverflow.com/questions/14851641/change-marker-size-in-google-maps-api-v2
        bitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * scale), (int)(bitmap.getHeight() * scale), true);

        //Recolor icon
        try //Check color viability
        {
            bitmap = tintBitmap(bitmap, hexToColor(color));
        }
        catch (Exception e)
        {
            Log.e("GPS", "Could not parse color " + color);
            bitmap = tintBitmap(bitmap, Color.BLUE);
        }


        BitmapDescriptor personIcon = BitmapDescriptorFactory.fromBitmap(bitmap);

        //Add custom marker
        MarkerOptions options = new MarkerOptions()
                .position(pos)
                .title(title)
                .icon(personIcon)
                .snippet(desc);

        return options;
    }

    private MarkerOptions defaultMarker(LatLng pos, String title, String desc, String color)
    {
        BitmapDescriptor icon = null;
        try //Check color viability
        {
            icon = BitmapDescriptorFactory.defaultMarker(hexToHue(color));
        }
        catch (Exception e)
        {
            Log.e("GPS", "Could not parse color " + color);
            icon = BitmapDescriptorFactory.defaultMarker(Color.BLACK);
        }


        return new MarkerOptions()
                .position(pos)
                .title(title)
                .icon(icon)
                .snippet(desc);
    }


    //Thanks StackOverflow
    //https://stackoverflow.com/questions/19076124/android-map-marker-color
    private static float hexToHue(String hexColor)
    {
        float[] hsv = new float[3];
        Color.colorToHSV(Color.parseColor(hexColor), hsv);
        return hsv[0];//BitmapDescriptorFactory.defaultMarker(hsv[0]);
    }

    private static int hexToColor(String hexColor)
    {
        return Color.parseColor(hexColor);
    }

    //https://stackoverflow.com/questions/6331906/how-to-tint-a-bitmap-to-a-solid-color
    private static Bitmap tintBitmap(Bitmap bitmap, int color)
    {
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        Bitmap bitmapResult = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapResult);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return bitmapResult;
    }

    //https://stackoverflow.com/questions/31099140/how-to-convert-lat-lng-to-a-location-variable
    Location latLngToLocation(LatLng loc)
    {
        Location temp = new Location(LocationManager.GPS_PROVIDER);
        temp.setLatitude(loc.latitude);
        temp.setLongitude(loc.longitude);
        return  temp;
    }
}
