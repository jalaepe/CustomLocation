package com.baicaide.customlocation;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements $Http.Response, OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback
{

    @Override
    public void onResponse(String response)
    {
        SharedPreferences spf = getSharedPreferences("MapData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = spf.edit();
        if(response.length()>0) {
            editor.putString("LastData", response);
            editor.apply();
        }else{
            response = spf.getString("LastData", "");
        }
            try {
                mapdatalist = new ArrayList<>();
                JSONArray ja = new JSONArray(response);
                for (int i = 0; i < ja.length(); i++) {
                    JSONObject jo = ja.getJSONObject(i);
                    MapdataModel mapdata = new MapdataModel();
                    mapdata.id = jo.getInt("id");
                    mapdata.title = jo.getString("title");
                    mapdata.snippet = jo.getString("snippet");
                    mapdata.lat = jo.getDouble("lat");
                    mapdata.lon = jo.getDouble("lon");
                    mapdatalist.add(mapdata);
                }
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(MainActivity.this);
                progressDialog.dismiss();

            } catch (JSONException e) {
               // Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            }
    }

    @Override
    public void onError(String msg)
    {
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();
    }
    List<MapdataModel> mapdatalist;
    private GoogleMap mMap;
    FusedLocationProviderClient mFusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean permissionDenied = false;
    int idx=1;
    boolean online = false;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_layout);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait ....");
        progressDialog.setCancelable(false);
    }
    @Override
    public void onMapReady(GoogleMap p1) {
        mMap = p1;
        enableMyLocation();
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }
            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.infowindow,null, false);
                TextView title = infoWindow.findViewById(R.id.tvTitle);
                title.setText(marker.getTitle());
                TextView snippet = infoWindow.findViewById(R.id.tvSnippet);
                snippet.setText(marker.getSnippet());
                return infoWindow;
            }
        });
      try {
          mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(mapdatalist.get(0).lat, mapdatalist.get(0).lon)));
          for (int i = 0; i < mapdatalist.size(); i++) {
              MarkerOptions mo = new MarkerOptions()
                      .title(mapdatalist.get(i).title)
                      .snippet(mapdatalist.get(i).snippet)
                      .icon(BitmapDescriptorFactory.defaultMarker(
                              BitmapDescriptorFactory.HUE_BLUE))
                      .position(new LatLng(mapdatalist.get(i).lat, mapdatalist.get(i).lon));
              mMap.addMarker(mo);
              // mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
          }
      }catch (Exception e){

      }
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng p1)
            {
                MarkerOptions marker = new MarkerOptions()
                        .position(new LatLng(p1.latitude, p1.longitude))
                        .title("YourNew Location "+idx++ +"\n\n" +"Latitude "+ p1.latitude + "\n\n" + "Longitude " + p1.longitude)
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN));
                mMap.addMarker(marker);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("location","Latitude : "+p1.latitude+"\nLongitude : "+p1.longitude);
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(MainActivity.this,"Location has been copied.",Toast.LENGTH_LONG).show();
                dialogBox("Do you want to add your location?",p1.latitude,p1.longitude);
            }
        });
    }
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (isOnline()) {
            online = true;
            progressDialog.show();
            new $Http($Http.RequesMethod.POST)
                    .url("https://jalaepe.000webhostapp.com/read_data.php")
                    .execute(this);
        }else{
            Toast.makeText(MainActivity.this,"NO INTERNET CONNECTION!",Toast.LENGTH_LONG).show();
            try {
                SharedPreferences spf = getSharedPreferences("MapData", Context.MODE_PRIVATE);
                String lastData = spf.getString("LastData", "");
                onResponse(lastData);
            }catch (Exception e){

            }
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            enableMyLocation();
        } else {
            permissionDenied = true;
        }
    }
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (permissionDenied) {
            showMissingPermissionError();
            permissionDenied = false;
        }
    }
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add("Refresh").setIcon(R.drawable.refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("Map View");
        menu.add("Satellite View");
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.getTitle().equals("Refresh")){
            onResume();
        }
        if(item.getTitle().equals("Map View")){

            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        if(item.getTitle().equals("Satellite View")){

            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        return super.onOptionsItemSelected(item);
    }
    public void dialogBox(String message, double lat,double lon) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setPositiveButton("YES",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        ViewDialog alertDialoge = new ViewDialog();
                        alertDialoge.showDialog(MainActivity.this, lat,lon);
                    }
                });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        arg0.dismiss();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    public class ViewDialog {

        public void showDialog(AppCompatActivity activity, Double lat,Double lon) {
            final Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(false);
            dialog.setContentView(R.layout.custom_dialog);
           final EditText etTitle = dialog.findViewById(R.id.etTitle);
           final EditText etSnippet = dialog.findViewById(R.id.etSnippet);
            Button addButton = (Button) dialog.findViewById(R.id.dialog_add);
            Button cancleButton = (Button) dialog.findViewById(R.id.dialog_cancel);
            addButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    String addtitle = "";
                    String addsnippet = "";
                    if (etTitle.getText().toString().trim().equalsIgnoreCase("")) {
                        etTitle.setError("This field can not be blank");
                    }else {
                        addtitle = etTitle.getText().toString();
                    }
                     if(etSnippet.getText().toString().trim().equalsIgnoreCase("")) {
                        etSnippet.setError("This field can not be blank");
                    }else {
                         addsnippet = etSnippet.getText().toString();
                     }
                    //Perfome Action
                    if(addtitle.length()>0) {
                        try {

                            new $Http($Http.RequesMethod.POST)
                                    .url("https://jalaepe.000webhostapp.com/insert_data.php")
                                    .field("title", addtitle)
                                    .field("snippet", addsnippet)
                                    .field("lat", lat+"")
                                    .field("lon", lon+"")
                                    .execute(MainActivity.this);
                            Toast.makeText(MainActivity.this,"Your Location is added to sever.",Toast.LENGTH_LONG).show();
                        } catch (Exception e) {

                            Toast.makeText(MainActivity.this,"Failed",Toast.LENGTH_LONG).show();
                        }
                    }
                    dialog.dismiss();
                }
            });
            cancleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            dialog.show();

        }
    }
    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }
}
