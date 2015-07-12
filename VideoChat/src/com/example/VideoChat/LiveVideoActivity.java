package com.example.VideoChat;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.*;
import android.widget.Button;
import android.widget.Toast;
import com.bairuitech.anychat.AnyChatBaseEvent;
import com.bairuitech.anychat.AnyChatCoreSDK;
import com.bairuitech.anychat.AnyChatDefine;
import com.bairuitech.demo.ConfigEntity;
import com.bairuitech.demo.ConfigService;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LiveVideoActivity extends Activity implements AnyChatBaseEvent {
    private SurfaceView mLocalSurfaceView;
    private SurfaceView mRemoteSurfaceView;
    private Button mBack;
    private Camera camera;
    private boolean isPreview;
    private Handler handler;
    private ConfigEntity entity;
    private TimerTask mTimerTask;
    private Timer mTimer = new Timer();
    private List<String> userList = new ArrayList<>();

    private int userId;

    private AnyChatCoreSDK anyChat;//AnyChat核心SDK
    private boolean mSelfVideoOpen = false;//本地视频是否打开
    private boolean mRemoteVideoOpen = false;//对方视频是否打开

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Window window = getWindow();//得到窗口
//        requestWindowFeature(Window.FEATURE_NO_TITLE);//没有标题
//        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//设置高亮
        setContentView(R.layout.live_video_layout);


        mLocalSurfaceView = (SurfaceView) findViewById(R.id.sv_mine_frame);
        mRemoteSurfaceView = (SurfaceView) findViewById(R.id.sv_other_frame);
        mBack = (Button) findViewById(R.id.btn_back);
        mLocalSurfaceView.getHolder().addCallback(new SurfaceCallBack());
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(LiveVideoActivity.this, "视频聊天将结束", Toast.LENGTH_SHORT).show();
                LiveVideoActivity.this.finish();
            }
        });

        entity = ConfigService.LoadConfig(this);

        loginSystem();

        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                handler.sendMessage(message);
            }
        };
        mTimer.schedule(mTimerTask, 1000, 100);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                VideoChat();
                super.handleMessage(msg);
            }
        };


    }

    private void VideoChat() {
        if (!mRemoteVideoOpen) {
            if (anyChat.GetCameraState(userId) == 2 && anyChat.GetUserVideoWidth(userId) == 0) {
                SurfaceHolder holder = mRemoteSurfaceView.getHolder();
                holder.setFormat(PixelFormat.RGB_565);
                holder.setFixedSize(anyChat.GetUserVideoWidth(userId), anyChat.GetUserVideoHeight(userId));
                Surface s = holder.getSurface();
                anyChat.SetVideoPos(userId, s, 0, 0, 0, 0);
                mRemoteVideoOpen = true;
            }
        }
        if (!mSelfVideoOpen) {
            if (anyChat.GetCameraState(-1) == 2 && anyChat.GetUserVideoWidth(-1) == 0) {
                SurfaceHolder holder = mLocalSurfaceView.getHolder();
                holder.setFormat(PixelFormat.RGB_565);
                holder.setFixedSize(anyChat.GetUserVideoWidth(-1), anyChat.GetUserVideoHeight(-1));
                Surface s = holder.getSurface();
                anyChat.SetVideoPos(-1, s, 0, 0, 0, 0);
                mSelfVideoOpen = true;
            }
        }
    }

    private void loginSystem() {
        if (anyChat == null) {
            anyChat = new AnyChatCoreSDK();
            anyChat.SetBaseEvent(this);
            if (entity.useARMv6Lib != 0)// 使用ARMv6指令集
                anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_CORESDK_USEARMV6LIB, 1);
            anyChat.InitSDK(Build.VERSION.SDK_INT, 0);//初始化sdk
        }
        anyChat.Connect("demo.anychat.cn", 8906);//连接服务器
    }

    @Override
    public void OnAnyChatConnectMessage(boolean bSuccess) {

        if (!bSuccess) {
            Toast.makeText(this, "服务器连接失败，自动重连，请稍后。。。", Toast.LENGTH_SHORT).show();
        }
        anyChat.Login("android", "nicole");
    }

    @Override
    public void OnAnyChatLoginMessage(int dwUserId, int dwErrorCode) {
        if (dwErrorCode == 0) {
            Toast.makeText(this, "登陆成功！", Toast.LENGTH_SHORT).show();
            anyChat.EnterRoom(1, "");
            ApplyVideoConfig();
        } else {
            Toast.makeText(this, "登陆失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void OnAnyChatEnterRoomMessage(int dwRoomId, int dwErrorCode) {
        if (dwErrorCode == 0) {                     // 进入房间成功打开本地音视频
            Toast.makeText(this, "进入房间成功", Toast.LENGTH_SHORT).show();
            anyChat.UserCameraControl(-1, 1);       // 打开本地视频
            anyChat.UserSpeakControl(-1, 1);      // 打开本地音频

        } else {
            Toast.makeText(this, "进入房间失败，错误代码：" + dwErrorCode, Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void OnAnyChatOnlineUserMessage(int dwUserNum, int dwRoomId) {
        if (dwRoomId == 1) {
            int user[] = anyChat.GetOnlineUser();
            if (user.length != 0) {
                for (int i = 0; i < user.length; i++) {
                    userList.add(user + "");
                }
                String temp = userList.get(0);
                userId = Integer.parseInt(temp);
                anyChat.UserCameraControl(userId, 1);// 请求用户视频
                anyChat.UserSpeakControl(userId, 1); // 请求用户音频
            } else {
                Toast.makeText(this, "当前没有在线用户", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void OnAnyChatUserAtRoomMessage(int dwUserId, boolean bEnter) {
        if (bEnter) {//新用户进入房间
            userList.add(dwUserId + "");
        } else {       //用户离开房间
            if (dwUserId == userId) {
                Toast.makeText(this, "视频用户已下线", Toast.LENGTH_SHORT).show();
                anyChat.UserCameraControl(userId, 0);// 关闭用户视频
                anyChat.UserSpeakControl(userId, 0); // 关闭用户音频
                userList.remove(userId + "");          //移除该用户
                if (userList.size() != 0) {
                    String temp = userList.get(0);
                    userId = Integer.parseInt(temp);
                    anyChat.UserCameraControl(userId, 1);// 请求其他用户视频
                    anyChat.UserSpeakControl(userId, 1); // 请求其他用户音频
                }
            } else {
                userList.remove(dwUserId + "");    //移除该用户
            }
        }
    }

    @Override
    public void OnAnyChatLinkCloseMessage(int dwErrorCode) {
        Toast.makeText(this, "连接关闭，error：" + dwErrorCode, Toast.LENGTH_SHORT).show();
    }

    private class SurfaceCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            try {
                Camera.CameraInfo info = new Camera.CameraInfo();
                for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                    Camera.getCameraInfo(i, info);
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        camera = Camera.open(i);
                    }
                }
                camera.setPreviewDisplay(mLocalSurfaceView.getHolder());
                camera.startPreview();
                isPreview = true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            if (camera != null) {
                if (isPreview) {
                    camera.stopPreview();
                    camera.release();
                }
            }
        }
    }

    private void ApplyVideoConfig() {
        if (entity.configMode == 1) // 自定义视频参数配置
        {
            // 设置本地视频编码的码率（如果码率为0，则表示使用质量优先模式）
            anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_BITRATECTRL, entity.videoBitrate);
            if (entity.videoBitrate == 0) {
                // 设置本地视频编码的质量
                anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_QUALITYCTRL, entity.videoQuality);
            }
            // 设置本地视频编码的帧率
            anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_FPSCTRL, entity.videoFps);
            // 设置本地视频编码的关键帧间隔
            anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_GOPCTRL, entity.videoFps * 4);
            // 设置本地视频采集分辨率
            anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_WIDTHCTRL, entity.resolution_width);
            anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_HEIGHTCTRL, entity.resolution_height);
            // 设置视频编码预设参数（值越大，编码质量越高，占用CPU资源也会越高）
            anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_PRESETCTRL, entity.videoPreset);
        }
        // 让视频参数生效
        anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_APPLYPARAM, entity.configMode);
        // P2P设置
        anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_NETWORK_P2PPOLITIC, entity.enableP2P);
        // 本地视频Overlay模式设置
        anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_OVERLAY, entity.videoOverlay);
        // 回音消除设置
        anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_AUDIO_ECHOCTRL, entity.enableAEC);
        // 平台硬件编码设置
        anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_CORESDK_USEHWCODEC, entity.useHWCodec);
        // 视频旋转模式设置
        anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_ROTATECTRL, entity.videorotatemode);
        // 视频采集驱动设置
        anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER, entity.videoCapDriver);
        // 本地视频采集偏色修正设置
        anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_FIXCOLORDEVIA, entity.fixcolordeviation);
        // 视频显示驱动设置
        anyChat.SetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL, entity.videoShowDriver);
    }


}
