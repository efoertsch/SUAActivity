# SUAActivity
Test app to show bugs with GeoJsonLayer and markers

As of 4/9/2019

The app will display a map that incorporates: 
  OnMapLongClickListener to display infowindow
  Markers with OnMarkerClickListener to display infowindow
  GeoJsonLayer with onFeatureClickListener

A. On initial startup where markers are NOT initially displayed:
  1. Clicking on GeoJson features or a long click outside a GeoJson feature work ok.
  2. Removing the GeoJson layer and click on random points does not fire the OnFeatureClickListener.
  3. MapLongClicks on the map display the infowindow OK.

B.  When markers are displayed and the GeoJson layer is removed (and onFeatureClickListener is set to null) long clicks on random points on the map may still trigger (not every time) a featureClickListener which gives:

  Process: com.fisincorporated.suaactivity, PID: 24038
    java.lang.NullPointerException: Attempt to invoke interface method 'void com.google.maps.android.data.Layer$OnFeatureClickListener.onFeatureClick(com.google.maps.android.data.Feature)' on a null object reference
        at com.google.maps.android.data.Layer$2.onMarkerClick(Layer.java:114)
        at com.google.android.gms.maps.zzb.zza(Unknown Source:7)
        at com.google.android.gms.maps.internal.zzas.dispatchTransaction(Unknown Source:11)
        at com.google.android.gms.internal.maps.zzb.onTransact(Unknown Source:22)
        at android.os.Binder.transact(Binder.java:627)
        at fg.a(:com.google.android.gms.dynamite_mapsdynamite@13280052@13.2.80 (040700-211705629):10)
        at com.google.android.gms.maps.internal.bi.a(:com.google.android.gms.dynamite_mapsdynamite@13280052@13.2.80 (040700-211705629):5)
        at com.google.maps.api.android.lib6.impl.da.b(:com.google.android.gms.dynamite_mapsdynamite@13280052@13.2.80 (040700-211705629):25)
        at com.google.maps.api.android.lib6.gmm6.api.e.c(:com.google.android.gms.dynamite_mapsdynamite@13280052@13.2.80 (040700-211705629):97)
        at com.google.maps.api.android.lib6.gmm6.vector.m.a(:com.google.android.gms.dynamite_mapsdynamite@13280052@13.2.80 (040700-211705629):61)
        at com.google.maps.api.android.lib6.gmm6.vector.af.e(:com.google.android.gms.dynamite_mapsdynamite@13280052@13.2.80 (040700-211705629):189)
        at com.google.maps.api.android.lib6.gmm6.vector.cs.onSingleTapConfirmed(:com.google.android.gms.dynamite_mapsdynamite@13280052@13.2.80 (040700-211705629):134)
        at com.google.maps.api.android.lib6.impl.gesture.h.onSingleTapConfirmed(:com.google.android.gms.dynamite_mapsdynamite@13280052@13.2.80 (040700-211705629):25)
        at com.google.maps.api.android.lib6.impl.gesture.d.handleMessage(:com.google.android.gms.dynamite_mapsdynamite@13280052@13.2.80 (040700-211705629):15)
        at android.os.Handler.dispatchMessage(Handler.java:106)
        at android.os.Looper.loop(Looper.java:164)
        at android.app.ActivityThread.main(ActivityThread.java:6494)
        at java.lang.reflect.Method.invoke(Native Method)
        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:438)
        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:807)


C. If OnFeatureClickListener is not set to null prior to removing GeoJsonLayer from map:

1. Clicking on random points on the map that are not Markers and not over a GeoJson feature may result (not every time you click) in the onFeaturerClickListener being fired and the getInfoWindow(Marker marker) method being executed (The onMapLongClick is not fired however).
2. Removing the GeoJsonLayer and clicking on points on the map  (not on the markers) can again cause the onFeatureClickListener and getInfoWindow(Marker marker) to be called.

