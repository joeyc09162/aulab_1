import java.awt.Toolkit;

public class ProjectTable {
	MainFrame MF;
	ConnectFrame CF;
	Connection C;
	PhoneFrame PF;
	
	final double Screen_rate;
	
	final int PC_connect_port = 12345;
	final int PC_connect_img_port = 12346;
	final int PC_screen_width;
	final int PC_screen_height;
	final int PC_frame_width;
	final int PC_frame_height;
	final int PC_image_width;
	final int PC_image_height;
	final int PC_button_spacing;
	
	String Phone_model;
	String Phone_number;
	String Phone_power_status;
	String Phone_temperature;
	final int Phone_screen_width = 480;
	final int Phone_screen_height = 800;
	
	public ProjectTable(){
		PC_screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
		PC_screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
		PC_frame_height = PC_screen_height/10*9;
		PC_frame_width = PC_frame_height/3*2;
		PC_image_height = PC_frame_height-70;
		Screen_rate = (double)PC_image_height/(double)Phone_screen_height;
		PC_image_width = (int)((double)Phone_screen_width*Screen_rate);
		PC_button_spacing = (PC_frame_width-300)/4;
	}
}
