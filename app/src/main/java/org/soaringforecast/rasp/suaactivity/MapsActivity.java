package org.soaringforecast.rasp.suaactivity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.RawRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.fisincorporated.suaactivity.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.kml.KmlLayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.soaringforecast.rasp.sua.json.Geometry;
import org.soaringforecast.rasp.sua.json.SUA;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapLongClickListener
        , GoogleMap.OnInfoWindowClickListener, GoogleMap.OnInfoWindowCloseListener {

    private GoogleMap googleMap;

    //Default for NewEngland - try to get something displayed quickly rather than blank screen
    private LatLngBounds mapLatLngBounds = new LatLngBounds(new LatLng(41.2665329, -73.6473083)
            , new LatLng(45.0120811, -70.5046997));
    private GeoJsonLayer geoJsonLayer = null;
    private KmlLayer kmlLayer = null;
    private Soundings soundings;
    private Button displaySuaButton;
    private List<Marker> soundingMarkers = new ArrayList<>();
    private Button displaySoundingsButton;

    private boolean displaySUA = true;
    private boolean displaySoundings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        getSoundingLocations();
        mapFragment.getMapAsync(this);
        displaySuaButton = findViewById(R.id.sua_toogle_button);
        displaySuaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displaySUA = !displaySUA;
                toogleSuaDisplay(displaySUA);

            }
        });

        displaySoundingsButton = findViewById(R.id.soundings_toogle_button);
        displaySoundingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displaySoundings = !displaySoundings;
                toggleSoundingMarkers(displaySoundings);
            }
        });
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setLatLngBoundsForCameraTarget(mapLatLngBounds);
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnMapLongClickListener(this);
        googleMap.setOnInfoWindowClickListener(this);
        googleMap.setOnInfoWindowCloseListener(this);
        googleMap.setInfoWindowAdapter(new MarkerInfoWindowAdapter());
        googleMap.setOnMapLoadedCallback(() -> googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mapLatLngBounds, 0)));
        displayForecast();
        toogleSuaDisplay(true);
        toggleSoundingMarkers(false);
    }

    private void displayForecast() {
        GroundOverlayOptions forecastOverlayOptions = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.wstar_bsratio_1400local_d2_body)))
                .positionFromBounds(mapLatLngBounds);
        forecastOverlayOptions.transparency(.4f);
        googleMap.addGroundOverlay(forecastOverlayOptions);
    }

    private void toogleSuaDisplay(boolean displaySUA) {
        displaySuaButton.setText(displaySUA ? "Remove SUA" : "Show SUA");
        if (displaySUA) {
            addGeoJsonLayerToMap(R.raw.sterlng7_sua_geojson, "NewEngland");
            // addKmlLayerToMap(R.raw.sterlng7_kml, "NewEngland");
            //readSuaToJsonClasses(R.raw.sterlng7_sua_geojson);
            //createEmptyGeoJsonLayer();
            //addGeoJsonLayerFromRaw(getApplicationContext(), R.raw.sterlng7_sua_geojson);

        } else {
            removeSuaFromMap();
        }
    }

    private void addGeoJsonLayerFromRaw(Context context, @RawRes int rawResId) {
        JSONObject jsonObject = readSuaToJsonObject(context, rawResId);
        if (jsonObject != null) {
            geoJsonLayer = new GeoJsonLayer(googleMap, jsonObject);
            geoJsonLayer.addLayerToMap();
            geoJsonLayer.setOnFeatureClickListener(new GeoJsonLayer.GeoJsonOnFeatureClickListener() {
                @Override
                public void onFeatureClick(Feature feature) {
                    displaySuaDetails(feature);
                }
            });
        }
    }

    // Simple GeoJson checking?
    private JSONObject readSuaToJsonObject(Context context, @RawRes int rawResId) {

        JSONObject jsonObject;
        JSONObject feature;
        JSONArray coordinatesArray;
        JSONArray startCoordinates;
        JSONArray endCoordinates;

        jsonObject = JSONResourceReader.getJsonObject(context, rawResId);

        if (jsonObject != null) {
            JSONArray featuresArray = jsonObject.optJSONArray("features");
            if (featuresArray != null) {
                for (int i = 0; i < featuresArray.length(); ++i) {
                    try {
                        feature = featuresArray.getJSONObject(i);
                         coordinatesArray = feature.getJSONObject("geometry").getJSONArray("coordinates").getJSONArray(0);
                          startCoordinates = coordinatesArray.getJSONArray(0);
                          endCoordinates = coordinatesArray.getJSONArray(coordinatesArray.length() - 1);
                    if (!(startCoordinates.getString(0).equals(endCoordinates.getString(0))
                            || startCoordinates.getString(1).equals(endCoordinates.getString(1)))) {
                        coordinatesArray.put(startCoordinates);
                        Timber.d("updated coordinates for feature:" + feature.getJSONObject("properties").get("TITLE"));
                    }
                    } catch (Exception e) {
                        Timber.e(e, "Some Exception in geojson parsing");
                    }

                }

            }
        }
        return jsonObject;
    }

    private void createEmptyGeoJsonLayer() {
        geoJsonLayer = new GeoJsonLayer(googleMap, new JSONObject());
    }


    // GeoJson features
    private void addGeoJsonLayerToMap(int geoJsonid, String suaRegionName) {
        try {
            geoJsonLayer = new GeoJsonLayer(googleMap, geoJsonid, this);
            // TODO reimplement after Google fixes bugs
            // Bug when clicking on map, may not get correct feature, also still getting click event
            // after layer removed from map
            geoJsonLayer.addLayerToMap();
            geoJsonLayer.setOnFeatureClickListener(new GeoJsonLayer.GeoJsonOnFeatureClickListener() {
                @Override
                public void onFeatureClick(Feature feature) {
                    displaySuaDetails(feature);
                }
            });
        } catch (IOException e) {
            postError(e, suaRegionName);
        } catch (JSONException e) {
            postError(e, suaRegionName);
        }

    }

    private GeoJsonLayer.GeoJsonOnFeatureClickListener geoJsonOnFeatureClickListener = new GeoJsonLayer.GeoJsonOnFeatureClickListener() {
        @Override
        public void onFeatureClick(Feature feature) {
            displaySuaDetails(feature);
        }
    };
    //feature -> {
    //displaySuaDetails(feature);
    // };

    private void displaySuaDetails(Feature feature) {
        ArrayList<String> suaProperties = new ArrayList<>();
        Iterator it = feature.getProperties().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            suaProperties.add(getString(R.string.sua_property, pair.getKey(), pair.getValue()));
        }
        if (suaProperties.size() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = getLayoutInflater();
            View suaPropertiesView = inflater.inflate(R.layout.sua_properties_list, null);
            builder.setView(suaPropertiesView);
            ListView suaPropertiesListView = suaPropertiesView.findViewById(R.id.sua_properties_listview);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            arrayAdapter.addAll(suaProperties);
            suaPropertiesListView.setAdapter(arrayAdapter);
            builder.setTitle("SUA");
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            alertDialog.setCanceledOnTouchOutside(true);

        }
    }

    // Simply going geoJsonLayer.removeLayerFromMap still leaves GeoJsonOnFeatureClickListener active

    public void removeSuaFromMap() {
        if (geoJsonLayer != null) {
            // click listener continues to function even when layer removed from map, so set flag (for now) to
            // indicate to ignore clicks
            //geoJsonLayer.setOnFeatureClickListener(null);
            geoJsonLayer.removeLayerFromMap();
            geoJsonLayer = null;
        }
        if (kmlLayer != null) {
            kmlLayer.removeLayerFromMap();
            kmlLayer = null;
        }
    }

    /**
     * Current KmlLayer does not pick up SUA properties
     *
     * @param kmlId
     * @param suaRegionName
     */
    private void addKmlLayerToMap(@IdRes int kmlId, String suaRegionName) {
        try {
            kmlLayer = new KmlLayer(googleMap, R.raw.sterlng7_kml, getApplicationContext());
            kmlLayer.addLayerToMap();
            kmlLayer.setOnFeatureClickListener(new KmlLayer.OnFeatureClickListener() {
                @Override
                public void onFeatureClick(Feature feature) {
                    Log.i("KmlClick", "Feature clicked: " + (feature != null ? feature.getId() : " Null feature"));
                    displaySuaDetails(feature);
                }
            });
        } catch (XmlPullParserException e) {
            postError(e, suaRegionName);
        } catch (IOException e) {
            postError(e, suaRegionName);
        }
    }

    //----------- Markers
    private void getSoundingLocations() {
        soundings = (new JSONResourceReader(getResources(), R.raw.soundings)).constructUsingGson(Soundings.class);
    }

    private void toggleSoundingMarkers(boolean displaySoundings) {
        displaySoundingsButton.setText(displaySoundings ? "Remove Soundings" : "Show Soundings");
        displaySoundingMarkers(displaySoundings);
    }

    private void displaySoundingMarkers(boolean display) {
        if (display) {
            showSoundingMarkers();
        } else if (soundingMarkers != null && soundingMarkers.size() > 0) {
            for (Marker marker : soundingMarkers) {
                marker.remove();
            }
        }
    }

    private void showSoundingMarkers() {
        soundingMarkers.clear();
        LatLng latLng;
        Marker marker;
        for (Sounding sounding : soundings.getSoundings()) {
            latLng = new LatLng(sounding.getLatitude(), sounding.getLongitude());
            marker = googleMap.addMarker(new MarkerOptions().position(latLng)
                    .title(sounding.getLocation()));
            soundingMarkers.add(marker);
            marker.setTag(sounding);
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Timber.d("Displaying point forecast for lat: %1$f  long: %2$f"
                , latLng.latitude, latLng.longitude);
        new PointForecast(latLng, "cloudy");
        if (googleMap != null) {
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.fromBitmap(
                            getBitmapFromVectorDrawable(this, R.drawable.transparent_marker)))); // transparent image
            marker.setTag(new PointForecast(latLng, "cloudy"));
            marker.showInfoWindow();
        }

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }


    //-----Infowindow for longclick and marker------------------------------------------------------------------------------
    @Override
    public void onInfoWindowClick(Marker marker) {
        marker.hideInfoWindow();
    }

    @Override
    public void onInfoWindowClose(Marker marker) {
        if (marker.getTag() instanceof PointForecast) {
            marker.remove();
        }
    }

    class MarkerInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private final View markerInfoWindowView;
        private final TextView markerInfoWindowInfo;

        MarkerInfoWindowAdapter() {
            markerInfoWindowView = ((LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.turnpoint_infowindow, null);
            markerInfoWindowInfo = markerInfoWindowView.findViewById(R.id.turnpoint_infowindow_info);
            markerInfoWindowInfo.setMovementMethod(new ScrollingMovementMethod());
        }

        public View getInfoWindow(Marker marker) {
            render(marker, markerInfoWindowInfo);
            return markerInfoWindowView;
        }

        public View getInfoContents(Marker marker) {
            return null;
        }

        private void render(Marker marker, TextView view) {
            if (marker.getTag() instanceof PointForecast) {
                view.setText(((PointForecast) marker.getTag()).getForecastText());
            }
        }
    }

    //--------------------------------------------------------------------
    private static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }


    private void postError(Exception e, String suaRegionName) {
        // TODO report exception
        e.printStackTrace();
    }




    // --- Custom processing of geojson file
    // 1. Check that ending lat/lng matches starting lat long
    // 2. Set color for types of SUA
    private void readSuaToJsonClasses(int jsonResId) {
        SUA sua = getSua(jsonResId);
        checkStartingEndingCorrdinates(sua);
        convertBackToJsonObject(sua);
    }

    private void convertBackToJsonObject(SUA sua) {

    }

    private SUA getSua(int jsonResId) {
        InputStream raw = getResources().openRawResource(jsonResId);
        Reader rd = new BufferedReader(new InputStreamReader(raw));
        Gson gson = new Gson();
        return gson.fromJson(rd, SUA.class);
    }

    /**
     * Geojson requirement is that ending coordinate = starting coordinate
     *
     * @param sua
     */
    private void checkStartingEndingCorrdinates(SUA sua) {
        Geometry geometry;
        List<List<List<Double>>> coordinates;
        int numberCoordinates;
        List<Double> startCoordinates;
        List<Double> endCoordinates;
        for (org.soaringforecast.rasp.sua.json.Feature feature : sua.getFeatures()) {
            geometry = feature.getGeometry();
            if (geometry != null && geometry.getCoordinates() != null && geometry.getCoordinates().size() > 0) {
                coordinates = geometry.getCoordinates();
                if (coordinates != null && coordinates.get(0).size() > 0) {
                    numberCoordinates = coordinates.get(0).size();
                    startCoordinates = coordinates.get(0).get(0);
                    endCoordinates = coordinates.get(0).get(numberCoordinates - 1);
                    if (startCoordinates != null && startCoordinates.size() == 2
                            && endCoordinates != null && endCoordinates.size() == 2) {
                        if (!(startCoordinates.get(0) == endCoordinates.get(0))
                                || !(startCoordinates.get(1) == endCoordinates.get(1))) {
                            coordinates.get(0).add(startCoordinates);
                            Timber.d("updated coordinates for feature:" + feature.getProperties().getTITLE());
                        }
                    }
                }

            }
        }
    }

}

