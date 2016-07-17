package com.github.jinsen47.pokefaker.app.event;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Jinsen on 16/7/13.
 */
public class MapPickEvent {
    public LatLng latLng;

    public MapPickEvent(LatLng latLng) {
        this.latLng = latLng;
    }
}
