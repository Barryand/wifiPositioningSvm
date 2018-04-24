package service;

import libsvm.*;
import java.io.*;
import java.util.*;

public class svm_predict {
	private static svm_print_interface svm_print_null = new svm_print_interface()//使用匿名类直接new接口
	{
		public void print(String s) {}
	};

	private static svm_print_interface svm_print_stdout = new svm_print_interface()
	{
		public void print(String s)
		{
			System.out.print(s);
		}
	};

	private static svm_print_interface svm_print_string = svm_print_stdout;

	static void info(String s)
	{
		svm_print_string.print(s);
	}

	private static double atof(String s)
	{
		return Double.valueOf(s).doubleValue();
	}

	private static int atoi(String s)
	{
		return Integer.parseInt(s);
	}

	private static void predict(StringBuffer scaledUserData, svm_model model, int predict_probability, List<Double> result) throws IOException
	{
		int correct = 0;
		int total = 0;
		double error = 0;
		double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;

		int svm_type=svm.svm_get_svm_type(model);
		int nr_class=svm.svm_get_nr_class(model);
		double[] prob_estimates=null;

		if(predict_probability == 1)
		{
			if(svm_type == svm_parameter.EPSILON_SVR ||
					svm_type == svm_parameter.NU_SVR)
			{
				svm_predict.info("Prob. model for test data: target value = predicted value + z,\nz: Laplace distribution e^(-|z|/sigma)/(2sigma),sigma="+svm.svm_get_svr_probability(model)+"\n");
			}
			else
			{
				int[] labels=new int[nr_class];
				svm.svm_get_labels(model,labels);
				prob_estimates = new double[nr_class];
			}
		}

		String line = scaledUserData.toString();

		StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

		double target = atof(st.nextToken());
		int m = st.countTokens()/2;
		svm_node[] x = new svm_node[m];
		for(int j=0;j<m;j++)
		{
			x[j] = new svm_node();
			x[j].index = atoi(st.nextToken());
			x[j].value = atof(st.nextToken());
		}

		double v, p;
		int prob_max_idx;
		if (predict_probability==1 && (svm_type==svm_parameter.C_SVC || svm_type==svm_parameter.NU_SVC))
		{
/**
*@描述
*@参数  [input, output, model, predict_probability, result]
*@返回值  void
*@注意   下面这小段是修改的部分
*@创建人  Barry
*@创建时间  2018/4/18
*@修改人和其它信息
*/
			prob_max_idx = (int)svm.svm_predict_probability(model,x,prob_estimates);
			v = model.label[prob_max_idx];
			p = prob_estimates[prob_max_idx];
			result.add(v);
			result.add(p);


		}
		else
		{
			v = svm.svm_predict(model,x);
			result.add(v);
		}

		if(v == target)
			++correct;
		error += (v-target)*(v-target);
		sumv += v;
		sumy += target;
		sumvv += v*v;
		sumyy += target*target;
		sumvy += v*target;
		++total;

		if(svm_type == svm_parameter.EPSILON_SVR ||
				svm_type == svm_parameter.NU_SVR)
		{
			svm_predict.info("Mean squared error = "+error/total+" (regression)\n");
			svm_predict.info("Squared correlation coefficient = "+
					((total*sumvy-sumv*sumy)*(total*sumvy-sumv*sumy))/
							((total*sumvv-sumv*sumv)*(total*sumyy-sumy*sumy))+
					" (regression)\n");
		}
		else
			svm_predict.info("Accuracy = "+(double)correct/total*100+
					"% ("+correct+"/"+total+") (classification)\n");
	}

	private static void exit_with_help()
	{
		System.err.print("usage: svm_predict [options] test_file model_file output_file\n"
				+"options:\n"
				+"-b probability_estimates: whether to predict probability estimates, 0 or 1 (default 0); one-class SVM not supported yet\n"
				+"-q : quiet mode (no outputs)\n");
		System.exit(1);
	}

	public static void main(String argv[], StringBuffer scaledUserData, List<Double> result) throws IOException
	{
		int i, predict_probability=0;
		svm_print_string = svm_print_stdout;

		// parse options
		for(i=0;i<argv.length;i++)
		{
			if(argv[i].charAt(0) != '-') break;
			++i;
			switch(argv[i-1].charAt(1))
			{
				case 'b':
					predict_probability = atoi(argv[i]);
					break;
				case 'q':
					svm_print_string = svm_print_null;
					i--;
					break;
				default:
					System.err.print("Unknown option: " + argv[i-1] + "\n");
					exit_with_help();
			}
		}
		if(i>=argv.length)
			exit_with_help();
		try
		{
			svm_model model = svm.svm_load_model(argv[i]);
			if (model == null)
			{
				System.err.print("can't open model file "+argv[i+1]+"\n");
				System.exit(1);
			}
			if(predict_probability == 1)
			{
				if(svm.svm_check_probability_model(model)==0)
				{
					System.err.print("Model does not support probabiliy estimates\n");
					System.exit(1);
				}
			}
			else
			{
				if(svm.svm_check_probability_model(model)!=0)
				{
					svm_predict.info("Model supports probability estimates, but disabled in prediction.\n");
				}
			}
			predict(scaledUserData,model,predict_probability,result);
		}
		catch(FileNotFoundException e)
		{
			exit_with_help();
		}
		catch(ArrayIndexOutOfBoundsException e)
		{
			exit_with_help();
		}
	}
}
