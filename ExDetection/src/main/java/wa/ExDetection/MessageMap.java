package wa.ExDetection;

import java.io.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;

import org.slf4j.LoggerFactory;
import utils.PrintException;

class MessageMap{
	private static org.slf4j.Logger logger = LoggerFactory.getLogger(MessageMap.class);
	
	public JSONObject map_ = null; 
	
	private static final String DATA_PATH_STRING = "MessageMap.json";
	
	public MessageMap() {
		// TODO Auto-generated constructor stub

		InputStream in = null;
		try {
			in = new FileInputStream(DATA_PATH_STRING);
		} catch (FileNotFoundException e) {
			PrintException.Print(logger,e);
		}

		String jsonString = null;
		try {
			jsonString = IOUtils.toString(in);
		} catch (IOException e) {
			PrintException.Print(logger,e);
		}
		try{
			map_ = JSON.parseObject(jsonString);
		}catch (Exception e){
			PrintException.Print(logger,e,"读取json文件出错");
		}

	}	
}