package iu.pervasive.autiapp.fragment;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import iu.pervasive.autiapp.R;
import ivb.com.materialstepper.stepperFragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class LocationFragment extends stepperFragment {

    private Button getLocationButton;
    private EditText editText;
    public LocationFragment() {
        // Required empty public constructor
    }

    @Override
    public boolean onNextButtonHandler() {
        return true;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_location, container, false);
        getLocationButton = (Button) v.findViewById(R.id.location_button);
        editText = (EditText) v.findViewById(R.id.location);
        Resources res = getResources(); //resource handle
        Drawable drawable = res.getDrawable(R.drawable.location);
        v.setBackground(drawable);

        getLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                1);
                    }else{
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                1);
                    }
                }else{
                    LocationManager locationManager=(LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    try {
                        editText.setText(getAddress(location.getLatitude(),location.getLongitude()));
                    }catch (Exception e){
                        Toast.makeText(getActivity(),"Exception  "+e,Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        return v;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(getActivity(),
                            Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        try {
                            editText.setText(getAddress(location.getLatitude(), location.getLongitude()));
                        } catch (Exception e) {
                            Toast.makeText(getActivity(), "Exception  " + e, Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(getActivity(), "Not permission  ", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public String getAddress(double lats, double longs) {

        Geocoder geocoder;
        double lat = lats;
        double lon = longs;
        geocoder = new Geocoder(getActivity(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(lat, lon, 1);
        } catch (IOException e) {

            e.printStackTrace();
        }

        if (addresses != null) {

            String address = addresses.get(0).getAddressLine(0);
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName();

            return address;
        } else {
            return "failed";
        }

    }

}
