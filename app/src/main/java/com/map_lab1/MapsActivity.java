package com.map_lab1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.map_lab1.databinding.ActivityMapsBinding;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final int POLYGON_SIDES = 3;
    private static final int REQUEST_CODE = 1;
    private static final String TAG = "MainActivity";
    private static final long UPDATE_INTERVAL = 5000;
    private static final long FASTEST_INTERVAL = 3000;
    boolean tap = false;
    int mName = 0;
    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FusedLocationProviderClient mClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private final List<Marker> markerList = new ArrayList<>();
    private Polygon shape;
    private Marker userMarker, favMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mClient = LocationServices.getFusedLocationProviderClient(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (!isGrantedlocationPermission()) {
            requestLocationPermission();
        } else {
            startUpdatingLocation();
        }

        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(@NonNull Polyline polyline) {

                float[] results = new float[1];
                //  polyline.setClickable(!tap);
                Location.distanceBetween(polyline.getPoints().get(0).latitude,
                        polyline.getPoints().get(0).longitude,
                        polyline.getPoints().get(1).latitude,
                        polyline.getPoints().get(1).longitude, results);

                popupMessage("Distance line  ", results[0] + "");

            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(@NonNull Marker marker) {
                popupInput(marker);
                return false;
            }
        });


        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
            @Override
            public void onPolygonClick(@NonNull Polygon polygon) {

                // tap = true;
                float[] results = new float[1];
                // polygon.setClickable(tap);
                //tap = !tap;
                Location.distanceBetween(polygon.getPoints().get(0).latitude,
                        polygon.getPoints().get(0).longitude,
                        polygon.getPoints().get(1).latitude,
                        polygon.getPoints().get(1).longitude, results);
                Location.distanceBetween(polygon.getPoints().get(1).latitude,
                        polygon.getPoints().get(1).longitude,
                        polygon.getPoints().get(2).latitude,
                        polygon.getPoints().get(2).longitude, results);
                Location.distanceBetween(polygon.getPoints().get(2).latitude,
                        polygon.getPoints().get(2).longitude,
                        polygon.getPoints().get(3).latitude,
                        polygon.getPoints().get(3).longitude, results);
                Location.distanceBetween(polygon.getPoints().get(3).latitude,
                        polygon.getPoints().get(3).longitude,
                        polygon.getPoints().get(0).latitude,
                        polygon.getPoints().get(0).longitude, results);

                popupMessage(results[0] / 1000 + " Km", "Perimeter of  Quadrilateral ");
            }

        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(@NonNull Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {

                int i = -1;
                for (Marker marker1 : markerList) {
                    i++;
                    if (marker.getTitle().equals(marker1.getTitle())) {
                        marker.remove();
                        Toast.makeText(MapsActivity.this, marker.getTitle(), Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (i != -1 )
                {
                    markerList.remove(i);
                    shape.remove();
                    drawShape();
                }



            }

            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {


            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {


                    setMarker(latLng);


            }


        });
    }

    public void popupInput(Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Marker's Title : ' " + marker.getTitle() + " '   , if you wish to change proceed ");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                marker.setTitle(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    public void popupMessage(String Msg, String title) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(Msg);
        // alertDialogBuilder.setIcon(R.drawable.ic_no_internet);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setNegativeButton("ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.d("internet", "Ok btn pressed");
                // add these two lines, if you wish to close the app:
                // finishAffinity();

                // System.exit(0);
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void setMarker(LatLng latLng) {

        String ch = "A";
        switch (mName) {
            case 0:
                ch = "A";
                break;
            case 1:
                ch = "B";
                break;
            case 2:
                ch = "C";
                break;
            case 3:
                ch = "D";
                break;
            default:
                ch = "O";
                break;
        }

        MarkerOptions options = new MarkerOptions()
                .position(latLng).title(ch)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .snippet("nice place");
        options.draggable(true);
        favMarker = mMap.addMarker(options);
        favMarker.setTag(mName);

        mName++;

        if (markerList.size() >= POLYGON_SIDES) {
//            for (Marker marker : markerList) {
//                marker.remove();
//            }
           // markerList.clear();
            shape.remove();
           // mName = 0;
        }
        markerList.add(favMarker);

            drawShape();



    }

    private void drawShape() {

        PolygonOptions options = new PolygonOptions().
                strokeColor(Color.RED)
                .fillColor(0x3500ff00)
                .clickable(true)
                .strokeWidth(10);

        for (Marker marker : markerList) {
            options.add(marker.getPosition());
        }
        shape = mMap.addPolygon(options);

    }

    @SuppressLint("MissingPermission")
    private void startUpdatingLocation() {

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location location = locationResult.getLastLocation();
                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

//            userMarker = mMap.addMarker(new MarkerOptions().position(userLocation).title("Vir is Here").snippet("your are here"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
            }
        };

        mClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

    }

    private void requestLocationPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);

    }

    private boolean isGrantedlocationPermission() {

        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (REQUEST_CODE == requestCode) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this).setMessage("Accessing th locaion is mandatory ").
                        setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
                            }
                        }).setNegativeButton("Cancel", null).create().show();
            } else {
                startUpdatingLocation();
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            Dialog errorDialog = GoogleApiAvailability.getInstance().
                    getErrorDialog
                            (this, REQUEST_CODE, REQUEST_CODE,
                                    new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialogInterface) {

                                            Toast.makeText(MapsActivity.this,
                                                    "The Service is not available",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });

            assert errorDialog != null;
            errorDialog.show();
        }
    }

}