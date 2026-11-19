package android.content;
import java.util.*;
/** Minimal in-memory preference adapter for exercising the real Store class. */
public final class SharedPreferences {
 private final Map<String,Object> values=new HashMap<String,Object>();
 public long getLong(String k,long d){Object v=values.get(k);return v==null?d:(Long)v;}
 public int getInt(String k,int d){Object v=values.get(k);return v==null?d:(Integer)v;}
 public float getFloat(String k,float d){Object v=values.get(k);return v==null?d:(Float)v;}
 public boolean getBoolean(String k,boolean d){Object v=values.get(k);return v==null?d:(Boolean)v;}
 public String getString(String k,String d){Object v=values.get(k);return v==null?d:(String)v;}
 public Editor edit(){return new Editor();}
 public final class Editor {
  public Editor putLong(String k,long v){values.put(k,v);return this;}
  public Editor putInt(String k,int v){values.put(k,v);return this;}
  public Editor putFloat(String k,float v){values.put(k,v);return this;}
  public Editor putBoolean(String k,boolean v){values.put(k,v);return this;}
  public Editor putString(String k,String v){values.put(k,v);return this;}
  public boolean commit(){return true;}public void apply(){}
 }
}
