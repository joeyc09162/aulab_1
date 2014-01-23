import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JOptionPane;


public class Connection implements Runnable{
	ProjectTable PT;
	ServerSocket SS;
	Socket SC;
	DataInputStream DIS;
	DataOutputStream DOS;
	Boolean isConnected;
	
	public Connection(ProjectTable PT){
		this.PT = PT;
		PT.C = this;
	}
	
	public void sendMsg(String S){
		if(isConnected){
			try {
				DOS.writeUTF(S);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		try {
			SS = new ServerSocket(PT.PC_connect_port);
			SC = SS.accept();
			isConnected = true;
			PT.CF.connected();
			DIS = new DataInputStream(new BufferedInputStream(SC.getInputStream()));
			DOS = new DataOutputStream(new BufferedOutputStream(SC.getOutputStream()));
			while(true){
				String Str = DIS.readUTF();
				if (Str.equals("SEND")) {
					catchImg();
				}else if(Str.equals("END")){
					DIS.close();
					SC.close();
					SS.close();
					JOptionPane.showConfirmDialog(PT.PF,"手機端已斷線","連線中斷",JOptionPane.CLOSED_OPTION);
					System.exit(0);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void catchImg() {
		try {
			ServerSocket SS1 = new ServerSocket(PT.PC_connect_img_port);
			Socket SC1 = SS1.accept();
			DataInputStream DIS = new DataInputStream(new BufferedInputStream(SC1.getInputStream()));
			ByteArrayOutputStream BAOS = new ByteArrayOutputStream(2048);
			byte[] buf = new byte[2048];
			int num = DIS.read(buf);
			while (num != -1) {
				BAOS.write(buf,0,num);
				num = DIS.read(buf);
			}
			BAOS.flush();
			PT.PF.I = Toolkit.getDefaultToolkit().createImage(BAOS.toByteArray());
			DIS.close();
			SC1.close();
			SS1.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
