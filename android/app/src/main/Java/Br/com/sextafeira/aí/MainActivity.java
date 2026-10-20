package br.com.sextafeira.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private static final int VOICE_REQUEST=101;
    private EditText prompt, apiKey, model;
    private TextView history, status;
    private TextToSpeech tts;
    private LinearLayout root;
    private int cyan=Color.rgb(56,220,255);
    private final ArrayList<String[]> messages=new ArrayList<>();
    @Override public void onCreate(Bundle b){super.onCreate(b);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(22,30,22,18);root.setBackgroundColor(Color.rgb(7,13,28));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);setContentView(scroll);
        TextView title=label("◉  S E X T A - F E I R A",26,cyan);title.setTypeface(null,Typeface.BOLD);root.addView(title);
        root.addView(label("ASSISTENTE PESSOAL  •  ONLINE",12,Color.LTGRAY));
        status=label("● Pronta para ouvir",15,cyan);root.addView(status);
        history=label("Olá! Toque em FALAR ou escreva um comando.\n",16,Color.WHITE);root.addView(history);
        root.addView(label("Chave de API (guardada apenas neste aparelho)",13,Color.LTGRAY));
        apiKey=field("Cole sua chave de API aqui");apiKey.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        apiKey.setText(getPreferences(0).getString("apiKey",""));root.addView(apiKey);
        root.addView(label("Modelo OpenAI",13,Color.LTGRAY));model=field("gpt-4.1-mini");model.setText(getPreferences(0).getString("model","gpt-4.1-mini"));root.addView(model);
        prompt=field("Digite seu comando...");root.addView(prompt);
        Button talk=button("🎙  FALAR COM SEXTA-FEIRA");root.addView(talk);talk.setOnClickListener(v->listen());
        Button send=button("➤  ENVIAR MENSAGEM");root.addView(send);send.setOnClickListener(v->{String q=prompt.getText().toString().trim();if(!q.isEmpty()){prompt.setText("");handle(q);}});
        root.addView(label("Conexão direta à API. Não publique sua chave no GitHub. A API pode ter custos.",12,Color.LTGRAY));
        tts=new TextToSpeech(this,result->{if(result==TextToSpeech.SUCCESS)tts.setLanguage(new Locale("pt","BR"));});
    }
    private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,12,0,12);return t;}
    private EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(false);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setHint(hint);e.setTextSize(15);return e;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.BLACK);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(cyan));return b;}
    private void listen(){try{Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pt-BR");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Pode falar!");startActivityForResult(i,VOICE_REQUEST);}catch(Exception e){status.setText("Reconhecimento de voz indisponível neste aparelho.");}}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==VOICE_REQUEST&&c==RESULT_OK&&d!=null){ArrayList<String> a=d.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(a!=null&&!a.isEmpty())handle(a.get(0));}}
    private void handle(String q){history.append("\nVOCÊ: "+q+"\n");String lower=q.toLowerCase(new Locale("pt","BR"));
        if(lower.contains("abrir youtube")||lower.contains("abra o youtube")){open("com.google.android.youtube","https://youtube.com");return;}
        if(lower.contains("abrir whatsapp")||lower.contains("abra o whatsapp")){open("com.whatsapp","https://wa.me/");return;}
        if(lower.contains("que horas")||lower.contains("hora agora")){reply("Agora são "+new java.text.SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date())+".");return;}
        String key=apiKey.getText().toString().trim();String selected=model.getText().toString().trim();if(key.isEmpty()){reply("Configure uma chave de API para conversar com a inteligência artificial.");return;}
        getPreferences(0).edit().putString("apiKey",key).putString("model",selected).apply();status.setText("● Pensando...");
        new Thread(()->{String answer=request(key,selected,q);runOnUiThread(()->{status.setText("● Pronta para ouvir");reply(answer);});}).start();
    }
    private void open(String pkg,String fallback){try{Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i!=null){startActivity(i);reply("Abrindo aplicativo.");return;}startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(fallback)));reply("Abrindo no navegador.");}catch(Exception e){reply("Não consegui abrir esse aplicativo.");}}
    private void reply(String s){history.append("SEXTA-FEIRA: "+s+"\n");if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"reply");}
    private String request(String key,String selected,String q){HttpURLConnection c=null;try{
        URL url=new URL("https://api.openai.com/v1/chat/completions");c=(HttpURLConnection)url.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(45000);c.setDoOutput(true);
        c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json");
        JSONArray ms=new JSONArray();ms.put(new JSONObject().put("role","system").put("content","Você é SEXTA-FEIRA, assistente pessoal prestativa. Responda em português brasileiro, com clareza e concisão."));
        for(String[] pair:messages){ms.put(new JSONObject().put("role",pair[0]).put("content",pair[1]));}
        ms.put(new JSONObject().put("role","user").put("content",q));JSONObject body=new JSONObject().put("model",selected.isEmpty()?"gpt-4.1-mini":selected).put("messages",ms);
        try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode();InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=stream.read(buf))!=-1)out.write(buf,0,n);
        String data=out.toString("UTF-8");if(code<200||code>=300)return "A API respondeu com erro HTTP "+code+". Verifique sua chave, modelo e créditos.";
        String answer=new JSONObject(data).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");messages.add(new String[]{"user",q});messages.add(new String[]{"assistant",answer});while(messages.size()>12)messages.remove(0);return answer;
    }catch(Exception e){return "Não consegui conectar à IA. Confira a internet e as configurações.";}finally{if(c!=null)c.disconnect();}}
    @Override protected void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
