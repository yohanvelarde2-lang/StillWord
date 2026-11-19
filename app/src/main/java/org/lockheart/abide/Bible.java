package org.lockheart.abide;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

final class Bible {
  private static Bible instance;
  final Context context; JSONObject fallback, active;
  private Bible(Context c) throws Exception {
    context=c.getApplicationContext();fallback=new JSONObject(read(context.getAssets().open("bible.json")));
    active=fallback;
    File f=new File(context.getFilesDir(),"authorized-bible.json");
    if(f.exists())try{JSONObject p=new JSONObject(read(new FileInputStream(f)));validate(p);active=p;}catch(Exception ignored){}
  }
  static synchronized Bible get(Context c){try{if(instance==null)instance=new Bible(c);return instance;}catch(Exception e){throw new IllegalStateException("Unable to load bundled Bible",e);}}
  static String read(InputStream in)throws IOException{
    try(InputStream stream=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] b=new byte[8192];int n;while((n=stream.read(b))!=-1){out.write(b,0,n);if(out.size()>15*1024*1024)throw new IOException("Bible pack exceeds 15 MB");}
      return out.toString("UTF-8");
    }
  }
  String verse(String id,int ch,String n){
    try{JSONArray books=active.getJSONArray("books");for(int i=0;i<books.length();i++){
      JSONObject b=books.getJSONObject(i);if(b.getString("id").equals(id)){
        JSONArray vs=b.getJSONArray("chapters").getJSONArray(ch-1);
        for(int k=0;k<vs.length();k++)if(vs.getJSONObject(k).getString("n").equals(n))return vs.getJSONObject(k).getString("t");
      }
    }}catch(Exception ignored){}return "Open your Bible and spend a moment in the Word.";
  }
  void validate(JSONObject p)throws Exception{
    if(!p.getString("format").equals("abide-bible-1"))throw new IOException("Unsupported Bible pack format");
    if(p.getString("translation").isEmpty()||p.getString("translation").length()>120||p.getString("abbreviation").length()>20||p.getString("copyright").isEmpty())throw new IOException("Missing translation or rights information");
    JSONArray books=p.getJSONArray("books"), ref=fallback.getJSONArray("books");
    if(books.length()!=66)throw new IOException("A complete 66-book Bible is required");
    for(int i=0;i<66;i++){
      JSONObject b=books.getJSONObject(i),r=ref.getJSONObject(i);
      if(!b.getString("id").equals(r.getString("id"))||b.getString("name").isEmpty())throw new IOException("Books must use canonical IDs and order");
      JSONArray cs=b.getJSONArray("chapters");if(cs.length()!=r.getJSONArray("chapters").length())throw new IOException("Missing chapter in "+b.getString("name"));
      for(int k=0;k<cs.length();k++){
        JSONArray vs=cs.getJSONArray(k);if(vs.length()==0||vs.length()>200)throw new IOException("Empty or oversized chapter");
        java.util.HashSet<String> seen=new java.util.HashSet<>();
        for(int v=0;v<vs.length();v++){JSONObject row=vs.getJSONObject(v);String n=row.getString("n"),t=row.getString("t");
          if(!n.matches("[0-9]{1,3}([a-z]|-[0-9]{1,3})?")||!seen.add(n)||t.trim().isEmpty()||t.length()>10000)throw new IOException("Invalid verse record");
        }
      }
    }
    checkVerse(p,"PHP",4,"8");checkVerse(p,"JHN",8,"32");
  }
  void checkVerse(JSONObject p,String id,int ch,String n)throws Exception{
    JSONArray bs=p.getJSONArray("books");for(int i=0;i<bs.length();i++)if(bs.getJSONObject(i).getString("id").equals(id)){
      JSONArray vs=bs.getJSONObject(i).getJSONArray("chapters").getJSONArray(ch-1);
      for(int k=0;k<vs.length();k++)if(vs.getJSONObject(k).getString("n").equals(n))return;
    }throw new IOException("Required gate verse missing");
  }
  void install(String raw)throws Exception{
    JSONObject p=new JSONObject(raw);validate(p);File tmp=new File(context.getFilesDir(),"bible.tmp"), dst=new File(context.getFilesDir(),"authorized-bible.json");
    try(FileOutputStream out=new FileOutputStream(tmp)){out.write(p.toString().getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
    if(!tmp.renameTo(dst))throw new IOException("Could not save imported Bible");active=p;
  }
  void restore(){new File(context.getFilesDir(),"authorized-bible.json").delete();active=fallback;}
}
