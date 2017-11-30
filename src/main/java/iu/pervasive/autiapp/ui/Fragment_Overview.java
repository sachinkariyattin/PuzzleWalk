/*
 * Copyright 2014 Thomas Hoffmann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package iu.pervasive.autiapp.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.BarModel;
import org.eazegraph.lib.models.PieModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import iu.pervasive.autiapp.BuildConfig;
import iu.pervasive.autiapp.Database;
import iu.pervasive.autiapp.R;
import iu.pervasive.autiapp.SensorListener;
import iu.pervasive.autiapp.util.Logger;
import iu.pervasive.autiapp.util.Util;

import static android.content.Context.VIBRATOR_SERVICE;

public class Fragment_Overview extends Fragment implements SensorEventListener {

    private TextView stepsView, totalView, averageView;

    private PieModel sliceGoal, sliceCurrent;
    private PieChart pg;

    private int todayOffset, total_start, goal, since_boot, total_days;
    public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());
    private boolean showSteps = true;

    private boolean milestone20 = false;
    private boolean milestone40 = false;
    private boolean milestone60 = false;
    private boolean milestone80 = false;
    private boolean milestone100 = false;

    int[] puzzle_images = {R.drawable.p_1, R.drawable.p_2, R.drawable.p_3, R.drawable.p_4, R.drawable.p_5 };

    String[][] options = {
            {"Speed", "Hiking", "Distance", "Direction"},
            {"8", "9", "10", "11"},
            {"15", "25", "35", "45"},
            {"14", "13", "10", "15"},
            {"62", "56", "78", "81"}
    };
    String[] answers = {
            "Direction",
            "9",
            "35",
            "14",
            "56"
    };
    int indexQuestion = 0;
    int actualScore = 0;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View v = inflater.inflate(iu.pervasive.autiapp.R.layout.fragment_overview, null);
        stepsView = (TextView) v.findViewById(iu.pervasive.autiapp.R.id.steps);
        totalView = (TextView) v.findViewById(iu.pervasive.autiapp.R.id.total);
        averageView = (TextView) v.findViewById(iu.pervasive.autiapp.R.id.average);

        Resources res = getResources(); //resource handle
        Drawable drawable = res.getDrawable(R.drawable.sidewalk);
        v.setBackground(drawable);

        pg = (PieChart) v.findViewById(iu.pervasive.autiapp.R.id.graph);

        // slice for the steps taken today
        sliceCurrent = new PieModel("", 0, Color.parseColor("#4da6ff"));
        pg.addPieSlice(sliceCurrent);

        // slice for the "missing" steps until reaching the goal
        sliceGoal = new PieModel("", Fragment_Settings.DEFAULT_GOAL, Color.parseColor("#B0B0B0"));
        pg.addPieSlice(sliceGoal);

        pg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                showSteps = !showSteps;
                stepsDistanceChanged();
            }
        });

        pg.setDrawValueInPie(false);
        pg.setUsePieRotation(true);
        pg.startAnimation();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);

        Database db = Database.getInstance(getActivity());

        if (BuildConfig.DEBUG) db.logState();
        // read todays offset
        todayOffset = db.getSteps(Util.getToday());

        SharedPreferences prefs =
                getActivity().getSharedPreferences("autiapp", Context.MODE_PRIVATE);

        goal = prefs.getInt("goal", Fragment_Settings.DEFAULT_GOAL);
        since_boot = db.getCurrentSteps(); // do not use the value from the sharedPreferences
        int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

        // register a sensorlistener to live update the UI if a step is taken
        if (!prefs.contains("pauseCount")) {
            SensorManager sm =
                    (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (sensor == null) {
                new AlertDialog.Builder(getActivity()).setTitle(iu.pervasive.autiapp.R.string.no_sensor)
                        .setMessage(iu.pervasive.autiapp.R.string.no_sensor_explain)
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(final DialogInterface dialogInterface) {
                                getActivity().finish();
                            }
                        }).setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).create().show();
            } else {
                sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
            }
        }

        since_boot -= pauseDifference;

        total_start = db.getTotalWithoutToday();
        total_days = db.getDays();

        db.close();

        stepsDistanceChanged();
    }

    /**
     * Call this method if the Fragment should update the "steps"/"km" text in
     * the pie graph as well as the pie and the bars graphs.
     */
    private void stepsDistanceChanged() {
        if (showSteps) {
            ((TextView) getView().findViewById(iu.pervasive.autiapp.R.id.unit)).setText(getString(iu.pervasive.autiapp.R.string.steps));
        } else {
            String unit = getActivity().getSharedPreferences("autiapp", Context.MODE_PRIVATE)
                    .getString("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT);
            if (unit.equals("cm")) {
                unit = "km";
            } else {
                unit = "mi";
            }
            ((TextView) getView().findViewById(iu.pervasive.autiapp.R.id.unit)).setText(unit);
        }

        updatePie();
        updateBars();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            SensorManager sm =
                    (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            sm.unregisterListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Database db = Database.getInstance(getActivity());
        db.saveCurrentSteps(since_boot);
        db.close();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(iu.pervasive.autiapp.R.menu.main, menu);
        MenuItem pause = menu.getItem(0);
        Drawable d;
        if (getActivity().getSharedPreferences("autiapp", Context.MODE_PRIVATE)
                .contains("pauseCount")) { // currently paused
            pause.setTitle(iu.pervasive.autiapp.R.string.resume);
            d = getResources().getDrawable(iu.pervasive.autiapp.R.drawable.ic_resume);
        } else {
            pause.setTitle(iu.pervasive.autiapp.R.string.pause);
            d = getResources().getDrawable(iu.pervasive.autiapp.R.drawable.ic_pause);
        }
        d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        pause.setIcon(d);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case iu.pervasive.autiapp.R.id.action_pause:
                SensorManager sm =
                        (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
                Drawable d;
                if (getActivity().getSharedPreferences("autiapp", Context.MODE_PRIVATE)
                        .contains("pauseCount")) { // currently paused -> now resumed
                    sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
                            SensorManager.SENSOR_DELAY_UI, 0);
                    item.setTitle(iu.pervasive.autiapp.R.string.pause);
                    d = getResources().getDrawable(iu.pervasive.autiapp.R.drawable.ic_pause);
                } else {
                    sm.unregisterListener(this);
                    item.setTitle(iu.pervasive.autiapp.R.string.resume);
                    d = getResources().getDrawable(iu.pervasive.autiapp.R.drawable.ic_resume);
                }
                d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                item.setIcon(d);
                getActivity().startService(new Intent(getActivity(), SensorListener.class)
                        .putExtra("action", SensorListener.ACTION_PAUSE));
                return true;
            default:
                return ((Activity_Main) getActivity()).optionsItemSelected(item);
        }
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, int accuracy) {
        // won't happen
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (BuildConfig.DEBUG)
            Logger.log("UI - sensorChanged | todayOffset: " + todayOffset + " since boot: " +
                    event.values[0]);
        if (event.values[0] > Integer.MAX_VALUE || event.values[0] == 0) {
            return;
        }
        if (todayOffset == Integer.MIN_VALUE) {
            // no values for today
            // we dont know when the reboot was, so set todays steps to 0 by
            // initializing them with -STEPS_SINCE_BOOT
            todayOffset = -(int) event.values[0];
            Database db = Database.getInstance(getActivity());
            db.insertNewDay(Util.getToday(), (int) event.values[0]);
            db.close();
        }
        since_boot = (int) event.values[0];
        updatePie();
    }

    /**
     * Updates the pie graph to show todays steps/distance as well as the
     * yesterday and total values. Should be called when switching from step
     * count to distance.
     */
    private void updatePie() {
        if (BuildConfig.DEBUG) Logger.log("UI - update steps: " + since_boot);
        // todayOffset might still be Integer.MIN_VALUE on first start
        int steps_today = Math.max(todayOffset + since_boot, 0);
        sliceCurrent.setValue(steps_today);
        if (steps_today <= 20 && milestone20 == true) {
            resetMilestones();
        }
        if (steps_today >= 20 && steps_today <= 30 && !milestone20) {
            milestone20 = true;
            ((Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE)).vibrate(1000);
            openDialog(buildMilestoneDialog(20));
        }
        else if (steps_today >= 40 && steps_today <= 50 && !milestone40) {
            milestone40 = true;
            ((Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE)).vibrate(1000);
            openDialog(buildMilestoneDialog(40));
        }
        else if (steps_today >= 60 && steps_today <= 70 && !milestone60) {
            milestone60 = true;
            ((Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE)).vibrate(1000);
            openDialog(buildMilestoneDialog(60));
        }
        else if (steps_today >= 80 && steps_today <= 90 && !milestone80) {
            milestone80 = true;
            ((Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE)).vibrate(1000);
            openDialog(buildMilestoneDialog(80));
        }
        else if (steps_today >= 100 && steps_today <= 110 && !milestone100){
            milestone100 = true;
            ((Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE)).vibrate(1000);
            openDialog(buildMilestoneDialog(100));
        }
        else if (milestone100) {
            ((Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE)).vibrate(1000);
            int wonRewards;
            if (actualScore >= 20) {
                wonRewards = 4;
            }
            else if (actualScore >=10 && actualScore < 20) {
                wonRewards = 2;
            }
            else if (actualScore > 0){
                wonRewards = 1;
            }
            else {
                wonRewards = 0;
            }
            openDialog(finishDialog(wonRewards));
        }
        if (goal - steps_today > 0) {
            // goal not reached yet
            if (pg.getData().size() == 1) {
                // can happen if the goal value was changed: old goal value was
                // reached but now there are some steps missing for the new goal
                pg.addPieSlice(sliceGoal);
            }
            sliceGoal.setValue(goal - steps_today);
        } else {
            // goal reached
            pg.clearChart();
            pg.addPieSlice(sliceCurrent);
        }
        pg.update();
        if (showSteps) {
            stepsView.setText(formatter.format(steps_today));
            totalView.setText(formatter.format(total_start + steps_today));
            averageView.setText(formatter.format((total_start + steps_today) / total_days));
        } else {
            // update only every 10 steps when displaying distance
            SharedPreferences prefs =
                    getActivity().getSharedPreferences("autiapp", Context.MODE_PRIVATE);
            float stepsize = prefs.getFloat("stepsize_value", Fragment_Settings.DEFAULT_STEP_SIZE);
            float distance_today = steps_today * stepsize;
            float distance_total = (total_start + steps_today) * stepsize;
            if (prefs.getString("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT)
                    .equals("cm")) {
                distance_today /= 100000;
                distance_total /= 100000;
            } else {
                distance_today /= 5280;
                distance_total /= 5280;
            }
            stepsView.setText(formatter.format(distance_today));
            totalView.setText(formatter.format(distance_total));
            averageView.setText(formatter.format(distance_total / total_days));
        }
    }

    /**
     * Updates the bar graph to show the steps/distance of the last week. Should
     * be called when switching from step count to distance.
     */
    private void updateBars() {
        SimpleDateFormat df = new SimpleDateFormat("E", Locale.getDefault());
        BarChart barChart = (BarChart) getView().findViewById(iu.pervasive.autiapp.R.id.bargraph);
        if (barChart.getData().size() > 0) barChart.clearChart();
        int steps;
        float distance, stepsize = Fragment_Settings.DEFAULT_STEP_SIZE;
        boolean stepsize_cm = true;
        if (!showSteps) {
            // load some more settings if distance is needed
            SharedPreferences prefs =
                    getActivity().getSharedPreferences("autiapp", Context.MODE_PRIVATE);
            stepsize = prefs.getFloat("stepsize_value", Fragment_Settings.DEFAULT_STEP_SIZE);
            stepsize_cm = prefs.getString("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT)
                    .equals("cm");
        }
        barChart.setShowDecimal(!showSteps); // show decimal in distance view only
        BarModel bm;
        Database db = Database.getInstance(getActivity());
        List<Pair<Long, Integer>> last = db.getLastEntries(8);
        db.close();
        for (int i = last.size() - 1; i > 0; i--) {
            Pair<Long, Integer> current = last.get(i);
            steps = current.second;
            if (steps > 0) {
                bm = new BarModel(df.format(new Date(current.first)), 0,
                        steps > goal ? Color.parseColor("#00b33c") : Color.parseColor("#42d4f4"));
                if (showSteps) {
                    bm.setValue(steps);
                } else {
                    distance = steps * stepsize;
                    if (stepsize_cm) {
                        distance /= 100000;
                    } else {
                        distance /= 5280;
                    }
                    distance = Math.round(distance * 1000) / 1000f; // 3 decimals
                    bm.setValue(distance);
                }
                barChart.addBar(bm);
            }
        }
        if (barChart.getData().size() > 0) {
            barChart.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    Dialog_Statistics.getDialog(getActivity(), since_boot).show();
                }
            });
            barChart.startAnimation();
        } else {
            barChart.setVisibility(View.GONE);
        }
    }

    public int getRandomID(int max_value){
        return new Random().nextInt(max_value);
    }

    public AlertDialog.Builder buildDialog2(final String Answer, int question_image, final String[] optns){
        ImageView image = new ImageView(getActivity());
        image.setImageResource(question_image);

        return new AlertDialog.Builder(getActivity())
                .setTitle("Solve the below puzzle")
                .setView(image)
                .setSingleChoiceItems(optns, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(optns[whichButton].equals(Answer)){
                            actualScore += 5;
                            dialog.dismiss();
                            openDialog(correctAnswerDialog());
                        }else {
                            actualScore -= 2;
                            dialog.dismiss();
                            openDialog(wrongAnswerDialog());
                        }
                    }
                });
    }

    public void openDialog(AlertDialog.Builder builder) {
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public AlertDialog.Builder buildMilestoneDialog(int steps) {

        ImageView image = new ImageView(getActivity());
        image.setImageResource(R.drawable.milestone);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("You reached a milestone "+ steps +" Steps")
                .setView(image)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        indexQuestion = getRandomID(puzzle_images.length);
                        openDialog(buildDialog2(answers[indexQuestion], puzzle_images[indexQuestion], options[indexQuestion]));
                    }
                });
        return builder;
    }

    public AlertDialog.Builder correctAnswerDialog() {
        ImageView image = new ImageView(getActivity());
        image.setImageResource(R.drawable.correct);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Right answer !!!  Your score: "+ actualScore)
                .setView(image)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        return builder;
    }

    public AlertDialog.Builder wrongAnswerDialog() {
        ImageView image = new ImageView(getActivity());
        image.setImageResource(R.drawable.wrong);
        if (actualScore < 0) {
            actualScore = 0;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Wrong answer !!! Your score: "+ actualScore)
                .setView(image)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        return builder;
    }

    public AlertDialog.Builder finishDialog(int rewards) {
        ImageView image = new ImageView(getActivity());
        image.setImageResource(R.drawable.finish);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("You have finished the challenge !!! \n Your score: "+ actualScore + "\n Rewards won "+ rewards + "\n\n Head back home and check the rewards")
                .setView(image)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        return builder;
    }

    public void resetMilestones() {
        milestone20 = false;
        milestone40 = false;
        milestone80 = false;
        milestone60 = false;
        milestone100 = false;
    }

}
