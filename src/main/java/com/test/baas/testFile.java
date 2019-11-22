package com.test.baas;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;

public class testFile {

	public static void main(String[] args) {
		System.err.println(extractPemString(
				"D:/code/huawei/java/yxl-cloud-parent/yxl-blockchain/target/classes/config/b403cc6b0cec092aca6766dbde500eb41c4d35ee.peer/msp",
				"keystore"));
	}

	public static String extractPemString(String path, String sub) {
		String pemString = "";
		File dir = new File(path + "/" + sub);
		if (!dir.exists()) {
			System.err.println("directory is not exist. path:" + dir);
			return "";
		}

		for (File f : dir.listFiles()) {
			try {
				FileInputStream stream = new FileInputStream(f);
				pemString = IOUtils.toString(stream, "UTF-8");
				return pemString;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return pemString;
	}
}
