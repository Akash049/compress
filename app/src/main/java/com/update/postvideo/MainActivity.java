package com.update.postvideo;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.update.postvideo.videocompression.MediaController;

import java.io.File;
import java.net.URISyntaxException;

import wseemann.media.FFmpegMediaMetadataRetriever;


public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 1;
    private Bitmap myVideoThumbnail;
    private int FRAME_AT_NTH_SECOND = 10;
    private Button upload;
    private String filePath;
    private int widthOverlay = 20;

    //This will specify the margin from left. I am keeping it zero overlay will stick to left
    private int xPositionPercent = 0;

    //This will specify the margin from top. I am keeping it also zero as overlay will stick to top left
    private int yPositionPercent = 0;

    private String strFilter = "[1:v]scale=h=-1:w=" + widthOverlay + "[overlay_scaled],"
            + "[0:v][overlay_scaled]overlay=eval=init:x=W*" + xPositionPercent
            + ":y=H*" + yPositionPercent;


    private String[] сmd = new String[] {
            "-i",
            "strPathSrcVideo",
            "-itsoffset",
            String.valueOf(10),
            "-i",
            "strPathOverlay",
            "-filter_complex",
            strFilter,
            "-preset",
            "ultrafast",
            "-g",
            "120",
            "strPathDstVideo"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        upload= (Button) findViewById(R.id.upload);


        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Video"),REQUEST_TAKE_GALLERY_VIDEO);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_GALLERY_VIDEO && resultCode == RESULT_OK) {
            Uri videoUri = data.getData();
            try {
                filePath=getFilePath(MainActivity.this,videoUri);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            File file=new File(filePath);
            Log.e("before compression",file.getAbsolutePath()+"");

            //This portion prepares the thumbnail for the video created
            //This can be stored or used somewhere
            FFmpegMediaMetadataRetriever med = new FFmpegMediaMetadataRetriever(); med.setDataSource(file.getAbsolutePath());
            myVideoThumbnail = med.getFrameAtTime(FRAME_AT_NTH_SECOND*1000000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);

            //After the Thumbnail has been extracted the video is set for watermark addition
            addWaterMarkOnVideo();
        }
    }

    public void addWaterMarkOnVideo(){
        FFmpeg ffmpeg = FFmpeg.getInstance(getApplicationContext());

        //Setting the file path
        сmd[1] = (new File(filePath)).getAbsolutePath();

        //Setting the path of the overlay
        Uri overlayPath = Uri.parse("android.resource://com.update.videocompression/drawable/watermark.png");
        сmd[5] = overlayPath.getPath();

        //The path for the final video also I am setting the same assuming the video will be saved at same point
        сmd[12] = (new File(filePath)).getAbsolutePath();

        try {

            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    Toast.makeText(getApplicationContext(),"INITIATED",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProgress(String message) {}

                @Override
                public void onFailure(String message) {
                    Toast.makeText(getApplicationContext(),"COMPRESSING_WITHOUT_WATERMARK",Toast.LENGTH_SHORT).show();
                    new VideoCompressor().execute();
                }

                @Override
                public void onSuccess(String message) {
                    Toast.makeText(getApplicationContext(),"COMPRESSING_WITH_WATERMARK",Toast.LENGTH_SHORT).show();
                    new VideoCompressor().execute();
                }

                @Override
                public void onFinish() {
                    Toast.makeText(getApplicationContext(),"COMPLETED_WATERMARK_ADDITION",Toast.LENGTH_SHORT).show();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            Toast.makeText(getApplicationContext(),"COMPRESSING_WITHOUT_WATERMARK",Toast.LENGTH_SHORT).show();
            new VideoCompressor().execute();
        }
    }

    //This class will compress the video and return the compressed video path
    private class VideoCompressor extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return MediaController.getInstance().convertVideo(filePath);
        }

        @Override
        protected void onPostExecute(Boolean compressed) {
            super.onPostExecute(compressed);
            if (compressed) {
                Log.e("Compression", "Compression successfully!");
               Log.e("Compressed File Path", "" + MediaController.cachedFile.getPath());
                Toast.makeText(getApplicationContext(),"Compressed Path"+MediaController.cachedFile.getPath(),Toast.LENGTH_SHORT).show();
            }

        }
    }
    @SuppressLint("NewApi")
    public static String getFilePath(Context context, Uri uri) throws URISyntaxException {
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
