package org.soaringforecast.rasp.suaactivity;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Soundings {

    @SerializedName("soundings")
    @Expose
    private List<Sounding> soundings = null;

    public List<Sounding> getSoundings() {
        return soundings;
    }

    public void setSoundings(List<Sounding> soundings) {
        this.soundings = soundings;
    }

}