package com.example.natha.pilltime;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
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

        try {
            intentExtraPill = getIntent().getStringExtra("pill");
            if (!intentExtraPill.isEmpty()){
                String[] extraPillInfo = intentExtraPill.split("\n");
                dbHelper db = new dbHelper(this);
                db.close();
                currentPill = db.getPillByName(extraPillInfo[0]);
            }
            else {
                currentPill = new Pill();
            }
        }catch(Exception e){}

        poplateData();
    }

    public void Initialize(){
        etName = (EditText) findViewById(R.id.nameET);
        cbActive = (CheckBox) findViewById(R.id.activeCB);
        etPillCount = (EditText) findViewById(R.id.pillCountET);
        etDosage = (EditText) findViewById(R.id.dosageET);
        etNotes = (EditText) findViewById(R.id.notesET);
        lvTimes = (ListView) findViewById(R.id.timesLV);
        //time_picker= (TimePicker) findViewById(R.id.timePicker);

    }
    public void insertToDb(View view){
        dbHelper db = new dbHelper(this);
        int active = 0;
        if(cbActive.isChecked()){
            active = 1;
        }
        else
        {
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
        pill.setTimeTake(currentPillTime, 0);
        db.addPill(pill);
        pill.setId(db.getPillId(pill));
        db.addTime(pill, currentPillTime);
        db.close();

        Intent mainIntent = new Intent(EditMedicationActivity.this, MainActivity.class);
        startActivity(mainIntent);
    }

    public void addNewTime(View view){
        final Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        /*AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setIcon()*/

        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener(){
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                currentPillTime = hourOfDay * 100 + minute;
                currentPill.getAllTimesS().add(currentTime.toString());

                arrayAdapter.add(currentTime.toString());
                arrayAdapter.notifyDataSetChanged();
            }
        };

        TimePickerDialog timePickerDialog = new TimePickerDialog(EditMedicationActivity.this, listener, hour, minute, true);
        timePickerDialog.setTitle("Select a Time");
        timePickerDialog.show();
    }

    public void poplateData(){
        if (!intentExtraPill.isEmpty())
        {
            etName.setText(currentPill.getName());
            etDosage.setText(currentPill.getDosage());
            etNotes.setText(currentPill.getNotes());
            etPillCount.setText(currentPill.getPillCount());
            arrayAdapter = new ArrayAdapter<String>(this, R.layout.pill_list_item, R.id.pillItemTV, currentPill.getAllTimesS());
        }
        else
        {
            etName.setText("");
            etDosage.setText("");
            etNotes.setText("");
            etPillCount.setText("");
            arrayAdapter = new ArrayAdapter<String>(this, R.layout.pill_list_item, R.id.pillItemTV, new String[]{""});
        }
        lvTimes.setAdapter(arrayAdapter);
    }

}