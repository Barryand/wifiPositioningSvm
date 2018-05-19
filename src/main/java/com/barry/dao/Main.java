package com.barry.dao;

import Entity.PointLocEntity;
import service.svm_predict;
import service.svm_scale_for_predict;
import service.svm_scale_for_train;
import service.svm_train;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Barry on 2018/4/17.
 */


public class Main {
    /**
     *@描述  c和gamma是事先用grid.py计算的结果,若换了新的训练集，需要重新计算c、g
     *
     *@创建人  Barry
     *@创建时间  2018/4/23
     *@修改人和其它信息
     */
    public static void main(String[] args) throws IOException {
        Main main = new Main();
        String recordFilename = "trainfile\\allRecord.txt";
        main.recordFileFormating(recordFilename, recordFilename);//对训练文件进行重新组织，这里是覆盖掉原始文件

        String[] sarg = {"-l", "0", "-u", "1", "-s", "trainfile\\scalerule.txt", //存放SVM缩放规则的路径
                "-z", recordFilename//存放缩放后数据的路径,这里是覆盖掉原始文件
                , recordFilename};//需要缩放的数据
        String[] targ = { "-c", "128", "-g", "0.5", //c和gamma是事先用grid.py计算的结果
                recordFilename, //存放SVM训练模型用的数据的路径
                "trainfile\\model_r.txt"}; //存放SVM通过训练数据训练出来的模型的路径
        main.svmTraining(sarg, targ);

//测试用,使用Main.main（...）时，将main的参数改为(Map<String, Double> userData, List<PointLocEntity> pointLocEntityList)
        Map<String, Double> userData = new HashMap<String, Double>();
        String[] AP = {"abc2", "abc3", "abc4", "abc6", "abc7", "abc8", "MERCURY_B932", "MERCURY_BD09",
                "MERCURY_CFF9", "Xiaomi_31CB_CE34", "Xiaomi_3525_CADA"};
        double[] RSSI = {-71, -57, -46, -33, -40, -39, -63, -0, -0, -65, -46};
        for (int i = 0; i < 11; i++) {
            userData.put(AP[i], RSSI[i]);
        }

//测试用
        List<PointLocEntity> pointLocEntityList = new ArrayList<PointLocEntity>();
        String locationFilename = "trainfile\\location.txt";
        File file = new File(locationFilename);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String templine = null;
        String[] temp = null;
        while ((templine = bufferedReader.readLine()) != null) {
            temp = templine.split("\\s+");
            PointLocEntity tempPointLocEntity = new PointLocEntity();
            tempPointLocEntity.setPoint_name(temp[0]);
            tempPointLocEntity.setX(Integer.valueOf(temp[1]));
            tempPointLocEntity.setY(Integer.valueOf(temp[2]));
            pointLocEntityList.add(tempPointLocEntity);
        }
        bufferedReader.close();


        String formatedUserData = null;
        formatedUserData = main.userDataFormating(userData);//对用户上传的数据进行重新组织
        String[] sarg2 = {"-r", "trainfile\\scalerule.txt"};//重载SVM缩放规则
        String[] parg = {"trainfile\\model_r.txt" }; //调用的是训练以后的模型
        main.svmPredicting(formatedUserData, pointLocEntityList, sarg2, parg);
    }
    /**
     *@描述   对训练文件进行重新组织
     *@参数  [filename, formatedFilename]
     *@返回值  void
     *@注意   因为allRecord.txt中每一行没有说明是哪个点，只知道100行对应一个点，因此
     * 有点麻烦的就是，当加入新的训练数据，而新的点测的次数不是100次时怎么处理
     *@创建人  Barry
     *@创建时间  2018/4/19
     *@修改人和其它信息
     */
    private void recordFileFormating(String filename, String formatedFilename) {

        File file = new File(filename);
        String tempLine, outLine;
        PrintWriter outputStream = null;
        BufferedReader bufferedReader = null;
        int MinuteHand = 1, SecondHand = 1;  //MinuteHand指一共50个点，SecondHand指每个点测100次
        try {
            bufferedReader = new BufferedReader(new FileReader(file));

            StringBuffer stringBuffer = new StringBuffer();//由于输入文件和输出文件文件名相同，所以得建立一个缓冲区，读完后再写入文件
            while ((tempLine = bufferedReader.readLine()) != null) {
                if (SecondHand > 100) {
                    SecondHand = 1;
                    MinuteHand++;
                }
                outLine = reorganize(tempLine, MinuteHand);
                stringBuffer.append(outLine).append(System.getProperty("line.separator"));
                SecondHand++;
            }
            outputStream = new PrintWriter(new FileOutputStream(formatedFilename));
            outputStream.print(stringBuffer);
            outputStream.flush();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("文件读取异常");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
            try {
                assert bufferedReader != null;
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private String reorganize(String tempLine, int MinuteHand) {
        String[] AP = {"abc2", "abc3", "abc4", "abc6", "abc7", "abc8", "MERCURY_B932", "MERCURY_BD09",
        "MERCURY_CFF9", "Xiaomi_31CB_CE34", "Xiaomi_3525_CADA"};
        StringBuilder sb = new StringBuilder(MinuteHand + "");
        Matcher matcher = Pattern.compile("\\d+").matcher(tempLine);
        int offset = 0;   //为文件格式作补偿
        for (int i = 0; i < 11; i++) {
            if (i <= 5) {
                offset = 6;
            } else if (i <= 8) {
                offset = 14;
            } else {
                offset = 18;
            }
            if (tempLine.indexOf(AP[i]) != -1) { //当一行数据中有某个AP
                matcher.find(tempLine.indexOf(AP[i]) + offset);
                sb.append(" ").append(i+1).append(":").append(matcher.group());
            } else {
                sb.append(" ").append(i+1).append(":").append(0);
            }
        }
        return sb.toString();
    }

    private String userDataFormating(Map<String, Double> userData) {
        String[] AP = {"abc2", "abc3", "abc4", "abc6", "abc7", "abc8", "MERCURY_B932", "MERCURY_BD09",
                "MERCURY_CFF9", "Xiaomi_31CB_CE34", "Xiaomi_3525_CADA"};
        List<Double> value = new ArrayList<Double>();
        for (String key : AP) {
            value.add(-userData.get(key));
        }
        String formatedUserData = 0 + " ";
        for (int i = 1; i < 12; i++) {
            formatedUserData = formatedUserData + i + ":" + value.get(i-1) + " ";
        }
        return formatedUserData;
    }
    private void svmTraining(String[] sarg, String[] targ)throws IOException {

        System.out.println("........SVM训练开始..........");
        svm_scale_for_train s = new svm_scale_for_train();//创建一个缩放对象
        s.main(sarg);
        svm_train t = new svm_train();//创建一个训练对象
        t.main(targ);  //调用

    }
    private void svmPredicting(String formatedUserData, List<PointLocEntity> pointLocEntityList,
                               String[] sarg2, String[] parg)throws IOException {
        svm_predict p = new svm_predict();//创建一个预测或者分类的对象
        svm_scale_for_predict s = new svm_scale_for_predict();//创建一个缩放对象
        StringBuffer scaledUserData = new StringBuffer();
        s.main(sarg2, formatedUserData, scaledUserData);
        List<Double> forecastNumbers = new ArrayList<Double>();
        p.main(parg, scaledUserData, forecastNumbers); //forecastNumbers本应为每一行测试数据预测结果的采样点序号
        double forecastNumber = forecastNumbers.get(0);//不过由于版本变化，只有一行用户数据需要测试，因此也只有一个输出位置
        //locationResult(forecastNumber, pointLocEntityList);
        PointLocEntity locationResult = pointLocEntityList.get((int)forecastNumber - 1);//forecastNumber指的是第几个采样点，所以需要-1
        System.out.println(locationResult.getPoint_name());
    }

}

