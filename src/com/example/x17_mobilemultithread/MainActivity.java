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
	// 确定下载地址
	String path = "http://192.168.1.101:8080/PowerWord.exe";
	// 进度条当前进度
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

		// 取到进度条
		pb = (ProgressBar) findViewById(R.id.pb);
		// 取到进度显示文本框
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
						// 拿到请求的文件资源的长度
						int length = conn.getContentLength();

						// 设置进度条的最大值
						pb.setMax(length);

						// 写到sd卡，应先判断sd卡是否可用（详见项目x03_reinrom）
						File file = new File(
								Environment.getExternalStorageDirectory(),
								getFileName(path));
						// 生成临时文件
						RandomAccessFile raf = new RandomAccessFile(file, "rwd");
						// 设置临时文件大小
						raf.setLength(length);
						raf.close();
						int size = length / threadCount;
						// 计算出每个线程应该下载多少字节
						for (int i = 0; i < threadCount; i++) {
							// 计算线程开始和结束的位置
							int startIndex = i * size;
							int endIndex = (i + 1) * size - 1;

							// 如果是最后一个线程，结束位置写死
							if (i == threadCount - 1) {
								endIndex = length - 1;
							}
							System.out.println("线程" + i + "的区间是：" + startIndex
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
				// 判断进度临时文件是否存在
				if (progressFile.exists()) {
					FileInputStream fis = new FileInputStream(progressFile);
					BufferedReader br = new BufferedReader(
							new InputStreamReader(fis));
					// 读取上一次文件的进度，计算出新的开始位置
					int lastProgress = Integer.parseInt(br.readLine());
					startIndex += lastProgress;

					// 把上次下载的进度加载到进度条
					currentProgress += lastProgress;
					pb.setProgress(currentProgress);

					// 发送消息，让主线程刷新UI（进度文本框）
					handler.sendEmptyMessage(1);

					fis.close();
				}

				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(5000);
				conn.setReadTimeout(5000);

				// 设置本次Http请求的请求的数据区间
				conn.setRequestProperty("Range", "bytes=" + startIndex + "-"
						+ endIndex);

				// 请求部分数据的请求成功的响应码是206
				if (conn.getResponseCode() == 206) {
					InputStream is = conn.getInputStream();
					byte[] b = new byte[1024];
					int len = 0;
					int total = 0;
					// 拿到临时文件的输出流
					File file = new File(
							Environment.getExternalStorageDirectory(),
							getFileName(path));
					RandomAccessFile raf = new RandomAccessFile(file, "rwd");
					// 把文件的写入位置移动至startIndex
					raf.seek(startIndex);
					while ((len = is.read(b)) != -1) {
						// 每次读取流里数据之后，同步把数据写入临时文件
						raf.write(b, 0, len);
						total += len;

						System.out.println("线程" + threadId + "下载了" + total
								+ "字节");

						// 设置进度条当前进度
						currentProgress += len;
						pb.setProgress(currentProgress);

						// 发送消息，让主线程刷新UI（进度文本框）
						handler.sendEmptyMessage(1);

						// 生成一个专门用来记录下载进度的临时文件
						RandomAccessFile progressRaf = new RandomAccessFile(
								progressFile, "rwd");
						// 每次读取流里的数据后，同步把当前进程下载的总进度写进进度文件中
						progressRaf.write((total + "").getBytes());
						progressRaf.close();
					}
					System.out.println("线程" + threadId + "下载完毕~~~~~~~~~~~~~~~");
					raf.close();

					finishedThread++;

					if (finishedThread == threadCount) {
						// 把进度文本框强制显示为100%（防止99%现象发生）
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
