package BluetoothChat.cc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class AppleActivity extends Activity{
	private Button b1;
	private EditText e1;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fo);
		
		b1 = (Button) findViewById(R.id.button1);
		e1 = (EditText) findViewById(R.id.textView1);
		
		b1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try{
					FileOutputStream fo = openFileOutput("blood.txt", MODE_PRIVATE);
					BufferedOutputStream bf = new BufferedOutputStream(fo);
					bf.write(e1.getText().toString().getBytes());
					bf.close();
				}catch(Exception e){
					e.printStackTrace();
				}
				
				Intent in = new Intent();
				in.setClass(AppleActivity.this, BluetoothChat.class);
				
				try{
					FileInputStream fi = openFileInput("blood.text");
					BufferedInputStream bi = new BufferedInputStream(fi);
					bi.close();
				}catch(Exception e){
					e.printStackTrace();
				}
				
				Bundle bu = new Bundle();
				bu.putString("blood", e1.getText().toString());
				in.putExtras(bu);
				startActivity(in);
			}
		});
	}
}
