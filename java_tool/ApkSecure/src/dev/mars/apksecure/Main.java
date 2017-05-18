package dev.mars.apksecure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * ��ԭAPK���ѿ�DEX�ϲ�������µ�apk
 * 
 * @author ma.xuanwei
 * 
 */
public class Main {
	private static final String DEX_APP_NAME = "dev.mars.secure.ProxyApplication";
	private static final String PASSWORD = "laChineestunlionendormi";
	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyyMMddHHmmss");
	private static Config config;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String cmd = args[0];
		if (!"b".equals(cmd)) {
			System.out.println("����Ĳ���:" + cmd);
			return;
		}
		// apk·��
		String apkPath = args[1];
		System.out.println("apkPath:" + apkPath);
		// ������Ŀ¼
		String decompiledDirName = apkPath.split("\\.")[0];
		System.out.println("decompiledDir:" + decompiledDirName);

		// ɾ��������Ŀ¼
		File decompiledFile = new File(getWorkPath() + "\\" + decompiledDirName);
		if (decompiledFile.exists()) {
			FileUtil.delete(decompiledFile);
			System.out.println("��ɾ��" + decompiledFile.getAbsolutePath());
		}

		// ����������Ŀ¼
		boolean decompiled = false;
		try {
			long startTime = System.currentTimeMillis();
			System.out.println("���ڷ�����" + apkPath);

			// ȷ��apktool.jar���ڹ���Ŀ¼��
			SystemCommand.execute("java -jar apktool.jar d " + apkPath);
			System.out.println("�������ʱ "
					+ (System.currentTimeMillis() - startTime) + " ms");
			System.out.println("���������,����Ŀ¼" + decompiledFile.getAbsolutePath());
			decompiled = true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (decompiled) {
			if (alterAndroidMainifest(decompiledFile.getAbsolutePath())) {

				// try {
				// buildAPK(decompiledDir, newPath);
				try {
					SystemCommand.execute("java -jar apktool.jar b "
							+ decompiledFile.getAbsolutePath());
					System.out.println("����ɹ�");
					String compiledApkPath = System.getProperty("user.dir")
							+ "\\" + decompiledDirName + "\\dist\\"
							+ decompiledDirName + ".apk";
					File compiledApkFile = new File(compiledApkPath);
					if (compiledApkFile.exists()) {
						System.out.println("�ҵ��޸�Manifest���apk:"
								+ compiledApkFile.getAbsolutePath());

						// ��ѹ�µ�apk�����ҵ�classes.dex�������ƶ���assets��
						String unzipFilePath = System.getProperty("user.dir")
								+ "\\" + decompiledDirName + "\\dist\\"
								+ decompiledDirName;
						FileUtil.delete(new File(unzipFilePath));
						ZipUtils.upzipFile(compiledApkFile, unzipFilePath);
						System.out.println("��ѹ��� ��ѹ���:" + unzipFilePath);
						
						File assetsFile = new File(unzipFilePath+"\\assets");
						if(!assetsFile.exists()){
							assetsFile.mkdir();
							System.out.println("����assets�ļ���");
						}
						// �ҵ�����.dex�ļ�
						List<File> allDexFiles = findAllDexFiles(unzipFilePath);
						String zipFilePath = unzipFilePath
								+ "\\assets\\abc"+System.currentTimeMillis()+".zip";
						// ������dex�ļ�ѹ����assets�µ�abc.zip
						ZipUtils.zipFile(zipFilePath, allDexFiles);
						System.out.println("����ѹ���ļ�:" + zipFilePath);
						
						DESUtils desUtils = new DESUtils();
						desUtils.initialize_encryptKey(PASSWORD);
						String outputPath = unzipFilePath + "\\assets\\apksecurefile";
						desUtils.encrypt(zipFilePath, outputPath);
						(new File(zipFilePath)).delete();
						System.out.println("���ɼ����ļ�:" + outputPath);
						outputPath.endsWith(suffix)
						// ɾ��ԭclasses.dex
						for (File f : allDexFiles) {
							f.delete();
						}

						String decladdingDexPath = System
								.getProperty("user.dir") + "\\classes.dex";
						String libsFolderPath = System.getProperty("user.dir")
								+ "\\secure-lib\\";
						String destLibsFolderPath = new File(unzipFilePath
								+ "\\lib\\").getAbsolutePath();
						String destDexPath = unzipFilePath+"\\classes.dex";
						if (copyDecladdingDexAndLibs(decladdingDexPath,
								destDexPath, libsFolderPath,
								destLibsFolderPath)) {
							String newAppPath = unzipFilePath + "\\"
									+ decompiledDirName + ".zip";
							packageApkFiles(unzipFilePath, newAppPath);
							File newZipFile = new File(newAppPath);
							if (newZipFile.exists()) {
								System.out.println("����ѹ���ļ�:" + newAppPath);
								File unsignedApkFile = new File(unzipFilePath
										+ "\\new-app.apk");
								newZipFile.renameTo(unsignedApkFile);
								System.out.println("���APP:"
										+ unsignedApkFile.getAbsolutePath());

								String signedApkPath = unzipFilePath + "\\"
										+ decompiledDirName + "_signed_"
										+ sdf.format(new Date()) + ".apk";

								signApk(unsignedApkFile.getAbsolutePath(),
										signedApkPath);

							} else {
								System.out.println("���appʧ��");
							}
						} else {
							System.out.println("���ƿ�DEX��soʧ��");
						}

					} else {
						System.out.println("δ�ҵ������ɵ�apk");
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("������ʧ��");
		}
	}

	/**
	 * ��ָ��·���ҵ�����dex�ļ�
	 * 
	 * @param unzipFilePath
	 * @return
	 * @throws IOException
	 */
	private static List<File> findAllDexFiles(String unzipFilePath)
			throws IOException {
		LinkedList<File> files = new LinkedList<>();
		File folderFile = new File(unzipFilePath);
		for (File f : folderFile.listFiles()) {
			if (f.getPath().endsWith(".dex")) {
				System.out.println("�ҵ�dex:" + f.getCanonicalPath());
				files.add(f);
			}
		}
		return files;
	}

	private static void packageApkFiles(String unzipFilePath, String newAppPath)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		String winRARPath = getConfig().winRARPath;
		String zipCommand = "cd " + unzipFilePath + " && \"" + winRARPath
				+ "\" a -r " + newAppPath + " ./*";
		System.out.println("cmd:" + zipCommand);
		SystemCommand.execute(zipCommand);
	}

	/**
	 * ִ�д˷���ȷ����jarsigner��·������ӵ�ϵͳ����������
	 * 
	 * @param unsignedApkPath
	 *            δǩ����apk��·��
	 * @param signedApkPath
	 *            ���ɵ�ǩ��apk��·��
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private static void signApk(String unsignedApkPath, String signedApkPath)
			throws IOException, InterruptedException {

		String signCommand = "jarsigner -verbose -keystore "
				+ getConfig().signaturePath
				+ " "
				+ "-storepass "
				+ getConfig().storePwd
				+ " -keypass "
				+ getConfig().aliasPwd
				+ " "
				+ "-sigfile CERT -digestalg SHA1 -sigalg MD5withRSA -signedjar "
				+ signedApkPath + " " + unsignedApkPath + " "
				+ getConfig().alias;
		System.out.println("cmd:" + signCommand);
		;
		SystemCommand.execute(signCommand);
		System.out.println("ǩ�����apk·��:" + signedApkPath);
	}

	private static boolean copyDecladdingDexAndLibs(String decladdingDexPath,
			String destPath, String libsFolderPath, String destLibsFolderPath) {
		// TODO Auto-generated method stub
		File dexFile = new File(decladdingDexPath);
		if (dexFile.exists()) {
			System.out.println("�ѿ�dex·��:" + decladdingDexPath);
			// ��ʼ�����ļ�
			try {
				FileInputStream fis = new FileInputStream(dexFile);
				FileOutputStream fos = new FileOutputStream(new File(destPath));
				byte[] buffer = new byte[1024];
				int readLength = 0;
				while (readLength != -1) {
					readLength = fis.read(buffer);
					if (readLength > 0) {
						fos.write(buffer, 0, readLength);
					}
				}
				fos.flush();
				fis.close();
				fos.close();
				System.out.println("��Dex�Ѹ��Ƶ�:" + destPath);

				System.out.println("��ʼ����libs,ԭʼ·��:" + libsFolderPath + " Ŀ��·��:"
						+ destLibsFolderPath);
				FileUtil.copyDir(libsFolderPath, destLibsFolderPath);
				return true;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			System.out.println("�ѿ�dexδ�ҵ�");
		}
		return false;
	}

	/**
	 * �޸�AndroidMinifest.xml�е�Application ClassΪ�ѿǵ�Application Class��
	 * ��Application��ǩ������ԭApplication Class��
	 * 
	 * @param workPath
	 */
	private static boolean alterAndroidMainifest(String workPath) {
		// TODO Auto-generated method stub
		String manifestFileName = "AndroidManifest.xml";
		File manifestFile = new File(workPath + "\\" + manifestFileName);
		if (!manifestFile.exists()) {
			System.err.println("�Ҳ���" + manifestFile.getAbsolutePath());
			return false;
		}
		SAXReader reader = new SAXReader();
		try {
			Document document = reader.read(manifestFile);
			Element root = document.getRootElement();

			System.out.println("��ǰ����:" + root.attribute("package").getText());
			Element applicationEle = root.element("application");
			System.out.println("����application��ǩ������:");
			Iterator<Attribute> attrIterator = applicationEle
					.attributeIterator();
			String APP_NAME = null;
			while (attrIterator.hasNext()) {
				Attribute attr = attrIterator.next();
				System.out.println(attr.getNamespacePrefix() + ":"
						+ attr.getName() + " = " + attr.getValue());
				if ("android".equals(attr.getNamespacePrefix())
						&& "name".equals(attr.getName())) {
					APP_NAME = attr.getValue();
					attr.setValue(DEX_APP_NAME);
					System.out.println("ԭapplication name:" + APP_NAME);
					System.out.println("��application name:" + attr.getValue());
				}
			}
			Element mataDataEle = applicationEle.addElement("meta-data");
			mataDataEle.addAttribute("android:name", "APP_NAME");
			mataDataEle.addAttribute("android:value", APP_NAME);

			manifestFile.delete();
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setEncoding("UTF-8");// ���ñ���
			Writer writer = new FileWriter(manifestFile.getAbsolutePath());
			XMLWriter outPut = new XMLWriter(writer, format);
			outPut.write(document);
			outPut.close();
			System.out.println("�޸�Manifest�ɹ�");
			return true;
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	private static Config getConfig() {
		if (config != null) {
			return config;
		}

		File signerConfigFile = new File(getWorkPath() + "\\" + "config.xml");
		if (!signerConfigFile.exists()) {
			System.err.println("�Ҳ���" + signerConfigFile.getAbsolutePath());
			return null;
		}
		// ��ȡXML
		SAXReader reader = new SAXReader();
		try {
			Document document = reader.read(signerConfigFile);
			Element root = document.getRootElement();
			Element signaturePathEle = root.element("signature-path");
			String signaturePath = signaturePathEle.getText();

			Element storePwdEle = root.element("store-pwd");
			String storePwd = storePwdEle.getText();

			Element aliasEle = root.element("alias");
			String alias = aliasEle.getText();

			Element aliasPwdEle = root.element("alias-pwd");
			String aliasPwd = aliasPwdEle.getText();

			Element winRARPathEle = root.element("winrar-path");
			String winRARPath = winRARPathEle.getText();

			System.out.println("signature-path:" + signaturePath
					+ " store-pwd:" + storePwd + " alias:" + alias
					+ " aliasPwd:" + aliasPwd + " winRARPath:" + winRARPath);
			config = new Config();
			config.signaturePath = signaturePath;
			config.storePwd = storePwd;
			config.alias = alias;
			config.aliasPwd = aliasPwd;
			config.winRARPath = winRARPath;
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return config;
	}

	private static String getWorkPath() {
		return System.getProperty("user.dir");
	}

	static class Config {
		public String signaturePath;
		public String storePwd;
		public String alias;
		public String aliasPwd;
		public String winRARPath;
	}
}
