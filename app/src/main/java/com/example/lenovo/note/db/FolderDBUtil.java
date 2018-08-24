package com.example.lenovo.note.db;

import android.text.TextUtils;
import android.util.Log;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.List;

import static org.litepal.crud.DataSupport.order;

/**
 * Created by Lenovo on 2018/8/20.
 */

public class FolderDBUtil {
    public static final int NAME_MAX_LENGTH=32;

    private static final String TAG = "FolderDBUtil";

    private static List<Folder> sFolderList=null;
    private static List<Integer> sCountList=new ArrayList<>();

    private static boolean sFiltering=false;
    private static String sQueryText;

    public static Folder get(int position){
        if(sFolderList==null){
//            List<Folder> result= DataSupport.order("folderName").find(Folder.class);
//            for(Folder folder:result){
//                sFolderList.add(folder);
//            }
            query();
        }
        return sFolderList.get(position);
    }

//    /** 获取便签夹中的便签数*/
//    public static int getCount(int folderId){
//        return DataSupport.where("folderId = ?",String.valueOf(folderId)).count(NoteFolder.class);
//    }

    /** 获取便签夹中的便签数*/
    public static int getNoteCount(int position){
        // FIXME: 2018/8/20 性能？
        if(sCountList.isEmpty()){
            int size=folderCount();
            for(int i=0;i<size;i++){
                sCountList.add(DataSupport
                        .where("folderId = ?",String.valueOf(get(i).getId()))
                        .count(NoteFolder.class));
            }
        }
        return sCountList.get(position);
    }

    public static int totalNotes(){
        return DataSupport.count(Note.class);
    }

    public static boolean add(Folder folder){
        int count=DataSupport.where("folderName = ?",folder.getFolderName())
                .count(Folder.class);
        if(count>0){
            return false;
        }
        folder.save();
        // FIXME: 2018/8/20 性能?
        clearCache();
        Log.d(TAG, "add: "+folder.getId());
        return true;
    }

    public static void remove(int position){
        Folder folder=get(position);
        Log.d(TAG, "remove: id "+folder.getId());
        Log.d(TAG, "remove: "+position);
        clearNotes(position);
        folder.delete();
        // FIXME: 2018/8/20 性能?
        clearCache();
    }

    public static boolean update(int position,String newName){
        Folder folder=get(position);
        // 同名检查
        int count=DataSupport.where("id <> ? and folderName = ?",
                String.valueOf(folder.getId()),newName)
                .count(Folder.class);
        if(count>0){
            return false;
        }
        folder.setFolderName(newName);
        folder.save();
        sFolderList=null;
        return true;
    }

    public static void clearNotes(int position){
        int folderId=get(position).getId();
        Log.d(TAG, "clearNotes: folderid "+folderId);
//        DataSupport.deleteAll(Note.class,
//                "id in (select noteId from NoteFolder where folderId = ?)",
//                String.valueOf(folderId));
//        DataSupport.deleteAll(NoteFolder.class,
//                "folderId = ?",String.valueOf(folderId));
        // 不仅要删除便签以及便签的目录项，还要删除便签附带的图片
        // 所以先查询，再逐个删除
        List<Note> notes=NoteDBUtil.query(folderId);
        Log.d(TAG, "clearNotes: "+notes.size());
        for(Note note:notes){
            NoteDBUtil.remove(note);
        }
        clearCache();
    }

    public static void clearCache(){
        sFolderList=null;
        sCountList.clear();
    }

    public static int folderCount(){
        if(sFolderList==null){
            query();
        }
        return sFolderList.size();
    }

    public static int getRank(int id){
        int rank=0;
        rank=DataSupport
                .where("folderName < (select folderName from Folder where id = ?)",
                        String.valueOf(id))
                .count(Folder.class);
        return rank;
    }

    public static Folder findByFolderId(int id){
        Folder result=DataSupport.find(Folder.class,id);
        return result;
    }

    public static void setFilter(boolean sFiltering,String sQueryText) {
        FolderDBUtil.sFiltering = sFiltering;
        FolderDBUtil.sQueryText = sQueryText;
        if(sFiltering&&TextUtils.isEmpty(sQueryText)){
            FolderDBUtil.sFiltering=false;
        }
        clearCache();
    }

    public static void query(){
        if(sFiltering){
            sFolderList=DataSupport.where("folderName like ?","%"+sQueryText+"%")
                    .order("folderName").find(Folder.class);
        }else{
            sFolderList= order("folderName").find(Folder.class);
        }
        sCountList.clear();
    }
}
