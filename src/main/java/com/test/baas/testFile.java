package com.test.baas;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;

import com.google.common.io.Resources;

public class testFile {

	public static void main(String[] args) throws IOException, URISyntaxException {
		System.err.println(extractPemString(
				"D:/code/huawei/java/yxl-cloud-parent/yxl-blockchain/target/classes/config/b403cc6b0cec092aca6766dbde500eb41c4d35ee.peer/msp",
				"keystore"));

		System.err.println(extractPemString("https://github.com/jiangyang118/java-sdk-demo-1.4.0", "keystore"));
		System.err.println(
				Resources.toString(new URL("https://github.com/jiangyang118/java-sdk-demo-1.4.0/blob/master/README.md"),
						Charset.forName("UTF-8")));

		File f2 = Paths.get("src/main/resources", "log4j.properties").toFile();
		FileInputStream stream = new FileInputStream(f2);
		System.err.println(IOUtils.toString(stream, "UTF-8"));

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
