package com.rjjhsys.gestureultimate;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rjjhsys.gestureultimate.FilterUtils.Filtfilt;
import com.rjjhsys.gestureultimate.WekaUtils.UseModelGesture2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    Button train;
    Button recognition;
    TextView message;
    Button drawO;
    Button drawV;
    int flag = -1; //1-不执行任何操作，0表示进行数据O数据采集，1表示进行数据V采集，2表示进行手势识别阶段
    StringBuilder stringBuilder;
    String warning = "1. 每种手势可变换速度执行（快中慢），每种手势总执行次数最好大于5次；\n\n2. 点击O V选择相应手势执行；\n\n3. “按住”绿色区域进行手势手机采集，“松开”停止采集；\n\n4. " +
            "数据采集结束后，点击“识别”按钮，更新模型，并进入手势识别模式。";
    String warning2 = "按住下面绿色区域进行手势（O V）识别,松开出现出现识别结果。";
    LinearLayout linear2;
    //进行手势数据收集的变量设置*******************************进行手势数据收集的变量设置
    boolean statusRecogbition = false; //对屏幕操作，是否按下和是否抬起
    SensorManager sensorManager;
    private Filtfilt filtfilt = new Filtfilt();
    ArrayList accx = new ArrayList<>();
    ArrayList accy = new ArrayList<>();
    ArrayList accz = new ArrayList<>();
    ArrayList gyx = new ArrayList<>();
    ArrayList gyy = new ArrayList<>();
    ArrayList gyz = new ArrayList<>();
    Instances data; //实例变量
    UseModelGesture2 useModelGesture2 = new UseModelGesture2();
    private double[] feature = new double[210];//进行特征数组数据存储
    private double[] B2 = {5.51450551888877e-12,5.51450551888877e-11,2.48152748349995e-10,6.61740662266652e-10,1.15804615896664e-09,1.38965539075997e-09,1.15804615896664e-09,6.61740662266652e-10,2.48152748349995e-10,5.51450551888877e-11,5.51450551888877e-12};
    private double[] A2 = {1,-8.99594505535417,36.4633075498862,-87.6908102873418,138.559912556039,-150.302116273606,113.348650426591,-58.6790854259776,19.9562604962225,-4.02604302102184,0.365869040210561};
    private ArrayList BB = new ArrayList();
    private ArrayList AA = new ArrayList();
    //进行手势识别的变量设置*******************************进行手势识别的变量设置
    RandomForest randomForest = new RandomForest();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);//加载界面资源
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        train = (Button) findViewById(R.id.train);
        recognition = (Button) findViewById(R.id.recognition);
        message = (TextView) findViewById(R.id.message);
        drawO = (Button) findViewById(R.id.getO);
        drawV = (Button) findViewById(R.id.getV);
        Toast.makeText(this,"请点击训练或者识别按钮\n手机打开app存储权限",Toast.LENGTH_LONG).show();//进行界面显示
        try {
            init();//实例初始化
        } catch (Exception e) {
            e.printStackTrace();
        }
        train.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawV.setEnabled(true);
                drawO.setEnabled(true);
                message.setText(warning);
            }
        });
        recognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawO.setEnabled(false);
                drawV.setEnabled(false);
                flag=2;
                message.setText(warning2);
                //将得到的总的实例进行保存
                try {
                    writeInstance();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    getModel();  //得到模型
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        drawO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = 0;
                String result=" ";
                result = warning+"\n \n\n\n\n                            进行 O 数据收集";
                message.setText(result);
            }
        });
        drawV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = 1;
                String result=" ";
                result = warning+"\n \n\n\n\n                            进行 V 数据收集";
                message.setText(result);
            }
        });
//        //进行界面采集数据，并且进行实例的添加
        linear2 = (LinearLayout) findViewById(R.id.Linear2);
        linear2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int eventaction = motionEvent.getAction();
                switch (eventaction) {
                    case MotionEvent.ACTION_DOWN:
                        linear2.setBackgroundColor(getResources().getColor(R.color.colorWhite));
                        if (flag != -1) {
                            statusRecogbition = true;
                            break;
                        }
                        else{
                            Toast.makeText(getApplicationContext(),"点击按钮  ［训练］  或  ［识别］",Toast.LENGTH_SHORT).show();
                        }
                    case MotionEvent.ACTION_UP:
                        linear2.setBackgroundColor(getResources().getColor(R.color.colorGreen));
                        if (flag != -1&&(flag==0||flag==1)) {
                            //进行手势数据采集*****************************************************
                            statusRecogbition = false;
                            if (accx.size()<=30){
                                clearList();
                            }else {
                                deal(); //实例里面加入数据
                                try {
                                    judgeWord(); //进行界面数据的显示
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        }
                        else if(flag != -1&&flag==2){
                            //进行手势识别阶段*******************************************************
                            statusRecogbition = false;
                            System.out.println("识别");
                            if (accx.size()<=30){
                                clearList();
                            }else {
                                getResult();
                            }
                        }
                        else if(flag==-1){
                            Toast.makeText(getApplicationContext(),"点击按钮  ［训练］  或  ［识别］",Toast.LENGTH_SHORT).show();
                        }
                }
                return true;
            }
        });
    }
    public void init() throws Exception {
        for (int i = 0; i < B2.length; i++) {//对传感器参数进行赋值
            BB.add(B2[i]);
            AA.add(A2[i]);
        }
        File sdcardDir = Environment.getExternalStorageDirectory();
        //得到一个路径，内容是sdcard的文件夹路径和名字
        String path = sdcardDir.getPath() + "//trainModel//feature.arff";
        File path1 = new File(path);
        if (path1.exists()) {
            //若不存在，创建目录，可以在应用启动的时候创建
            ArffLoader arffloader = new ArffLoader();
            arffloader.setFile(path1);
            data = arffloader.getDataSet();//得到实例
            data.setClassIndex(data.numAttributes()-1);
            double sum = data.numInstances(); //得到实例数目
            System.out.println(sum);
        }
        else{
            InputStream inputStream = getAssets().open("gesture.arff");
            ArffLoader arffloader = new ArffLoader();
            arffloader.setSource(inputStream);
            data = arffloader.getDataSet();//得到实例
            data.setClassIndex(data.numAttributes()-1);
            double sum = data.numInstances(); //得到实例数目
            System.out.println(sum);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),8333);
        sensorManager.registerListener(this,sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),8333);
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                if(statusRecogbition==true){
                    accx.add((double)(sensorEvent.values[0]));
                    accy.add((double)(sensorEvent.values[1]));
                    accz.add((double)(sensorEvent.values[2]));
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                if(statusRecogbition==true){
                    gyx.add((double)(sensorEvent.values[0]));
                    gyy.add((double)(sensorEvent.values[1]));
                    gyz.add((double)(sensorEvent.values[2]));
                }
                break;
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    public void deal(){
        //用于处理得到的传感器数据,使用滤波器对数据进行处理
        accx =  filtfilt.doFiltfilt(BB, AA, accx);
        accy =  filtfilt.doFiltfilt(BB, AA, accy);
        accz =  filtfilt.doFiltfilt(BB, AA, accz);
        gyx =  filtfilt.doFiltfilt(BB, AA, gyx);
        gyy =  filtfilt.doFiltfilt(BB, AA, gyy);
        gyz =  filtfilt.doFiltfilt(BB, AA, gyz);
        if(accx==null){
            Toast.makeText(this,"手势不完整，重新采集",Toast.LENGTH_SHORT).show();
            accx = new ArrayList<>();
            accy = new ArrayList<>();
            accz = new ArrayList<>();
            gyx = new ArrayList<>();
            gyy = new ArrayList<>();
            gyz = new ArrayList<>();
        }
        else {
            //特征提取
            feature = FeatureExtract2.getFeature(accx, accy, accz, gyx, gyy, gyz);
            //通过判断number的大小，判断传入的手势的种类
            data = useModelGesture2.createNewInstances(data, feature, flag);
            clearList();
        }
    }
    public void judgeWord(){
        if(flag==0){
            String result=" ";
            result = warning+"\n \n\n\n\n                            进行 O 数据收集";
            message.setText(result);
        }
        else if(flag==1){
            String result=" ";
            result = warning+"\n \n\n\n\n                            进行 V 数据收集";
            message.setText(result);
        }
    }
    public void writeInstance() throws IOException {
        File sdcardDir = Environment.getExternalStorageDirectory();
        //得到一个路径，内容是sdcard的文件夹路径和名字
        String path = sdcardDir.getPath() + "//trainModel//";
        File path1 = new File(path);
        if (!path1.exists()) {
            //若不存在，创建目录，可以在应用启动的时候创建
            path1.mkdirs();
            System.out.println("不存在");
        }
        String path2 = path + "//feature.arff";
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(path2));
        saver.writeBatch();
        System.out.println("保存成功");
    }
    public void getModel() throws Exception {
        //通过读取文件进行model的得到
        File sdcardDir = Environment.getExternalStorageDirectory();
        //得到一个路径，内容是sdcard的文件夹路径和名字
        String path = sdcardDir.getPath() + "//trainModel//";
        String path2 = path + "//feature.arff";
        File file = new File(path2);
        if(file.exists()){//判断文件的存在性,存在说明进行了训练，得到新的训练文件
            ArffLoader arffloader = new ArffLoader();
            arffloader.setFile(file);
            data = arffloader.getDataSet();//得到实例
            data.setClassIndex(data.numAttributes()-1);
            //进行随机森林的参数的设置
            randomForest.buildClassifier(data); //得到模型
        }
        else {
            InputStream inputStream = getAssets().open("gesture.arff");
            ArffLoader arffloader = new ArffLoader();
            arffloader.setSource(inputStream);
            data = arffloader.getDataSet();//得到实例
            data.setClassIndex(data.numAttributes()-1);
            //进行随机森林的参数的设置
            randomForest.buildClassifier(data); //得到模型
        }
    }
    public void getResult(){
        accx =  filtfilt.doFiltfilt(BB, AA, accx);
        accy =  filtfilt.doFiltfilt(BB, AA, accy);
        accz =  filtfilt.doFiltfilt(BB, AA, accz);
        gyx =  filtfilt.doFiltfilt(BB, AA, gyx);
        gyy =  filtfilt.doFiltfilt(BB, AA, gyy);
        gyz =  filtfilt.doFiltfilt(BB, AA, gyz);
        //特征提取
        feature = FeatureExtract2.getFeature(accx, accy, accz, gyx, gyy, gyz);
        data = UseModelGesture2.createNewInstances(data, feature);
        double result = -1;
        //识别
        try {
            result = randomForest.classifyInstance(data.instance(data.numInstances() - 1));
            data.delete(data.numInstances()-1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("机器学习返回的是"+result);
        String value=" ";
        if(result==0){
            value = "0";
        }
        else if(result==1){
            value = "V";
        }
        String warn = warning2+"\n得到手势为\n\n\n\n"+"                                         "+value;
        message.setText(warn);
        clearList();
    }
    public void clearList(){
        accx.clear();
        accy.clear();
        accz.clear();
        gyx.clear();
        gyy.clear();
        gyz.clear();
        //将所有的链表数据清空
    }
}





