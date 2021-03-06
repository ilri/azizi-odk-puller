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

package org.cgiar.ilri.odk.pull.backend.services;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.cgiar.ilri.odk.pull.R;
import org.cgiar.ilri.odk.pull.SettingsActivity;
import org.cgiar.ilri.odk.pull.backend.DataHandler;
import org.cgiar.ilri.odk.pull.backend.carriers.Form;

/**
 * Created by Jason Rogena j.rogena@cgiar.org on 10th July 2014.
 * This service is meant to run every minute to determine which external data needs to be downloaded
 * The service should be launced using AlarmManager and should not be an infinitely running service.
 * Refer to:
 *      - http://stackoverflow.com/questions/2566350/how-to-always-run-a-service-in-the-background
 *      - http://www.androidguys.com/2010/03/29/code-pollution-background-control/
 */
public class PersistentService extends IntentService {

    private static final String TAG = "ODKPuller.PersistentService";

    public static final long RESTART_INTERVAL = 30000l;//30 seconds
    private static int NOTIFICATION_ID = 9232;
    private static String ODK_COLLECT_PACKAGE_NAME = "org.odk.collect.android";

    /**
     * Default constructor. Make sure this is there or less android will be unable to call the service
     */
    public PersistentService() {
        super(TAG);
    }

    /**
     * Another constructor.
     * In case you want to call this service by another name e.g. if you create a class that is a sub-class
     * of this class
     *
     * @param name
     */
    public PersistentService(String name) {
        super(name);
    }

    /**
     * This method is called whenever the service is called. Note that the service might have already been running when
     * it was called
     *
     * @param intent The intent with which the service was called
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG,"************************************************");
        /*NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.determining_data_for_pulling));

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());*/
        //register all unregistered forms
        registerNewPullForms();
        //get all forms that are due for updating
        List<Form> dueForms = getAllDueForms();

        for(int index = 0; index < dueForms.size(); index++){
            startPullService(dueForms.get(index));
        }

        /*try {
            //Thread.sleep(10000l);
            Thread.sleep(2000l);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        mNotificationManager.cancel(NOTIFICATION_ID);*/
        Log.d(TAG, "************************************************");
    }

    /**
     * This method gets all forms from the SQLite database that are due for fetching external data for.
     * Forms include:
     *      - Those that need to be refreshed after every X minutes and these X minutes have expired
     *      - Those whose external item sets are to be fetched whenever ODK Collect is launched
     *
     * @return A list of forms whose external item sets are to be fetched
     */
    private List<Form> getAllDueForms(){
        List<Form> allForms = DataHandler.getAllForms(this);

        List<Form> dueForms = new ArrayList<Form>();

        //check if the top most activity is ODK Collect
        ActivityManager activityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        String topMostActivityPackage = activityManager.getRunningTasks(1).get(0).topActivity.getPackageName();

        boolean odkTop = false;
        if(topMostActivityPackage.equals(ODK_COLLECT_PACKAGE_NAME)){
            //check if ODK Collect was already launched the last time this service ran
            String isODKAlreadyLaunched = DataHandler.getSharedPreference(this, DataHandler.PREF_ODK_ALREADY_ON, DataHandler.ODK_NOT_LAUCHED);

            if(isODKAlreadyLaunched.equals(DataHandler.ODK_NOT_LAUCHED)){//means that this service has not seen odk at the top before
                DataHandler.setSharedPreference(this, DataHandler.PREF_ODK_ALREADY_ON, DataHandler.ODK_ALREADY_LAUCHED);
                odkTop = true;
                Log.i(TAG, "ODK Collect now the current activity");
            }
        }
        else{
            Log.d(TAG, "Current activity is "+topMostActivityPackage);
            DataHandler.setSharedPreference(this, DataHandler.PREF_ODK_ALREADY_ON, DataHandler.ODK_NOT_LAUCHED);
        }

        /*
        For all the fetched forms check if the need updates based on:
            - them needing an update based on the staleness of the data based on the last time the data was updated
            - them needing an update because ODK Collect has been launched
          */
        for(int index = 0; index < allForms.size(); index++){
            Log.d(TAG, "Current form is: "+ allForms.get(index).getName());
            if(allForms.get(index).isTimeForPull() || (odkTop && allForms.get(index).isPullWhenODKLaunched())){
                dueForms.add(allForms.get(index));
                Log.i(TAG, allForms.get(index).getName() + " needs data pulled like now");
            }
            else{
                Log.i(TAG, allForms.get(index).getName() + " does not need data pulled right now");
            }
        }

        return dueForms;
    }

    /**
     * This method gets ODK forms that are on the device that have corresponding pull data on the server
     */
    private void registerNewPullForms() {
        List<String> serverForms = DataHandler.getAllFormsOnServer(this);
        List<Form> registeredForms = DataHandler.getAllForms(this);
        List<String> allODKFormNames = DataHandler.getAllODKForms();
        for(int i = 0; i < allODKFormNames.size(); i++){
            String currFormName = allODKFormNames.get(i);
            boolean found = false;
            for(int j = 0; j < registeredForms.size(); j++){
                if(currFormName.equals(registeredForms.get(j).getName())){
                    found = true;
                    break;//no need to continue with loop
                }
            }
            if(found == false) {//current form is not registered with ODK pull
                //check if current form has pull data on the server
                for(int j = 0; j < serverForms.size(); j++){
                    if(currFormName.equals(serverForms.get(j))){
                        //form has form data on the server but is not registered
                        Form currUnregisteredForm = new Form(currFormName, Form.PULL_5_MIN, true, true);
                        currUnregisteredForm.setLastPull(new Date().getTime() - (60000*6));//make form updated last more than 6 mins ago
                        DataHandler.saveFormPreferences(this, currUnregisteredForm);
                        updateNotification(currFormName, this.getString(R.string.added_to_odk_pull));
                    }
                }
            }
        }
    }

    /**
     * This method updates/creates the notification for this service.
     * Note that the notification generated here is a compact notification
     *
     * @param title The title for the notification
     * @param details The details of the notification
     */
    private void updateNotification(String title, String details){
        Intent intent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(details)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(details))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    /**
     * This method pulls an external item set for the given form
     *
     * @param form The form for which you want to get external item sets
     */
    public void startPullService(Form form){
        Intent intent = new Intent(PersistentService.this, FetchFormDataService.class);
        intent.putExtra(FetchFormDataService.KEY_FORM_NAME, form.getName());

        startService(intent);
    }
}
