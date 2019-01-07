package com.callke8.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.jfinal.plugin.activerecord.Record;

/**
 * Txt 文件工具
 * 	   主要用于读取上传的文件和写入文件
 * 
 * @author hwz
 *
 */
public class TxtUtils {

	/**
	 * 读取txt文件,并写入一个 Record
	 * 
	 * @param file
	 * @return
	 */
	public static List<Record> readTxt(File file) {
	
		List<Record> list = new ArrayList<Record>();   //定义一个 list
	
		BufferedReader reader = null;
		
		try {
			
			//指定读取文件的编码格式，要和写入的格式一致，以免出现中文乱码
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
			
			String str = null;
			
			while((str=reader.readLine())!=null) {
				String splitIdentify = ",";
				if(str.contains("|")) {
					splitIdentify = "\\|";
				}
				String[] strs = str.split(splitIdentify);
				
				Record record = new Record();
				for(int i=0;i<strs.length;i++) {
					record.set(String.valueOf(i), strs[i]);
				}
				
				list.add(record);
			}
			
		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e) {
			e.printStackTrace();
		}finally {
			try{
				reader.close();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		return list;
	}
	
	
}
