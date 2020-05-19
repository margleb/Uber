package com.project.uber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;

import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback, RoutingListener {

    String driverId;

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;

    private Button mLogout, mSettings, mRideStatus, mHistory;

    private Switch mWorkingSwitch;
    private boolean switch_status;

    private int status = 0;

    private String customerId = "", destionation;
    private LatLng destionationLatLng;
    private float rideDistance;

    private Boolean isLoggingOut = false;

    private LinearLayout mCustomerInfo;
    private ImageView mCostomerProifleImage;
    private TextView mCostumerName, mCustomerPhone, mCustomerDestination;
    private SupportMapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        // прорисовка маршрута движения автомобиля до клента
        polylines = new ArrayList<>();

        // Провайдер, обеспечивающий отобржаение расположения на карте
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Добавляет менеджер поддержки фрагментов, для загрузки карты
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mCustomerInfo = (LinearLayout) findViewById(R.id.customerInfo);
        mCostomerProifleImage = (ImageView) findViewById(R.id.customerProfileImage);
        mCostumerName = (TextView) findViewById(R.id.customerName);
        mCustomerPhone = (TextView) findViewById(R.id.customerPhone);
        mCustomerDestination = (TextView) findViewById(R.id.customerDestination);

        mWorkingSwitch = (Switch) findViewById(R.id.workingSwitch);
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    switch_status = true;
                    connectDriver();
                } else {
                    switch_status = false;
                    disconnectDriver();
                }
            }
        });

        mLogout = (Button) findViewById(R.id.logout);
        mSettings = (Button) findViewById(R.id.settings);
        mRideStatus = (Button) findViewById(R.id.rideStatus);
        mHistory = (Button) findViewById(R.id.history);

        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(status) {
                    case 1:  // едим на пункт назначения
                        status = 2;
                        erasePolyLines(); // стираем маршут
                        if(destionationLatLng.latitude!=0.0 && destionationLatLng.longitude!=0.0) {
                            getRouteToMarker(destionationLatLng);
                        }
                        mRideStatus.setText("Отправляемя на пункт назначения");
                        break;
                    case 2:  // завершить поездку
                        recordRide();
                        endRide();
                        break;
                }
            }
        });

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoggingOut = true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapActivity.this, DriverSettingsActivity.class);
                startActivity(intent);
            }
        });
        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverMapActivity.this, HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Drivers");
                startActivity(intent);
            }
        });

        getAssignedCustomer();
    }

    private void getAssignedCustomer() {
        DatabaseReference assignCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        assignCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    status = 1;
                    Map<String, Object> map = new HashMap<>();
                    map.put("customerRideId", dataSnapshot.getValue());
                    if (map.get("customerRideId") != null) {
                        customerId = Objects.requireNonNull(map.get("customerRideId")).toString();
                        getAssignedCustomerPickupLocation(); // место назначение клента
                        getAssignedCustomerPickupInfo(); // информация о кленте
                        getAssignedCustomerDestination(); // направление до клента
                    }
                } else {
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerDestination() {
        DatabaseReference assignCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        assignCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    Map<String, Object> customerRequestMap = new HashMap<>();
                    for(DataSnapshot item: dataSnapshot.getChildren()) {
                        customerRequestMap.put(item.getKey(), item.getValue());
                    }

                    if(customerRequestMap.get("destination")!=null) {
                        destionation = Objects.requireNonNull(customerRequestMap.get("destination")).toString();
                        mCustomerDestination.setText("Пункт назначения : "  + destionation);
                    }
                    else {
                        mCustomerDestination.setText("Пункт назначения : не указан");
                    }

                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if(customerRequestMap.get("destinationLat") != null) {
                        destinationLat = Double.valueOf(Objects.requireNonNull(customerRequestMap.get("destinationLat")).toString());
                    }
                    if(customerRequestMap.get("destinationLng") != null) {
                        destinationLng = Double.valueOf(Objects.requireNonNull(customerRequestMap.get("destinationLng")).toString());
                        destionationLatLng = new LatLng(destinationLat, destinationLng);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DriverMapActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT);
            }
        });
    }

    private void getAssignedCustomerPickupInfo() {
        mCustomerInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {

                    Map<String, Object> customerInfoMap = new HashMap<>();
                    for(DataSnapshot item: dataSnapshot.getChildren()) {
                        customerInfoMap.put(item.getKey(), item.getValue());
                    }

                    if (customerInfoMap.get("name") != null) {
                        mCostumerName.setText(customerInfoMap.get("name").toString());
                    }
                    if (customerInfoMap.get("phone") != null) {
                        mCustomerPhone.setText(customerInfoMap.get("phone").toString());
                    }
                    if (customerInfoMap.get("profileImageUrl") != null) {
                        // библиотека, кеширует url изображения и помещает его область изображений
                        Glide.with(getApplication()).load(customerInfoMap.get("profileImageUrl").toString()).into(mCostomerProifleImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DriverMapActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    Marker pickupMarker;
    private DatabaseReference assignCustomerPickupLocation;
    private ValueEventListener assignedCustomerPickupLocationRefListener;
    private LatLng pickupLatLng;
    private void getAssignedCustomerPickupLocation() {
        assignCustomerPickupLocation = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l");
        assignedCustomerPickupLocationRefListener = assignCustomerPickupLocation.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerId.equals("")) {

                    List<Object> map = new ArrayList<>();
                    map.add(dataSnapshot.getValue());

                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    pickupLatLng = new LatLng(locationLat, locationLng);
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("pickup location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_pickup)));
                    getRouteToMarker(pickupLatLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DriverMapActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getRouteToMarker(LatLng pickupLatLng) {
        Routing routing = new Routing.Builder()
                .key(getString(R.string.api_key))
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)
                .build();
        routing.execute();
    }


    private void recordRide() {

        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef =  FirebaseDatabase.getInstance().getReference().child("history");

        String requestId = historyRef.push().getKey();
        driverRef.child(requestId).setValue(true);
        customerRef.child(requestId).setValue(true);

        Map map = new HashMap<>();
        map.put("driver", driverId);
        map.put("customer", customerId);
        map.put("rating", 0);
        map.put("timestamp", System.currentTimeMillis()/1000);
        map.put("destination", destionation);
        map.put("location/from/lat", pickupLatLng.latitude);
        map.put("location/from/lng", pickupLatLng.longitude);
        map.put("location/to/lat", destionationLatLng.latitude);
        map.put("location/to/lng", destionationLatLng.longitude);
        map.put("distance", rideDistance);
        historyRef.child(requestId).updateChildren(map);

    }

    private void endRide() {

        // при нажатии кнопки отмена вызова uber
        mRideStatus.setText("Подобрать клиента");

        erasePolyLines(); // стираем маршрут

        // устанавливаем у элемента значение true, тем самым удаляя дочерние
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        driverRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire getFire = new GeoFire(ref);
        getFire.removeLocation(customerId);
        customerId = "";
        rideDistance = 0;
        if(pickupMarker != null) {
            pickupMarker.remove();
        }
        if (assignedCustomerPickupLocationRefListener != null) {
            assignCustomerPickupLocation.removeEventListener(assignedCustomerPickupLocationRefListener);
        }
        mCustomerInfo.setVisibility(View.GONE);
        mCostumerName.setText("");
        mCustomerPhone.setText("");
        mCustomerDestination.setText("Destination: --");
        mCostomerProifleImage.setImageResource(R.mipmap.ic_default_user);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        checkLocationPermission();
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            for(Location location : locationResult.getLocations()) {
                if (switch_status) {

                    mLastLocation = location;

                    if(!customerId.equals("")) {
                        // расстояние до клента
                        rideDistance += mLastLocation.distanceTo(location)/1000;
                    }

                    // перемещает камеру в текущую точку расположения
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
                    mMap.animateCamera(cameraUpdate);

                    // переключаем в зависимости от занятости и доступности водителя
                    DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
                    DatabaseReference refWoriking = FirebaseDatabase.getInstance().getReference("driversWorking");
                    GeoFire geoFireAvailable = new GeoFire(refAvailable);
                    GeoFire geoFireWorking = new GeoFire(refWoriking);
                    switch (customerId) {
                        case "":
                            geoFireWorking.removeLocation(driverId);
                            geoFireAvailable.setLocation(driverId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;
                        default:
                            geoFireAvailable.removeLocation(driverId);
                            geoFireWorking.setLocation(driverId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                            break;
                    }
                }
            }
        }
    };

    private void checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Доступ к гео данным")
                        .setMessage("Предоставьте доступ к вашему местоположению").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    }
                }).create().show();
            } else {
                // ActivityCompat.requestPermissions( CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Пожалуйста, предоставьте доступ к геоданным", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    private void connectDriver() {
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    private void disconnectDriver() {
        if(mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        // получаем список доступных авто
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable");
        GeoFire geoFire = new GeoFire(ref);

        // при остановке приложения удалем id пользователя из базы данных
        geoFire.removeLocation(driverId);
    }


    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Что то произошло, попробуйте снова", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onRoutingStart() {

    }

    // маршурт и цвет
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shorestRouteIndex) {

        polylines = new ArrayList<>();

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }


        // добавляем маршрут на карту
        for (int i = 0; i < route.size(); i++) {

            // если больше 5 маршуртов на карте
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);
            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": дистанция - " + route.get(i).getDistanceValue() + ": протяженность - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }

    }

    private void erasePolyLines() {
        for(Polyline line: polylines) {
            line.remove();
        }
        polylines.clear();
    }

    @Override
    public void onRoutingCancelled () {

    }
}
