package com.example.x17_mobilemultithread;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {

	int threadCount = 3;
	int finishedThread = 0;
	// ȷ�����ص�ַ
	String path = "http://192.168.1.101:8080/PowerWord.exe";
	// ��������ǰ����
	int currentProgress;

	private ProgressBar pb;
	private TextView tv;

	Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			tv.setText((long) pb.getProgress() * 100 / pb.getMax() + "%");
		};
	};

	public String getFileName(String path) {
		int index = path.lastIndexOf("/");
		return path.substring(index + 1);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// ȡ��������
		pb = (ProgressBar) findViewById(R.id.pb);
		// ȡ��������ʾ�ı���
		tv = (TextView) findViewById(R.id.tv);
	}

	public void click(View v) {
		Thread t = new Thread() {
			@Override
			public void run() {

				// TODO Auto-generated method stub

				try {
					URL url = new URL(path);

					HttpURLConnection conn = (HttpURLConnection) url
							.openConnection();
					conn.setRequestMethod("GET");
					conn.setConnectTimeout(5000);
					conn.setReadTimeout(5000);

					if (conn.getResponseCode() == 200) {
						// �õ�������ļ���Դ�ĳ���
						int length = conn.getContentLength();

						// ���ý����������ֵ
						pb.setMax(length);

						// д��sd����Ӧ���ж�sd���Ƿ���ã������Ŀx03_reinrom��
						File file = new File(
								Environment.getExternalStorageDirectory(),
								getFileName(path));
						// ������ʱ�ļ�
						RandomAccessFile raf = new RandomAccessFile(file, "rwd");
						// ������ʱ�ļ���С
						raf.setLength(length);
						raf.close();
						int size = length / threadCount;
						// �����ÿ���߳�Ӧ�����ض����ֽ�
						for (int i = 0; i < threadCount; i++) {
							// �����߳̿�ʼ�ͽ�����λ��
							int startIndex = i * size;
							int endIndex = (i + 1) * size - 1;

							// ��������һ���̣߳�����λ��д��
							if (i == threadCount - 1) {
								endIndex = length - 1;
							}
							System.out.println("�߳�" + i + "�������ǣ�" + startIndex
									+ "~" + endIndex);
							new DownloadThread(startIndex, endIndex, i).start();

						}
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		};
		t.start();

	}

	class DownloadThread extends Thread {
		int startIndex;
		int endIndex;
		int threadId;

		public DownloadThread() {

		}

		public DownloadThread(int startIndex, int endIndex, int threadId) {
			super();
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.threadId = threadId;
		}

		public void run() {
			try {
				File progressFile = new File(
						Environment.getExternalStorageDirectory(), threadId
								+ ".txt");
				// �жϽ�����ʱ�ļ��Ƿ����
				if (progressFile.exists()) {
					FileInputStream fis = new FileInputStream(progressFile);
					BufferedReader br = new BufferedReader(
							new InputStreamReader(fis));
					// ��ȡ��һ���ļ��Ľ��ȣ�������µĿ�ʼλ��
					int lastProgress = Integer.parseInt(br.readLine());
					startIndex += lastProgress;

					// ���ϴ����صĽ��ȼ��ص�������
					currentProgress += lastProgress;
					pb.setProgress(currentProgress);

					// ������Ϣ�������߳�ˢ��UI�������ı���
					handler.sendEmptyMessage(1);

					fis.close();
				}

				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(5000);
				conn.setReadTimeout(5000);

				// ���ñ���Http������������������
				conn.setRequestProperty("Range", "bytes=" + startIndex + "-"
						+ endIndex);

				// ���󲿷����ݵ�����ɹ�����Ӧ����206
				if (conn.getResponseCode() == 206) {
					InputStream is = conn.getInputStream();
					byte[] b = new byte[1024];
					int len = 0;
					int total = 0;
					// �õ���ʱ�ļ��������
					File file = new File(
							Environment.getExternalStorageDirectory(),
							getFileName(path));
					RandomAccessFile raf = new RandomAccessFile(file, "rwd");
					// ���ļ���д��λ���ƶ���startIndex
					raf.seek(startIndex);
					while ((len = is.read(b)) != -1) {
						// ÿ�ζ�ȡ��������֮��ͬ��������д����ʱ�ļ�
						raf.write(b, 0, len);
						total += len;

						System.out.println("�߳�" + threadId + "������" + total
								+ "�ֽ�");

						// ���ý�������ǰ����
						currentProgress += len;
						pb.setProgress(currentProgress);

						// ������Ϣ�������߳�ˢ��UI�������ı���
						handler.sendEmptyMessage(1);

						// ����һ��ר��������¼���ؽ��ȵ���ʱ�ļ�
						RandomAccessFile progressRaf = new RandomAccessFile(
								progressFile, "rwd");
						// ÿ�ζ�ȡ��������ݺ�ͬ���ѵ�ǰ�������ص��ܽ���д�������ļ���
						progressRaf.write((total + "").getBytes());
						progressRaf.close();
					}
					System.out.println("�߳�" + threadId + "�������~~~~~~~~~~~~~~~");
					raf.close();

					finishedThread++;

					if (finishedThread == threadCount) {
						// �ѽ����ı���ǿ����ʾΪ100%����ֹ99%��������
					//	tv.setText("100%");
						for (int i = 0; i < threadCount; i++) {
							File f = new File(
									Environment.getExternalStorageDirectory(),
									i + ".txt");
							f.delete();
						}
						finishedThread = 0;
					}
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

}
