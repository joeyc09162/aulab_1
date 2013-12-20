package BluetoothChat.cc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

public class BounceView extends View {
	protected static final int BOUNCE_WIDTH = 30;
	protected static final int BOUNCE_HEIGHT = 30;
	protected Drawable mySprite;
	protected Point mySpritePos = new Point(50,50); 
	protected enum HorizontalDirection {LEFT, RIGHT}
	protected enum VerticalDirection {UP, DOWN}
	protected HorizontalDirection myXDirection = HorizontalDirection.RIGHT;
	protected VerticalDirection myYDirection = VerticalDirection.UP;
	protected Paint mPaint = null;

	
	public BounceView(Context context) {
		super(context);
		mPaint = new Paint();
//		this.setBackground(this.getResources().getDrawable(R.drawable.android));
//		this.mySprite = this.getResources().getDrawable(R.drawable.world); 
	}
	
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
//		this.mySprite.setBounds(this.mySpritePos.x, this.mySpritePos.y, 
//		this.mySpritePos.x + 50, this.mySpritePos.y + 50);
		Log.e("ondraw", "ondraw");
//		Log.e("BluetoothChat.candraw", String.valueOf(BluetoothChat.candraw));
//		Log.e("BluetoothChat.hasball", String.valueOf(BluetoothChat.hasball));		
//		Log.e("BluetoothChat.isRight", String.valueOf(BluetoothChat.isRight));	
//		Log.e("BluetoothChat.ball_x", String.valueOf(BluetoothChat.ball_x));
//		Log.e("BluetoothChat.ball_y", String.valueOf(BluetoothChat.ball_y));		
		Log.e("BluetoothChat.isconnect", String.valueOf(BluetoothChat.isconnect));	
//		Log.e("BluetoothChat.issend", String.valueOf(BluetoothChat.issend));		
		
		if(BluetoothChat.isconnect==1){
			if(BluetoothChat.hasball == 1){
				if(BluetoothChat.isRight==1){
					//右半邊
					if(BluetoothChat.issend==1){
						if(BluetoothChat.dir==1){
							myYDirection=VerticalDirection.DOWN;
							mPaint.setColor(Color.YELLOW);
						}else{
							myYDirection=VerticalDirection.UP;
							mPaint.setColor(Color.BLUE);
						}
							myXDirection=HorizontalDirection.RIGHT;
							mySpritePos.x = 0;
							mySpritePos.y = BluetoothChat.ball_y;
							BluetoothChat.issend=0;
					}	
						
						if (mySpritePos.x >= this.getWidth() - BOUNCE_WIDTH) { 
							mPaint.setColor(Color.RED);
							this.myXDirection = HorizontalDirection.LEFT;
						} else if (mySpritePos.x < 0) {
							if(this.myYDirection == VerticalDirection.DOWN){
								BluetoothChat.sendMessage(String.valueOf(mySpritePos.x)+" "+String.valueOf(mySpritePos.y)+" "+String.valueOf(1));
							}else{
								BluetoothChat.sendMessage(String.valueOf(mySpritePos.x)+" "+String.valueOf(mySpritePos.y)+" "+String.valueOf(2));
							}
							
							BluetoothChat.candraw =0;
						}
						
						if (mySpritePos.y >= this.getHeight() - BOUNCE_WIDTH) { 
							mPaint.setColor(Color.BLUE);
							this.myYDirection = VerticalDirection.UP;
						} else if (mySpritePos.y <= BOUNCE_WIDTH) {
							mPaint.setColor(Color.YELLOW);
							this.myYDirection = VerticalDirection.DOWN;
						}
						
						/*==========設置x的位移量==========*/
						if (this.myXDirection == HorizontalDirection.RIGHT) { 
							this.mySpritePos.x += 2;
						} else {
							this.mySpritePos.x -= 2;
						}
						/*==========設置y的位移量==========*/
						if (this.myYDirection == VerticalDirection.DOWN) { 
							this.mySpritePos.y += 2;
						} else {
							this.mySpritePos.y -= 2;
					}
					
				}else{
					//左半邊
					if(BluetoothChat.issend==1){
						
						if(BluetoothChat.dir==1){
							myYDirection=VerticalDirection.DOWN;
						}else{
							myYDirection=VerticalDirection.UP;
						}
						myXDirection=HorizontalDirection.LEFT;
						mySpritePos.x = this.getWidth();
						mySpritePos.y = BluetoothChat.ball_y;
						mPaint.setColor(Color.BLUE);
						BluetoothChat.issend=0;
					}	
						if (mySpritePos.x > this.getWidth() ) { 
							if(this.myYDirection == VerticalDirection.DOWN){
								BluetoothChat.sendMessage(String.valueOf(mySpritePos.x)+" "+String.valueOf(mySpritePos.y)+" "+String.valueOf(1));
							}else{
								BluetoothChat.sendMessage(String.valueOf(mySpritePos.x)+" "+String.valueOf(mySpritePos.y)+" "+String.valueOf(2));
							}
							BluetoothChat.candraw =0;
						//	mPaint.setColor(Color.RED);
						//	this.myXDirection = HorizontalDirection.LEFT;
						} else if (mySpritePos.x <= BOUNCE_WIDTH) {
						
							mPaint.setColor(Color.GREEN);
							this.myXDirection = HorizontalDirection.RIGHT;
						}
						
						if (mySpritePos.y >= this.getHeight() - BOUNCE_WIDTH) { 
							mPaint.setColor(Color.BLUE);
							this.myYDirection = VerticalDirection.UP;
						} else if (mySpritePos.y <= BOUNCE_WIDTH) {
							mPaint.setColor(Color.YELLOW);
							this.myYDirection = VerticalDirection.DOWN;
						}
						
						if (this.myXDirection == HorizontalDirection.RIGHT) { 
							this.mySpritePos.x += 2;
						} else {
							this.mySpritePos.x -= 2;
						}
						
						if (this.myYDirection == VerticalDirection.DOWN) { 
							this.mySpritePos.y += 2;
						} else {
							this.mySpritePos.y -= 2;
						}
					
				}
			}else{
				if (mySpritePos.x >= this.getWidth() - BOUNCE_WIDTH) { 
					mPaint.setColor(Color.RED);
					this.myXDirection = HorizontalDirection.LEFT;
				} else if (mySpritePos.x <= BOUNCE_WIDTH) {
				
					mPaint.setColor(Color.GREEN);
					this.myXDirection = HorizontalDirection.RIGHT;
				}
				
				if (mySpritePos.y >= this.getHeight() - BOUNCE_WIDTH) { 
					mPaint.setColor(Color.BLUE);
					this.myYDirection = VerticalDirection.UP;
				} else if (mySpritePos.y <= BOUNCE_WIDTH) {
					mPaint.setColor(Color.YELLOW);
					this.myYDirection = VerticalDirection.DOWN;
				}
				
				if (this.myXDirection == HorizontalDirection.RIGHT) { 
					this.mySpritePos.x += 2;
				} else {
					this.mySpritePos.x -= 2;
				}
				
				if (this.myYDirection == VerticalDirection.DOWN) { 
					this.mySpritePos.y += 2;
				} else {
					this.mySpritePos.y -= 2;
				}
				
			}
		}
		
		canvas.drawCircle(mySpritePos.x, mySpritePos.y, BOUNCE_WIDTH, mPaint);
//		this.draw(canvas);
//		this.mySprite.draw(canvas);
	}
}