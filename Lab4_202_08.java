package lab4_202_08.uwaterloo.ca.lab4_202_08;

import android.graphics.Color;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Button;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Locale;

import ca.uwaterloo.sensortoy.LineGraphView;
import mapper.*;


class RotationVectorEventListener implements SensorEventListener {
    TextView output;
    static double angle;

    public RotationVectorEventListener(TextView outputView){
        output = outputView;
    }

    public void onAccuracyChanged(Sensor s, int i) {}

    public void onSensorChanged(SensorEvent se){
        if (se.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            //Show the current values of rotation vector on screen
            String v = String.format(Locale.getDefault(), "(%.2f, %.2f, %.2f)", se.values[0], se.values[1], se.values[2]);
            String t = "Rotation Vector Values: " + v;
            output.setText(t);

            //create new vectors to hold values from the getRotationMatrixFromVector function and getOrientation function
            float[] R = new float[9];
            float[] orientation = new float[3];

            SensorManager.getRotationMatrixFromVector(R,se.values);

            //reorients the vectors from the rotation matrix function to allow the phone to function when being held up instead of flat
            float[] RotAxis = new float[9];
            SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, RotAxis);

            //uses getOrientation function to get angle from North in radians
            SensorManager.getOrientation(RotAxis, orientation);
            angle = orientation[0];
            output.setText("Angle from North: " + Math.toDegrees(angle));

        }
    }
}


public class Lab4_202_08 extends AppCompatActivity {

    LineGraphView graph;
    mapper.MapView mv;
    NavigationalMap map;
    PointF pf = new PointF();
    float yAxis = (float)18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab4_202_08);

        //adds map of room by calling function
        mv = new mapper.MapView(getApplicationContext(), 1200, 1200, 68, 68);
        registerForContextMenu(mv);
        map = mapper.MapLoader.loadMap(getExternalFilesDir(null), "Lab-room-peninsula.svg");
        mv.setMap(map);

        mv.addListener(Position);


        TextView ls = (TextView) findViewById(R.id.label1); //light sensor text view
        ls.setText(" ");
        //TextView for rotation
        TextView Rot = new TextView(getApplicationContext());
        Rot.setTextColor(Color.BLACK); //change colour of rotation textview

        LinearLayout l = (LinearLayout) findViewById(R.id.label2); //set the linear layout orientation
        l.setOrientation(LinearLayout.VERTICAL);

        mv.setVisibility(View.VISIBLE);
        l.addView(mv); //add map to display

        //graph = new LineGraphView(getApplicationContext(),100, Arrays.asList("x", "y", "z"));
        //l.addView(graph);
        //graph.setVisibility(View.VISIBLE);

        TextView Ac = new TextView(getApplicationContext()); //Accelerometer text view
        Ac.setText("Accelerometer");
        l.addView(Ac);
        l.addView(Rot);

        TextView directions = new TextView(getApplicationContext());
        directions.setTextColor(Color.BLACK);
        l.addView(directions);


        Button button = (Button)findViewById(R.id.button);

        //setting the colours of all the labels to black
        ls.setTextColor(0xff000000);
        Ac.setTextColor(0xff000000);

        //Accelerometer
        SensorManager sensorManager1 = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor Accelerometer = sensorManager1.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        SensorEventListener a = new AccelerometerEventListener(Ac, mv, ls);
        sensorManager1.registerListener(a,Accelerometer,SensorManager.SENSOR_DELAY_NORMAL);

        //Rotation Vector
        SensorManager sensorManager2 = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor RotationVector = sensorManager2.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        SensorEventListener r = new RotationVectorEventListener(Rot);
        sensorManager2.registerListener(r, RotationVector, SensorManager.SENSOR_DELAY_NORMAL);


    }

    PositionListener Position = new PositionListener() {
    @Override
    public void originChanged(MapView source, PointF loc) {
    mv.setUserPoint(loc);

    //Path finding
    PointF origin = new PointF();
    origin.set(mv.getOriginPoint());
    PointF destination = new PointF();
    destination.set(mv.getDestinationPoint());
        ArrayList<PointF> paths = new ArrayList<PointF>();
    if (map.calculateIntersections(origin, destination).isEmpty()) {
        paths.add(origin);
        paths.add(destination);
    }
    else {
        PointF pathOne = new PointF(origin.x, yAxis);
        PointF pathTwo = new PointF(destination.x, yAxis);
        paths.add(origin);
        paths.add(pathOne);
        paths.add(pathTwo);
        paths.add(destination);
    }

    mv.setUserPath(paths);
}

@Override
public void destinationChanged(MapView source, PointF dest) {
mv.setDestinationPoint(dest);

PointF origin = new PointF();
origin.set(mv.getOriginPoint());
PointF destination = new PointF();
destination.set(mv.getDestinationPoint());
    ArrayList<PointF> paths = new ArrayList<PointF>();
    if (map.calculateIntersections(origin, destination).isEmpty()) {
        paths.add(origin);
        paths.add(destination);
    }
    else {
        PointF pathOne = new PointF(origin.x, yAxis);
        PointF pathTwo = new PointF(destination.x, yAxis);
        paths.add(origin);
        paths.add(pathOne);
        paths.add(pathTwo);
        paths.add(destination);
    }

mv.setUserPath(paths);
}
};

@Override
public  void  onCreateContextMenu(ContextMenu  menu , View v, ContextMenu.ContextMenuInfo menuInfo) {
super.onCreateContextMenu(menu , v, menuInfo);
mv.onCreateContextMenu(menu , v, menuInfo);
}
@Override
public  boolean  onContextItemSelected(MenuItem item) {
return  super.onContextItemSelected(item) ||  mv.onContextItemSelected(item);
}

class AccelerometerEventListener implements SensorEventListener {
    TextView output;
    TextView notice;

    int count = 0; //defined a variable to count steps
    int state = 0; //defined a variable to see the current state of the step
    final int waiting = 0; // defined a variable equal to 0 acceleration or the "waiting" period

    float[] smValues = {0, 0, 0}; //holds filtered values from accelerometer
    float c = (float)2.5;
    float q = (float) 0.7;

    float northSteps = 0; //steps towards north (steps are negative if towards south)
    float eastSteps = 0; //steps towards east (steps are negative if towards west)

    long stepTime = System.currentTimeMillis();

    MapView stepmap;


    Button button = (Button)findViewById(R.id.button);


    public AccelerometerEventListener(TextView outputView, MapView map, TextView popup) {
        output = outputView;
        stepmap = map;
        notice = popup;
    }

    public void onAccuracyChanged(Sensor s, int i) {
    }

    public void onSensorChanged(SensorEvent se) {

        if (se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            String z = String.format(Locale.getDefault(), "(%f, %f, %f)\n", se.values[0], se.values[1], se.values[2]);

            //low pass filter to smooth values
            smValues[0] += (se.values[0] - smValues[0])/c;
            smValues[1] += (se.values[1] - smValues[1])/c;
            smValues[2] += (se.values[2] - smValues[2])/c;

            //defined the finite state machine

            switch(state){

                case 0: if(smValues[1] >= -0.2) state = 1;
                else {
                    state = waiting;
                }
                    break;

                case 1:	if(smValues[1] > 0.2 ) state = 2;
                else {
                    state = waiting;
                }
                    break;

                case 2:	if(smValues[1] > 0.4 ) state = 2;
                else if (smValues[1]<0.4){
                    state = 3;
                }
                else {
                    state = waiting;
                }
                    break;

                case 3:	if(smValues[1] <= 0.11)state = 4;
                else {
                    state = 1;
                }
                    break;

                    //Count a step and turn the state back to waiting

                case 4:	if(System.currentTimeMillis() - stepTime >= 500)  {
                    count++;
                    float newnorthSteps = 0;
                    float neweastSteps = 0;

                    //Code to determine direction travelled
                    double currentAngle = RotationVectorEventListener.angle;
                    if(currentAngle <= Math.PI/2 && currentAngle >= 0){
                        newnorthSteps += Math.abs(Math.cos(currentAngle));
                        neweastSteps += Math.abs(Math.sin(currentAngle));
                    }
                    if(currentAngle < 0 && currentAngle >= -Math.PI/2) {
                        newnorthSteps += Math.abs(Math.cos(currentAngle));
                        neweastSteps -= Math.abs(Math.sin(currentAngle));
                    }
                    if(currentAngle < -Math.PI/2 && currentAngle >= -Math.PI) {
                        newnorthSteps -= Math.abs(Math.cos(currentAngle));
                        neweastSteps -= Math.abs(Math.sin(currentAngle));
                    }
                    if(currentAngle <= Math.PI && currentAngle > Math.PI/2) {
                        newnorthSteps -= Math.abs(Math.cos(currentAngle));
                        neweastSteps += Math.abs(Math.sin(currentAngle));
                    }

                    northSteps += newnorthSteps;
                    eastSteps += neweastSteps;

                    PointF tp = new PointF();
                    tp.set(stepmap.getUserPoint());

                    float newX = tp.x + neweastSteps/q;
                    float newY = tp.y - newnorthSteps/q;
                    stepmap.setUserPoint(newX, newY);

                    if(Math.abs(newX - stepmap.getDestinationPoint().x) < 0.5 && Math.abs(newY - stepmap.getDestinationPoint().y) < 0.5) {
                        notice.setText("You have reached your destination!");
                    }


                    state = waiting;
                }

                else{
                    state = waiting;}

                    stepTime = System.currentTimeMillis();

                    break;

                default: break;
            }

            //Created a button to reset number of steps and turn the state to waiting and turn all calculated values to 0
            button.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    count = 0;
                    state = waiting;
                    northSteps = 0;
                    eastSteps = 0;
                    notice.setText(" ");
                }
            });

            //graph.addPoint(smValues); //added the accelerometer points to the graph

            output.setText(String.format("Step: %d \nState: %d \nSteps to the North: %f \nSteps to the East: %f", count, state, northSteps, eastSteps));

        }
    }
}

}

