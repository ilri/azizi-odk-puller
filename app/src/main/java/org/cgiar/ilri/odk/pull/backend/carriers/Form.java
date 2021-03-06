/*
 Note that this application has been custom made by and for use by the ILRI Azizi Biorepository team and does not follow any standards at all ;). Server side code used by this application is in another repository. You can however reverse engineer this app or simply contact me if you feel like you have to use this App. (C) 2015 Jason Rogena <j.rogena@cgiar.org>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cgiar.ilri.odk.pull.backend.carriers;

import android.os.Environment;
import android.util.Log;

import java.util.Date;

/**
 * Created by Jason Rogena j.rogena@cgiar.org on 09th July 2014.
 * This is just a carrier class for form data
 */
public class Form {

    private static final String TAG = "ODKPuller.Form";

    public static final String PULL_DONT = "dnt";
    public static final String PULL_5_MIN = "5_min";
    public static final String PULL_15_MIN = "15_min";
    public static final String PULL_30_MIN = "30_min";
    public static final String PULL_1_HR = "1_hr";
    public static final String PULL_24_HR = "24_hr";

    public static final String BASE_ODK_LOCATION = Environment.getExternalStorageDirectory() + "/odk/forms/";
    public static final String EXTERNAL_ITEM_SET_SUFFIX = "-media";
    public static final String DEFAULT_CSV_FILE_NAME = "itemsets";
    public static final String SUFFIX_CSV = ".csv";
    public static final String SUFFIX_IMPORTED = ".imported";
    public static final String SUFFIX_JOURNAL = "-journal";
    public static final String SUFFIX_DB = ".db";
    public static final String DB_DATA_TABLE = "externalData";
    public static final String DB_DATA_COLUMN_PREFIX = "c_";
    public static final String DB_DATA_SORT_FIELD = "c_sortby";
    public static final String SUFFIX_INDEX_FIELD = "_key";
    public static final String DB_DATA_COLUMN_TYPE = "text collate nocase";
    public static final String DB_DATA_SORT_COLUMN_TYPE = "real";
    public static final String DB_METADATA_TABLE = "android_metadata";
    public static final String DB_META_LOCALE_FIELD = "local";
    public static final String DB_META_LOCALE_FIELD_TYPE = "text";
    public static final String SUFFIX_INDEX = "_idx";
    public static final String DB_DEFAULT_LOCALE = "en_GB";

    private String name;
    private String pullFrequency;
    private boolean pullWhenInternetOn;
    private boolean pullWhenODKLaunched;
    private long lastPull;

    public Form(String name, String pullFrequency, boolean pullWhenInternetOn, boolean pullWhenODKLaunched){
        this.name = name;
        this.pullFrequency = pullFrequency;
        this.pullWhenInternetOn = pullWhenInternetOn;
        this.pullWhenODKLaunched = pullWhenODKLaunched;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPullFrequency() {
        return pullFrequency;
    }

    public void setPullFrequency(String pullFrequency) {
        this.pullFrequency = pullFrequency;
    }

    public boolean isPullWhenInternetOn() {
        return pullWhenInternetOn;
    }

    public void setPullWhenInternetOn(boolean pullWhenInternetOn) {
        this.pullWhenInternetOn = pullWhenInternetOn;
    }

    public boolean isPullWhenODKLaunched() {
        return pullWhenODKLaunched;
    }

    public void setPullWhenODKLaunched(boolean pullWhenODKLaunched) {
        this.pullWhenODKLaunched = pullWhenODKLaunched;
    }

    public void setLastPull(String lastPull){
        this.lastPull = Long.parseLong(lastPull);
    }

    public void setLastPull(long lastPull){
        this.lastPull = lastPull;
    }

    public long getLastPull(){
        return this.lastPull;
    }

    /**
     * This method checks whether this form is due for an external data update
     *
     * @return True if the form is due for the update
     */
    public boolean isTimeForPull(){
        long currTime = new Date().getTime();

        long timeDiff = currTime - lastPull;

        Log.d(TAG, "Time difference between last update and current time is "+String.valueOf(timeDiff/1000)+" seconds");

        long maxTimePull = 60000;//number of milliseconds in a minute

        Log.d(TAG, "Pull frequency is "+pullFrequency);
        if(pullFrequency.equals(PULL_5_MIN)) {
            maxTimePull = maxTimePull * 5;
            Log.d(TAG, "MaxTimePull =  "+String.valueOf(maxTimePull/1000)+" seconds");
        }
        else if(pullFrequency.equals(PULL_15_MIN)) {
            maxTimePull = maxTimePull * 15;
            Log.d(TAG, "MaxTimePull =  "+String.valueOf(maxTimePull/1000)+" seconds");
        }
        else if(pullFrequency.equals(PULL_30_MIN)) {
            maxTimePull = maxTimePull * 30;
            Log.d(TAG, "MaxTimePull =  "+String.valueOf(maxTimePull/1000)+" seconds");
        }
        else if(pullFrequency.equals(PULL_1_HR)) {
            maxTimePull = maxTimePull * 60;
            Log.d(TAG, "MaxTimePull =  "+String.valueOf(maxTimePull/1000)+" seconds");
        }
        else if(pullFrequency.equals(PULL_24_HR)) {
            maxTimePull = maxTimePull * 60 * 24;
            Log.d(TAG, "MaxTimePull =  "+String.valueOf(maxTimePull/1000)+" seconds");
        }
        else if(pullFrequency.equals(PULL_DONT)){
            Log.i(TAG, "Not supposed to pull this form");
            return false;
        }
        else {
            Log.w(TAG, "Unable to determine the pull frequency.");
            return false;
        }

        if(maxTimePull < timeDiff) return true;
        else return false;
    }
}
