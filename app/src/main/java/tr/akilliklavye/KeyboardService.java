package tr.akilliklavye;

import android.inputmethodservice.InputMethodService;
import android.graphics.Color;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.Toast;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.ExtractedTextRequest;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class KeyboardService extends InputMethodService {
  private boolean shift=false, symbols=false;
  private LinearLayout root, suggestions;
  private SpeechRecognizer speech;
  private boolean listening=false;
  private final Handler deleteHandler=new Handler(Looper.getMainLooper());
  private boolean deleting=false;
  private final Runnable repeatDelete=new Runnable(){@Override public void run(){if(deleting){deleteOne();deleteHandler.postDelayed(this,65);}}};
  private void deleteOne(){InputConnection ic=getCurrentInputConnection();if(ic!=null){CharSequence selected=ic.getSelectedText(0);if(selected!=null&&selected.length()>0)ic.commitText("",1);else ic.deleteSurroundingText(1,0);}refresh();}
  private void stopDelete(){deleting=false;deleteHandler.removeCallbacks(repeatDelete);}
  private final Locale tr=new Locale("tr","TR");
  private final String[] words=("merhaba nasılsın nasılsınız günaydın iyi akşamlar teşekkür ederim rica lütfen tamam evet hayır bugün yarın dün şimdi sonra biraz çok güzel harika olur olacak olmadı neden nasıl nerede ne zaman kim hangi için ile ama çünkü ben sen biz siz onlar bizim sizin benim senin bana sana size bize burada orada geliyorum gidiyorum geleceğim gidiyorum gönderiyorum gönderildi gönderdim yazıyorum yazdım istiyorum istedim gerekli gerekiyor olabilir olabilir mi yardımcı olur musun müsait misin telefon klavye mesaj görüşürüz görüşmek üzere kolay gelsin iyi çalışmalar bilgi bilginize kontrol kontrolü kontrol edildi müşterimiz müşteri ürün ürünün ürünler ürünleri parça parçanın değişim değişimi değiştirilecek servis kayıt kaydı kaydın kayıtları firma firmaya firmadan çözüm sorun görsel görseli evrak arşiv iş emri teslimat teslim edildi işlemler işlemi işlem sonuç sonucunda açıklama açıklaması ekip ekibi çalışma çalışma arkadaşlar arkadaşımız merhabalar selam tamamdır bekliyorum bekleniyor beklenmektedir paylaşabilir paylaşabilir misiniz yapabilir yapabilir misin gerekiyor teşekkürler").split(" ");
  @Override public View onCreateInputView(){
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(3,4,3,4);root.setBackgroundColor(Color.rgb(220,224,230));
    render();return root;
  }
  private Button key(String label,int weight){
    Button b=new Button(this);b.setText(label);b.setTextSize(17);b.setAllCaps(false);b.setPadding(0,0,0,0);
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,(int)(60*getResources().getDisplayMetrics().density),weight);
    p.setMargins(1,2,1,2);b.setLayoutParams(p);return b;
  }
  private void render(){
    root.removeAllViews();
    suggestions=new LinearLayout(this);
    root.addView(suggestions);
    String[] rows=symbols?new String[]{"1234567890","@#₺%&-+()","*':;!?"}:new String[]{"qwertyuıopğü","asdfghjklşi","zxcvbnmöç"};
    for(int ri=0;ri<rows.length;ri++){
      LinearLayout line=new LinearLayout(this);
      for(char ch:rows[ri].toCharArray()){
        String letter=String.valueOf(ch);
        Button b=key(shift?letter.toUpperCase(tr):letter,1);
        b.setOnClickListener(v->send(shift?letter.toUpperCase(tr):letter));
        line.addView(b);
      }
      if(ri==2){
        Button del=key("⌫",1);
        del.setOnTouchListener((v,event)->{switch(event.getActionMasked()){case MotionEvent.ACTION_DOWN:stopDelete();deleteOne();deleting=true;deleteHandler.postDelayed(repeatDelete,360);return true;case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:stopDelete();return true;default:return true;}});
        line.addView(del);
      }
      root.addView(line);
    }
    LinearLayout controls=new LinearLayout(this);
    Button mode=key(symbols?"ABC":"?123",1);mode.setOnClickListener(v->{symbols=!symbols;render();});controls.addView(mode);
    Button cap=key("⇧",1);cap.setOnClickListener(v->{shift=!shift;render();});controls.addView(cap);
    Button comma=key(",",1);comma.setOnClickListener(v->send(","));controls.addView(comma);
    Button space=key("Boşluk",5);space.setOnClickListener(v->space());controls.addView(space);
    Button dot=key(".",1);dot.setOnClickListener(v->send("."));controls.addView(dot);
    Button enter=key("↵",1);enter.setOnClickListener(v->send("\n"));controls.addView(enter);
    Button mic=key("🎤",1);mic.setOnClickListener(v->startVoice());controls.addView(mic);
    root.addView(controls);
    refresh();
  }
  private void send(String s){
    InputConnection ic=getCurrentInputConnection();if(ic!=null)ic.commitText(s,1);
    refresh();
  }
  private String currentWord(){
    InputConnection ic=getCurrentInputConnection();if(ic==null)return "";
    CharSequence before=ic.getTextBeforeCursor(60,0);if(before==null)return "";
    String s=before.toString();int i=s.length();
    while(i>0&&Character.isLetter(s.charAt(i-1)))i--;
    return s.substring(i);
  }
  private int distance(String a,String b){
    int[] prev=new int[b.length()+1],next=new int[b.length()+1];
    for(int j=0;j<=b.length();j++)prev[j]=j;
    for(int i=1;i<=a.length();i++){
      next[0]=i;
      for(int j=1;j<=b.length();j++)next[j]=Math.min(Math.min(next[j-1]+1,prev[j]+1),prev[j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
      int[] t=prev;prev=next;next=t;
    }
    return prev[b.length()];
  }
  private ArrayList<String> candidates(String raw){
    String w=raw.toLowerCase(tr);
    ArrayList<String> list=new ArrayList<>();
    if(w.length()<2)return list;
    for(String s:words)if(!list.contains(s)&& (s.startsWith(w)|| (w.length()>2&&Math.abs(s.length()-w.length())<=2&&distance(w,s)<= (w.length()>5?2:1))))list.add(s);
    Collections.sort(list,(a,b)->{
      int aa=a.startsWith(w)?-1:distance(w,a);
      int bb=b.startsWith(w)?-1:distance(w,b);
      return aa!=bb?Integer.compare(aa,bb):Integer.compare(a.length(),b.length());
    });
    return list;
  }
  private void replace(String replacement,boolean addSpace){
    InputConnection ic=getCurrentInputConnection();if(ic==null)return;
    String old=currentWord();
    ic.beginBatchEdit();
    ic.deleteSurroundingText(old.length(),0);
    if(!old.isEmpty()&&Character.isUpperCase(old.charAt(0)))replacement=replacement.substring(0,1).toUpperCase(tr)+replacement.substring(1);
    ic.commitText(replacement+(addSpace?" ":""),1);
    ic.endBatchEdit();refresh();
  }
  private void space(){
    String old=currentWord();ArrayList<String> cs=candidates(old);
    String w=old.toLowerCase(tr);
    if(!w.isEmpty()&&!w.contains(" ")&&!java.util.Arrays.asList(words).contains(w)&&!cs.isEmpty()){
      String best=cs.get(0);
      if(!best.startsWith(w)&&distance(w,best)==1&&w.length()>=4){replace(best,true);return;}
    }
    send(" ");
  }
  private void refresh(){
    if(suggestions==null)return;
    suggestions.removeAllViews();
    String word=currentWord();ArrayList<String> cs=candidates(word);
    if(word.length()>0){
      Button original=key(word,1);original.setOnClickListener(v->replace(word,false));suggestions.addView(original);
    }
    for(int i=0;i<Math.min(cs.size(),word.length()>0?2:3);i++){
      String option=cs.get(i);
      Button b=key(option,1);b.setOnClickListener(v->replace(option,true));suggestions.addView(b);
    }
    if(suggestions.getChildCount()==0){
      Button placeholder=key("Türkçe öneriler",1);placeholder.setEnabled(false);suggestions.addView(placeholder);
    }
  }
  private void startVoice(){
    if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
      Toast.makeText(this,"Mikrofon izni için Akıllı Klavye uygulamasını açın",Toast.LENGTH_LONG).show();return;
    }
    if(!SpeechRecognizer.isRecognitionAvailable(this)){
      Toast.makeText(this,"Telefonda konuşma tanıma hizmeti bulunamadı",Toast.LENGTH_LONG).show();return;
    }
    if(speech==null){
      speech=SpeechRecognizer.createSpeechRecognizer(this);
      speech.setRecognitionListener(new RecognitionListener(){
        public void onReadyForSpeech(Bundle p){listening=true;}
        public void onBeginningOfSpeech(){}
        public void onRmsChanged(float rms){}
        public void onBufferReceived(byte[] data){}
        public void onEndOfSpeech(){listening=false;}
        public void onError(int error){listening=false;Toast.makeText(KeyboardService.this,"Ses algılanamadı ("+error+")",Toast.LENGTH_SHORT).show();}
        public void onResults(Bundle results){listening=false;ArrayList<String> items=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(items!=null&&!items.isEmpty())send(items.get(0)+" ");}
        public void onPartialResults(Bundle partial){}
        public void onEvent(int type,Bundle params){}
      });
    }
    if(listening){speech.stopListening();listening=false;return;}
    Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"tr-TR");
    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
    try{speech.startListening(intent);Toast.makeText(this,"Konuşabilirsiniz",Toast.LENGTH_SHORT).show();}
    catch(Exception e){Toast.makeText(this,"Sesli yazma başlatılamadı",Toast.LENGTH_SHORT).show();}
  }
  @Override public void onDestroy(){stopDelete();if(speech!=null){speech.destroy();speech=null;}super.onDestroy();}
  @Override public void onUpdateSelection(int oldSelStart,int oldSelEnd,int newSelStart,int newSelEnd,int candidatesStart,int candidatesEnd){super.onUpdateSelection(oldSelStart,oldSelEnd,newSelStart,newSelEnd,candidatesStart,candidatesEnd);refresh();}
}
