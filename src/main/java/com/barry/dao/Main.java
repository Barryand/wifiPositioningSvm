package com.barry.dao;

import service.svm_predict;
import service.svm_scale;
import service.svm_train;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Barry on 2018/4/17.
 */
public class Main {
    public static void main(String[] args) throws IOException {
        Main main = new Main();
        String filename = "trainfile\\allRecord.txt";
        String formatedFilename = "trainfile\\train1.txt";//对原始文件进行重新组织
        main.fileFormating(filename, formatedFilename);
        main.svmTraining(formatedFilename);
        String testFilename = "trainfile\\test1.txt";
        String locationFilename = "trainfile\\location.txt";
        main.svmPredicting(testFilename, locationFilename);
    }
    private void fileFormating(String filename, String formatedFilename) {
        File file = new File(filename);
        String tempLine, outLine;

        PrintWriter outputStream = null;
        int MinuteHand = 1, SecondHand = 1;  //MinuteHand指一共50个点，SecondHand指每个点测100次
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            outputStream = new PrintWriter(new FileOutputStream(formatedFilename));
            while ((tempLine = bufferedReader.readLine()) != null) {
                if (SecondHand > 100) {
                    SecondHand = 1;
                    MinuteHand++;
                }
                outLine = reorganize(tempLine, MinuteHand);
                outputStream.println(outLine);
                outputStream.flush();
                SecondHand++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("文件读取异常");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            outputStream.close();
        }
    }
    private String reorganize(String tempLine, int MinuteHand) {
        String[] AP = {"TP-LINK_3051", "TP-LINK_35EB", "TP-LINK_3625", "TP-LINK_5958",
                "TP-LINK_E7D2", "Four-Faith-2", "Four-Faith-3"};
        StringBuffer sb = new StringBuffer(MinuteHand + "");
        Matcher matcher = Pattern.compile("\\d+").matcher(tempLine);
        for (int i = 0; i < 7; i++) {
            if (tempLine.indexOf(AP[i]) != -1) {
                matcher.find(tempLine.indexOf(AP[i]) + 14);
                sb.append(" ").append(i).append(":").append(matcher.group());
            } else {
                sb.append(" ").append(i).append(":").append(0);
            }
        }
        return sb.toString();
    }

    private void svmTraining(String formatedFilename)throws IOException {
        String[] sarg = {"-l", "0", "-u", "1", "-s", "trainfile\\scalerule.range", //存放SVM缩放规则的路径
                "-z", "trainfile\\train1scaled.txt"//存放缩放后数据的路径
                , formatedFilename};//需要缩放的数据
        String[] targ = { "-c", "8", "-g", "8", //c和gamma是事先用grid.py计算的结果
                "-b", "1", "trainfile\\train1scaled.txt", //存放SVM训练模型用的数据的路径
                "trainfile\\model_r.txt"}; //存放SVM通过训练数据训练出来的模型的路径
        System.out.println("........SVM训练开始..........");
        svm_scale s = new svm_scale();//创建一个缩放对象
        s.main(sarg);
        svm_train t = new svm_train();//创建一个训练对象
        t.main(targ);  //调用

    }
    private void svmPredicting(String testFilename, String locationFilename)throws IOException {
        String[] sarg2 = {"-r", "trainfile\\scalerule.range", //重载SVM缩放规则
                "-z", "trainfile\\test1scaled.txt",//存放缩放后测试数据的路径
                testFilename};//需要缩放的数据

        String[] parg = {   "-b", "1", "trainfile\\test1scaled.txt",  //这个是存放测试数据
                "trainfile\\model_r.txt", //调用的是训练以后的模型
                "trainfile\\out_r.txt"}; //生成的结果的文件的路径

        svm_predict p = new svm_predict();//创建一个预测或者分类的对象
        svm_scale s = new svm_scale();//创建一个缩放对象
        s.main(sarg2);
        List<Double> forecastNumber = new ArrayList<Double>();
        p.main(parg, forecastNumber); //forecast中下标偶数为每一行测试数据的预测结果编号，奇数为其概率
        locationResult(forecastNumber, locationFilename);
    }
    /**
     *@描述
     *@参数  [forecastNumber, locationFilename]
     *@返回值  void
     *@注意  forecastLocation为最终预测的所有位置，locationProbability为预测位置对应的概率
     *@创建人  Barry
     *@创建时间  2018/4/19
     *@修改人和其它信息
     */
    private void locationResult(List<Double> forecastNumber, String locationFilename) {

        File file = new File(locationFilename);
        String tempLine;
        String[] coordinate;
        List<String[]> locations = new ArrayList<String[]>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            while ((tempLine = bufferedReader.readLine()) != null) {
                coordinate = tempLine.split(" ");
                locations.add(coordinate);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String[]> forecastLocation = new ArrayList<String[]>();
        for (int i = 0; i < forecastNumber.size(); i=i+2) {
            forecastLocation.add(locations.get(forecastNumber.get(i).intValue() - 1));
        }
        List<Double> locationProbability = new ArrayList<Double>();
        for (int i = 1; i < forecastNumber.size(); i=i+2) {
            locationProbability.add(forecastNumber.get(i));
        }
        for (String[] temp : forecastLocation) {
            for (String tep : temp) {
                System.out.print(tep + " ");
            }
            System.out.println();
        }
    }
}

