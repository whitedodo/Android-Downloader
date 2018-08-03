package com.localhost.kr.download;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by lab on 2017-11-26.
 */

public class DownloadFilesTask extends AsyncTask<String, String, Long> {

    private Context context;
    private PowerManager.WakeLock mWakeLock;
    private ProgressDialog progressBar;

    private File outputFile; //파일명까지 포함한 경로
    private File path;//디렉토리경로

    public DownloadFilesTask(Context context) {
        this.context = context;

        progressBar = new ProgressDialog(context);
        progressBar.setMessage("다운로드중");
        progressBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressBar.setIndeterminate(true);
        progressBar.setCancelable(true);

        progressBar.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancel(true);
            }
        });

    }

    //파일 다운로드를 시작하기 전에 프로그레스바를 화면에 보여줍니다.
    @Override
    protected void onPreExecute() { //2
        super.onPreExecute();

        //사용자가 다운로드 중 파워 버튼을 누르더라도 CPU가 잠들지 않도록 해서
        //다시 파워버튼 누르면 그동안 다운로드가 진행되고 있게 됩니다.
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.acquire();

        progressBar.show();
    }

    //파일 다운로드를 진행합니다.
    @Override
    protected Long doInBackground(String... string_url) { //3

        int count;
        long FileSize = -1;

        InputStream input = null;
        OutputStream output = null;
        URLConnection connection = null;

        try {
            URL url = new URL(string_url[0]);
            connection = url.openConnection();
            connection.connect();


            //파일 크기를 가져옴
            FileSize = connection.getContentLength();

            //URL 주소로부터 파일다운로드하기 위한 input stream
            input = new BufferedInputStream(url.openStream(), 8192);

            path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            outputFile= new File(path, "filedb.db"); //파일명까지 포함함 경로의 File 객체 생성

            // SD카드에 저장하기 위한 Output stream
            output = new FileOutputStream(outputFile);


            byte data[] = new byte[1024];
            long downloadedSize = 0;
            while ((count = input.read(data)) != -1) {
                //사용자가 BACK 버튼 누르면 취소가능
                if (isCancelled()) {
                    input.close();
                    return Long.valueOf(-1);
                }

                downloadedSize += count;

                if (FileSize > 0) {
                    float per = ((float)downloadedSize/FileSize) * 100;
                    String str = "Downloaded " + downloadedSize + "KB / " + FileSize + "KB (" + (int)per + "%)";
                    publishProgress("" + (int) ((downloadedSize * 100) / FileSize), str);

                }

                //파일에 데이터를 기록합니다.
                output.write(data, 0, count);
            }
            // Flush output
            output.flush();

            // Close streams
            output.close();
            input.close();


        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            mWakeLock.release();

        }
        return FileSize;
    }


    //다운로드 중 프로그레스바 업데이트
    @Override
    protected void onProgressUpdate(String... progress) { //4
        super.onProgressUpdate(progress);

        // if we get here, length is known, now set indeterminate to false
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        progressBar.setProgress(Integer.parseInt(progress[0]));
        progressBar.setMessage(progress[1]);
    }

    //파일 다운로드 완료 후
    @Override
    protected void onPostExecute(Long size) { //5
        super.onPostExecute(size);

        progressBar.dismiss();

        if ( size > 0) {
            Toast.makeText(context, "다운로드 완료되었습니다. 파일 크기=" + size.toString(), Toast.LENGTH_LONG).show();

            Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(outputFile));
            context.sendBroadcast(mediaScanIntent);

            //playVideo(outputFile.getPath());

        }
        else
            Toast.makeText( context , "다운로드 에러", Toast.LENGTH_LONG).show();
    }

}