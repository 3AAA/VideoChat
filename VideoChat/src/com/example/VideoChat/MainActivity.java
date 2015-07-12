package com.example.VideoChat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by nicole on 15/7/10.
 */
public class MainActivity extends Activity {
    private ListView mOnlineFriends;
    private ArrayAdapter adapter;

    private ArrayList<String> list=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        mOnlineFriends= (ListView) findViewById(R.id.lv_friend_online);

        for (int i=0;i<10;i++){
            list.add("item"+(i+1));
        }
        /**
         * 服务端返回在线的用户，点击某一个用户，发送视频邀请
         *
         * 服务端查询被点击的用户是否online  是则推送邀请，否则提示发送方
         *
         * 此时发送方等待对方结果，进入等待视频聊天界面
         *
         * 如果对方同意视频邀请后，双方都进入视频聊天界面
         *
         * 如果对方拒绝，则提示拒绝视频邀请，返回到在线用户列表界面
         *
         * 中途网络问题 视频挂断
         *
         */

        adapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,list);

        mOnlineFriends.setAdapter(adapter);

        mOnlineFriends.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent=new Intent(MainActivity.this,LiveVideoActivity.class);
                startActivity(intent);
            }
        });
    }

}
