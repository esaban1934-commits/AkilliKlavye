package tr.akilliklavye;
import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(28,50,28,20);
  TextView t=new TextView(this);t.setText("Akıllı Klavye\n\nSesli yazma için mikrofon iznini verin. Ardından klavyeyi etkinleştirip seçin.");l.addView(t);
  Button mic=new Button(this);mic.setText("Mikrofon izni ver");mic.setOnClickListener(v->{if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);else Toast.makeText(this,"Mikrofon izni zaten verilmiş",Toast.LENGTH_SHORT).show();});l.addView(mic);
  Button a=new Button(this);a.setText("Klavyeyi etkinleştir");a.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));l.addView(a);
  Button c=new Button(this);c.setText("Klavyeyi seç");c.setOnClickListener(v->{InputMethodManager m=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);m.showInputMethodPicker();});l.addView(c);setContentView(l);
 }
}