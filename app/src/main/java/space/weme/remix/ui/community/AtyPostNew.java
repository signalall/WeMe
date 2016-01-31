package space.weme.remix.ui.community;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import me.nereo.multi_image_selector.MultiImageSelectorActivity;
import okhttp3.MediaType;
import space.weme.remix.R;
import space.weme.remix.ui.base.BaseActivity;
import space.weme.remix.util.LogUtils;
import space.weme.remix.util.OkHttpUtils;
import space.weme.remix.util.StrUtils;
import space.weme.remix.widgt.GridLayout;

/**
 * Created by Liujilong on 2016/1/31.
 * liujilong.me@gmail.com
 */
public class AtyPostNew extends BaseActivity {
    private static final String TAG = "AtyPostNew";
    public static final String INTENT_ID = "topicID";
    private static final int MAX_PICTURE = 9;
    private static final int REQUEST_IMAGE = 0xad;
    private static final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/*");

    private String mTopicID;
    ArrayList<String> mChosenPicturePathList;
    private AtomicInteger mSendImageResponseNum;

    private EditText mTitle;
    private EditText mContent;
    private SimpleDraweeView mDrawAddImage;
    private GridLayout mImageGrids;
    ProgressDialog mProgressDialog;

    private View.OnClickListener mListener;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTopicID = getIntent().getStringExtra(INTENT_ID);
        setContentView(R.layout.aty_post_new);

        mListener = new ImageListener();
        View.OnClickListener mPostListener = new PostListener();
        mImageGrids = (GridLayout) findViewById(R.id.aty_post_new_images);
        TextView mSend = (TextView) findViewById(R.id.aty_post_new_send);
        mTitle = (EditText) findViewById(R.id.aty_post_new_title);
        mContent = (EditText) findViewById(R.id.aty_post_new_content);
        mDrawAddImage = (SimpleDraweeView) findViewById(R.id.aty_post_new_add_image);
        mDrawAddImage.setImageURI(Uri.parse("res:/" + R.mipmap.add_image));
        mDrawAddImage.setOnClickListener(mListener);

        mSend.setOnClickListener(mPostListener);
        mChosenPicturePathList = new ArrayList<>();
        mSendImageResponseNum = new AtomicInteger();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK){
            List<String> paths=data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);
            mChosenPicturePathList.clear();
            mChosenPicturePathList.addAll(paths);
            mImageGrids.removeAllViews();
            for(String path : mChosenPicturePathList){
                SimpleDraweeView image = new SimpleDraweeView(AtyPostNew.this);
                image.setImageURI(Uri.parse("file://"+path));
                mImageGrids.addView(image);
                image.setOnClickListener(mListener);
            }
            if(mImageGrids.getChildCount()<9){
                mImageGrids.addView(mDrawAddImage);
            }
        }
    }

    @Override
    protected String tag() {
        return TAG;
    }

    private class ImageListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(AtyPostNew.this, MultiImageSelectorActivity.class);
            intent.putExtra(MultiImageSelectorActivity.EXTRA_SHOW_CAMERA, true);
            // 最大可选择图片数量
            intent.putExtra(MultiImageSelectorActivity.EXTRA_SELECT_COUNT, MAX_PICTURE);
            // 选择模式
            intent.putExtra(MultiImageSelectorActivity.EXTRA_SELECT_MODE, MultiImageSelectorActivity.MODE_MULTI);
            // 默认选择
            if(mChosenPicturePathList != null && mChosenPicturePathList.size()>0){
                intent.putExtra(MultiImageSelectorActivity.EXTRA_DEFAULT_SELECTED_LIST, mChosenPicturePathList);
            }
            startActivityForResult(intent, REQUEST_IMAGE);
        }
    }

    private class PostListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            String title = mTitle.getText().toString();
            String content = mContent.getText().toString();
            if(title.length() == 0){
                return;
            }
            mProgressDialog = ProgressDialog.show(AtyPostNew.this,null,getResources().getString(R.string.committing));
            ArrayMap<String,String> param = new ArrayMap<>();
            param.put("token",StrUtils.token());
            param.put("topicid",mTopicID);
            param.put("title",title);
            param.put("body",content);
            OkHttpUtils.post(StrUtils.PUBLISH_POST_URL,param,TAG,new OkHttpUtils.SimpleOkCallBack(){
                @Override
                public void onFailure(IOException e) {
                    super.onFailure(e);
                }
                @Override
                public void onResponse(String s) {
                    LogUtils.i(TAG,s);
                    JSONObject j = OkHttpUtils.parseJSON(AtyPostNew.this, s);
                    if(j == null){
                        return;
                    }
                    String id = j.optString("id");
                    if(mChosenPicturePathList.size()==0) {
                        setResult(RESULT_OK);
                        finish();
                        return;
                    }
                    ArrayMap<String,String> p = new ArrayMap<>();
                    p.put("token",StrUtils.token());
                    p.put("type","-4");
                    p.put("postid",id);
                    mSendImageResponseNum.set(0);
                    for(int number = 0; number<mChosenPicturePathList.size(); number++){
                        p.put("number", String.format("%d", number));
                        String path = mChosenPicturePathList.get(number);
                        OkHttpUtils.uploadFile(StrUtils.UPLOAD_AVATAR_URL,p,path,MEDIA_TYPE_JPG,TAG,new OkHttpUtils.SimpleOkCallBack(){
                            @Override
                            public void onFailure(IOException e) {
                                uploadImageReturned();
                            }

                            @Override
                            public void onResponse(String s) {
                                uploadImageReturned();
                            }
                        });
                    }

                }
            });
        }
    }
    private void uploadImageReturned(){
        int num = mSendImageResponseNum.incrementAndGet();
        if(num == mChosenPicturePathList.size()){
            setResult(RESULT_OK);
            finish();
        }

    }
}
