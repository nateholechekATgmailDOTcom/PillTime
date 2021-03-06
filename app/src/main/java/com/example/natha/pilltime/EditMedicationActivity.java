package com.example.natha.pilltime;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Vector;

/**
 * Created by natha on 11/30/2017.
 */

public class EditMedicationActivity extends Activity {


    String nameBeforeChange;
    Vector<Integer> timeVec = new Vector<>();
    boolean timeInput;
    Vector<String> times;
    ArrayAdapter<String> arrayAdapter;
    Pill currentPill;
    String intentExtraPill;
    private int currentPillTime;
    EditText etName;
    CheckBox cbActive;
    EditText etPillCount;
    EditText etDosage;
    EditText etNotes;
    ListView lvTimes;

    private TimePicker time_picker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_medication_activity);
        Initialize();

        times = new Vector<>();

        try {
            intentExtraPill = getIntent().getStringExtra("pill");
            if (!intentExtraPill.isEmpty()) {
                String[] extraPillInfo = intentExtraPill.split("\n");
                dbHelper db = new dbHelper(this);
                currentPill = db.getPillByName(extraPillInfo[0]);
                Vector<Integer> currentPillTimes = db.getAllTimes(currentPill);
                db.close();
                for (Integer time : currentPillTimes) {
                    times.add(time.toString());
                }
            } else {
                currentPill = new Pill();
            }
        } catch (Exception e) {
        }

        poplateData();
    }

    public void Initialize() {
        etName = (EditText) findViewById(R.id.nameET);
        cbActive = (CheckBox) findViewById(R.id.activeCB);
        etPillCount = (EditText) findViewById(R.id.pillCountET);
        etDosage = (EditText) findViewById(R.id.dosageET);
        etNotes = (EditText) findViewById(R.id.notesET);
        lvTimes = (ListView) findViewById(R.id.timesLV);

    }

    public void insertToDb(View view) {
        if (etName.length() == 0 || etPillCount.length() == 0 || etDosage.length() == 0) {
            String errmsg = "You did not input enough data";
            Toast toast = new Toast(getApplicationContext());
            toast.makeText(getApplicationContext(), errmsg, Toast.LENGTH_LONG).show();
        } else {
            dbHelper db = new dbHelper(this);
            String name = etName.getText().toString();
            int active = 0;
            if (cbActive.isChecked()) {
                active = 1;
            } else {
                active = 0;
            }
            Pill pill = new Pill(
                    0,
                    etName.getText().toString(),
                    active,
                    Integer.parseInt(etPillCount.getText().toString()),
                    etDosage.getText().toString(),
                    etNotes.getText().toString()
            );
            currentPill.getName();
            if (timeInput) {
                pill.setTimeTake(currentPillTime, 0);
            }

            if (!db.checkDB(name)) {
                if (currentPill.getName() == null) {
                    db.addPill(pill);
                } else {
                    pill.setId(currentPill.getId());
                    db.updatePill(pill);
                }
                pill.setId(db.getPillId(pill));
            } else {
                pill.setId(db.getPillId(pill));
                db.updatePill(pill);
            }
            if (timeInput) {
                for (int i : timeVec) {
                    db.addTime(pill, i);
                    setAlarm(pill.getName(), pill.getId(), i, true);
                }
            }
            timeInput = false;
            db.close();
            Intent mainIntent = new Intent(EditMedicationActivity.this, MainActivity.class);
            startActivity(mainIntent);
        }
    }
    public void addNewTime(View view) {
        timeInput = true;
        final Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        /*AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setIcon()*/

        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                currentPillTime = hourOfDay * 100 + minute;
                if (arrayAdapter.getPosition(String.valueOf(currentPillTime)) == -1) {
                    currentPill.setTimeTake(currentPillTime, 0);
                    timeVec.add(currentPillTime);
                    arrayAdapter.add(String.valueOf(currentPillTime));
                    arrayAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getApplicationContext(), "Time is already here", Toast.LENGTH_SHORT).show();
                }
            }
        };

        TimePickerDialog timePickerDialog = new TimePickerDialog(EditMedicationActivity.this, listener, hour, minute, true);
        timePickerDialog.setTitle("Select a Time");
        timePickerDialog.show();
    }

    public void poplateData() {
        if (!intentExtraPill.isEmpty()) {
            etName.setText(currentPill.getName());
            etDosage.setText(currentPill.getDosage());
            etNotes.setText(currentPill.getNotes());
            if (currentPill.getActive() == 1) {
                cbActive.setChecked(true);
            } else {
                cbActive.setChecked(false);
            }
            etPillCount.setText(currentPill.getPillCount().toString());
            arrayAdapter = new ArrayAdapter<String>(this, R.layout.pill_list_item, R.id.pillItemTV, times);
        } else {
            etName.setText("");
            etDosage.setText("");
            etNotes.setText("");
            etPillCount.setText("");
            arrayAdapter = new ArrayAdapter<String>(this, R.layout.pill_list_item, R.id.pillItemTV, times);
        }
        lvTimes.setAdapter(arrayAdapter);

        final dbHelper db = new dbHelper(EditMedicationActivity.this);

        lvTimes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(EditMedicationActivity.this)
                        .setTitle("Edit or Delete a Time")
                        .setMessage("Press edit to edit a time, Delete to delete this time")
                        .setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final Calendar currentTime = Calendar.getInstance();
                                int hour = currentTime.get(Calendar.HOUR_OF_DAY);
                                int minute = currentTime.get(Calendar.MINUTE);
                                TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                        currentPillTime = hourOfDay * 100 + minute;
                                        if (arrayAdapter.getPosition(String.valueOf(currentPillTime)) == -1) {
                                            currentPill.setTimeTake(currentPillTime, 0);
                                            timeVec.add(currentPillTime);
                                            String name = currentPill.getName();
                                            Integer timeVal = Integer.parseInt(arrayAdapter.getItem(position));
                                            if (!(name == null)){
                                                db.removePillTimeByName(name, timeVal);
                                            }
                                            arrayAdapter.remove(arrayAdapter.getItem(position));
                                            timeInput = true;
                                            arrayAdapter.add(String.valueOf(currentPillTime));
                                            arrayAdapter.notifyDataSetChanged();
                                            cancelAlarm(currentPill.getId(), timeVal);
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Time is already here", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                };

                                TimePickerDialog timePickerDialog = new TimePickerDialog(EditMedicationActivity.this, listener, hour, minute, true);
                                timePickerDialog.setTitle("Select a Time");
                                timePickerDialog.show();
                            }
                        })
                        .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                    String name = currentPill.getName();
                                    Integer timeVal = Integer.parseInt(arrayAdapter.getItem(position));
                                    if(!(name == null)){
                                        db.removePillTimeByName(name, timeVal);
                                    }
                                    timeVec.remove(timeVal);
                                    arrayAdapter.remove(arrayAdapter.getItem(position));
                                    arrayAdapter.notifyDataSetChanged();
                                    cancelAlarm(currentPill.getId(), timeVal);
                            }
                        })
                        .setNeutralButton("Close", null).show();
                arrayAdapter.notifyDataSetChanged();
            }
        });
        arrayAdapter.notifyDataSetChanged();
        db.close();
    }

    public void cancel(View view) {
        AlertDialog alertDialogBuilder = new AlertDialog.Builder(this)
                .setTitle("EXIT")
                .setMessage("Cancel and return to previous page?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent mainIntent = new Intent(EditMedicationActivity.this, MainActivity.class);
                        startActivity(mainIntent);
                    }
                })
                .setNegativeButton("no", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }).show();
    }

    public void setAlarm(String pillName, int pillId, int timeToTake, boolean isRepeating) {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent myIntent;
        PendingIntent pendingIntent;

        myIntent = new Intent(EditMedicationActivity.this, AndroidNotificationReciever.class);
        myIntent.putExtra("meds", pillName);
        myIntent.setAction("meds" + "meds");

        int alarmTime = convertTime(timeToTake);

        int inpMinutes = timeToTake % 100;
        int inpHrs = (timeToTake - inpMinutes) / 100;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, inpHrs);
        calendar.set(Calendar.MINUTE, inpMinutes);
        calendar.set(Calendar.SECOND, 0);
        int intentId = pillId + timeToTake;
        pendingIntent = PendingIntent.getBroadcast(this,intentId,myIntent,0);
        if (isRepeating) {
            manager.setRepeating(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),86400000,pendingIntent);
        } else {
            manager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
        }


    }
    public void cancelAlarm(int pillID, int time)
    {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent myIntent = new Intent(getApplicationContext(),
                EditMedicationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,pillID+time, myIntent,0);
        alarmManager.cancel(pendingIntent);
    }

    public int convertTime(int inpTime) {
        final Calendar currentTime = Calendar.getInstance();
        int currhour = currentTime.get(Calendar.HOUR_OF_DAY);
        int currminute = currentTime.get(Calendar.MINUTE);
        // int currsecond = currentTime.get(Calendar.SECOND);
        //int currmills = currentTime.get(Calendar.MILLISECOND);

        currminute = currminute + (currhour * 60);
        int currsecond = (currminute * 60);
        int currmills = (currsecond * 1000);


        int inpMinutes = inpTime % 100;
        int inpHrs = (inpTime - inpMinutes) / 100;
        int inpTotalMinutes = (inpHrs * 60) + inpMinutes;
        int inpTotalSeconds = inpTotalMinutes * 60;
        int inpTotalMills = inpTotalSeconds * 1000;

        int returnTime = inpTotalMills - currmills;

        return returnTime;
    }
    public String formatTime(int i) {
        String times = "";
        if (i < 60) {
            if (i < 10) {
                times += ("00:0" + i + "\n");
            } else {
                times += ("00:" + i) + "\n";
            }
        } else if (i % 100 < 10) {
            times += (i - i % 100) / 100 + ":" + "0" + (i % 100) + "\n";
        } else {
            times += (i - i % 100) / 100 + ":" + (i % 100) + "\n";
        }
        return times;
    }
}
