# Compression App

## This included 3 functionalities
- Take a snapshot as a thumbnail
  ```
     //This portion of the function is used to take the snapshot of the video. The "myVideoThumbnail" can be used 
     //accordingly
     FFmpegMediaMetadataRetriever med = new FFmpegMediaMetadataRetriever(); med.setDataSource(file.getAbsolutePath());
     myVideoThumbnail = med.getFrameAtTime(FRAME_AT_NTH_SECOND*1000000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST);
  ```
- Add a watermark to the video
 ```
  //This function adds a water mark to video. I hava used a sample image for watermark.
  //There was no diirect method available. So this library uses console commands to execute the process.
  //Even if the add watermark crashed I have prceeded with the compression part
  addWaterMarkOnVideo()
 ```
- Compress the video
 ```
  //This is the compression class
  VideoCompressor
 ```
