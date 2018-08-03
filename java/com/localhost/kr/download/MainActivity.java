package com.localhost.kr.download;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    static final int PERMISSION_REQUEST_CODE = 1;
    String[] PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE","android.permission.WRITE_EXTERNAL_STORAGE"};

    private File outputFile; //파일명까지 포함한 경로
    private File path;//디렉토리경로

    private boolean hasPermissions(String[] permissions) {

        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions){
            if (!(checkCallingOrSelfPermission(perms) == PackageManager.PERMISSION_GRANTED)){
                //퍼미션 허가 안된 경우
                return false;
            }
        }
        //퍼미션이 허가된 경우
        return true;
    }

    private void requestNecessaryPermissions(String[] permissions) {
        //마시멜로( API 23 )이상에서 런타임 퍼미션(Runtime Permission) 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!hasPermissions(PERMISSIONS)) { //퍼미션 허가를 했었는지 여부를 확인
            requestNecessaryPermissions(PERMISSIONS);//퍼미션 허가안되어 있다면 사용자에게 요청
        } else {
            //이미 사용자에게 퍼미션 허가를 받음.
        }

        initWidget();
    }

    private void initWidget(){

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) { //1
                //웹브라우저에 아래 링크를 입력하면 Alight.avi 파일이 다운로드됨.
                final String fileURL = "http://192.168.0.12:8080/JWeb/fileDown.do?fileName=filedb.db";

                path= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                //path = Environment.getExternalStorageDirectory();
                outputFile= new File(path, "filedb.db"); //파일명까지 포함함 경로의 File 객체 생성

                if (outputFile.exists()) { //이미 다운로드 되어 있는 경우

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("파일 다운로드");
                    builder.setMessage("이미 SD 카드에 존재합니다. 다시 다운로드 받을까요?");
                    builder.setNegativeButton("아니오",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    Toast.makeText(getApplicationContext(),"기존 파일을 플레이합니다.", Toast.LENGTH_LONG).show();
                                    //playVideo(outputFile.getPath());
                                    playImage(outputFile.getPath());
                                }
                            });
                    builder.setPositiveButton("예",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    outputFile.delete(); //파일 삭제

                                    final DownloadFilesTask downloadTask = new DownloadFilesTask(MainActivity.this);
                                    downloadTask.execute(fileURL);
                                }
                            });

                    builder.show();

                } else { //새로 다운로드 받는 경우
                    final DownloadFilesTask downloadTask = new DownloadFilesTask(MainActivity.this);
                    downloadTask.execute(fileURL);

                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch(permsRequestCode){

            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean readAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                        if ( !readAccepted || !writeAccepted  )
                        {
                            showDialogforPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                            return;
                        }
                    }
                }
                break;
        }
    }

    private void showDialogforPermission(String msg) {

        final AlertDialog.Builder myDialog = new AlertDialog.Builder( this );
        myDialog.setTitle("알림");
        myDialog.setMessage(msg);
        myDialog.setCancelable(false);
        myDialog.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, PERMISSION_REQUEST_CODE);
                }

            }
        });
        myDialog.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        myDialog.show();
    }

    private void playVideo(String path) {
        Uri videoUri = Uri.fromFile(new File(path));
        Intent videoIntent = new Intent(Intent.ACTION_VIEW);
        videoIntent.setDataAndType(videoUri, "video/*");
        if (videoIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(videoIntent, null));
        }
    }

    private void playImage(String path){

        //저장한 이미지 열기
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String targetDir = Environment.getExternalStorageDirectory().toString();

        File file = new File(path);

        //type 지정 (이미지)
        i.setDataAndType(Uri.fromFile(file), "image/*");
        startActivity(i);
    }

}
