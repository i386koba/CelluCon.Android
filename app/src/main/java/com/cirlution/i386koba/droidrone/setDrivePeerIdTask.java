package com.cirlution.i386koba.droidrone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
//import android.widget.EditText;
import android.widget.TextView;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.cirlution.i386koba.droidrone.MainActivity.REQUEST_AUTHORIZATION_FROM_DRIVE;

// Created by kobayashi on 2016/03/16.

//グーグルのAPIを使うときに欠かせないGoogle OAuthの作り方と使い方 (3/3)  //http://www.atmarkit.co.jp/ait/articles/1509/15/news017_3.html
// AsyncTask | Android Developers http://developer.android.com/intl/ja/reference/android/os/AsyncTask.html
//AsyncTaskを使った非同期処理のきほん http://dev.classmethod.jp/smartphone/android/asynctask/
//ジェネリクス<>の指定 doInBackgroundメソッドの引数の型, onProgressUpdateメソッドの引数の型, onPostExecuteメソッドの戻り値の型
//doInBackgroundメソッドに複数の異なるデータ型の引数を渡したい場合は、Object型を指定してdoInBackground内でキャスト
//  http://niudev.blogspot.jp/2012/02/blog-post.html
public class setDrivePeerIdTask extends AsyncTask<Object, String, Intent> {
    private TextView textView;
    static final String SKYWAYRC_DIR = "SkyWayRC";
    static final String SKYWAY_ANDROID_ID = "SkyWayAndroid.id";
    /**
     * コンストラクタ
     */
    public setDrivePeerIdTask(TextView t) {
        super();
        this.textView = t;
    }
    Context context;
    @Override
    protected Intent doInBackground(Object... params) {
        String skyWayRC_FolderId = null;// 指定のタイトルのフォルダID を取得
        Drive mDrive = (Drive) params[0];
        String peerId = (String) params[1];
        context = (Context) params[2];
        Log.e("doInBackground", mDrive.toString());
        Intent mIntent = null;
        try {
            // API呼び出し  // クエリ文字列をセット（以下は"'root' in parents MIMETYPEを指定）クエリ使わないとレスポンス遅い。ゴミ箱でないクエリ追加
            Drive.Files.List request = mDrive.files().list().setQ("trashed = false and 'root' in parents and mimeType = 'application/vnd.google-apps.folder'");
            List<File> mFolderList = new ArrayList<>();
            //do..while文 http://www.javadrive.jp/start/for/index7.html
            do {
                FileList files = request.execute();
                // 取得したFileリストを保持用のメンバーにセット
                mFolderList.addAll(files.getItems());
                // 全アイテムを取得するために繰り返し
                request.setPageToken(files.getNextPageToken());
            } while (request.getPageToken() != null && request.getPageToken().length() > 0);

            //拡張for文(for-each文) http://www.javadrive.jp/start/for/index8.html
            for (File f : mFolderList) {
                //フォルダチェック getExplicitlyTrashed() ゴミ箱にあるのか調べるの追加
                if (SKYWAYRC_DIR.equals(f.getTitle()) && !f.getExplicitlyTrashed()) {
                    skyWayRC_FolderId = f.getId();
                    publishProgress("Exist a folder." + f.getTitle());
                    Log.e("Exist a folder.", f.getTitle() + " MimeType: " + f.getMimeType() + " p: " + f.getParents());
                    break;
                }
            }
            // タイトルのファイルの ID を取得
            String fileIdOrNull = null;
            if (skyWayRC_FolderId != null) {
                Log.e("skyWayRC_FolderId", skyWayRC_FolderId);
                //ID ファイルチェック
                // API呼び出し  // クエリ文字列をセット（以下はskyWayRC_FolderIdを指定）
                Drive.Files.List requestF = mDrive.files().list().setQ("'" + skyWayRC_FolderId + "' in parents");
                List<File> mFileListF = new ArrayList<>();
                //do..while文 http://www.javadrive.jp/start/for/index7.html
                do {
                    FileList files = requestF.execute();
                    // 取得したFileリストを保持用のメンバーにセット
                    mFileListF.addAll(files.getItems());
                    // 全アイテムを取得するために繰り返し
                    requestF.setPageToken(files.getNextPageToken());
                } while (requestF.getPageToken() != null && requestF.getPageToken().length() > 0);
                for (File f : mFileListF) {
                    if (SKYWAY_ANDROID_ID.equals(f.getTitle())) {
                        fileIdOrNull = f.getId();
                        publishProgress("Exist a file." + f.getTitle());
                        break;
                    }
                }
            } else {
                //SKYWAYRC_DIR フォルダ作成 Creating a folder https://developers.google.com/drive/v2/web/folder#creating_a_folder
                File body = new File();
                body.setTitle(SKYWAYRC_DIR);
                body.setMimeType("application/vnd.google-apps.folder");
                Log.e("Creating a folder", SKYWAYRC_DIR);
                File f = mDrive.files().insert(body).execute();
                // folder ID.
                skyWayRC_FolderId = f.getId();
                publishProgress("Created a folder" + f.getTitle());
                Log.e("Created a folder", f.getTitle());
            }

            File body = new File();
            body.setTitle(SKYWAY_ANDROID_ID);//fileContent.getName());
            body.setMimeType("text/plain");
            body.setDescription(peerId);
            body.setParents(Collections.singletonList(new ParentReference().setId(skyWayRC_FolderId)));
            //フォルダ指定  Java で要素がひとつだけのコレクションをつくる http://www.metareal.org/2007/10/07/java-singleton-collection/
            //body.setParents(Collections.singletonList(new ParentReference().setId(skyWayRC_FolderId)));
            ByteArrayContent content = new ByteArrayContent("text/plain", peerId.getBytes(Charset.forName("UTF-8")));
            if (fileIdOrNull == null) {
                mDrive.files().insert(body, content).execute();
                publishProgress("insert!");
                Log.e("insert a file", SKYWAY_ANDROID_ID);
            } else {
                mDrive.files().update(fileIdOrNull, body, content).execute();
                publishProgress("PeerID update!");
                Log.e("update a file", SKYWAY_ANDROID_ID);
            }
        } catch (UserRecoverableAuthIOException e) {
            mIntent = e.getIntent();
            Log.e("AuthIOException", e.getMessage(), e);
            publishProgress("AuthIOException error occur...");
        } catch (IOException e) {
            Log.e("doInBackground", e.getMessage(), e);
            publishProgress("doInBackground error occur...");
        }
        return mIntent;
     }

    @Override
    protected void onPostExecute(Intent mIntent) {
        if (mIntent != null) {
            ((Activity) context).startActivityForResult(mIntent, REQUEST_AUTHORIZATION_FROM_DRIVE);
            Log.e("onPostExecute", mIntent.toString());//"onPostExecute Done.");
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        //Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        //Android TextView : “Do not concatenate text displayed with setText” http://stackoverflow.com/questions/33164886/android-textview-do-not-concatenate-text-displayed-with-settext
        //追加した行を一番上に表示させる。
        String msg = values[0] + "\n";
        String addMsg = msg + textView.getText();
        textView.setText(addMsg);
        //textView.append(msg);
    }
}
//AsyncTask非同期処理後のコールバック機能
//https://qiita.com/a_nishimura/items/1548e02b96bebd0d43e4