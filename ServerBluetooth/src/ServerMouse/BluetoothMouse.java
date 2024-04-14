package ServerMouse;
import java.awt.*;
import java.util.StringTokenizer;

import static java.awt.event.InputEvent.BUTTON1_DOWN_MASK;
import static java.awt.event.InputEvent.BUTTON3_DOWN_MASK;

//TODO Handle multiple screens
public class BluetoothMouse {
    Robot robot;
    Toolkit tools;
    double dpiX, dpiY;
    final float INCHES_PER_METER = 39.3701F;
    int x;
    int y;
    public BluetoothMouse(){
        tools = Toolkit.getDefaultToolkit();
        setDPIs();
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.out.println("Could not create robot");
            throw new RuntimeException(e);
        }
        x = MouseInfo.getPointerInfo().getLocation().x;
        y = MouseInfo.getPointerInfo().getLocation().y;
    }
    public void processMouseEvent(String message){
        StringTokenizer st = new StringTokenizer(message,",\n");
        String tempToken = st.nextToken();
        if(tempToken.equals("M")){
            //process mouse data
            String xToken = st.nextToken();
            String yToken = st.nextToken();
            double xPos =  Double.parseDouble(xToken);
            double yPos =  Double.parseDouble(yToken);
            calculateMousePath(xPos,yPos);

        }
        else if(tempToken.equals("C")){
            int x = Integer.parseInt(st.nextToken());
            if(x%2 != 0)
                mouseClick(x);
            else
                mouseRelease(x);
        }
    }

    private void calculateMousePath(double xPos, double yPos){
        x = (int)(dpiX * (INCHES_PER_METER*xPos) + x);
        y = y - (int)(dpiY * (INCHES_PER_METER*yPos));
        // Get the screen resolution in pixels
        mouseMove(x,y);
    }

    private void setDPIs(){
        double screenWidth = tools.getScreenSize().getWidth();
        double screenHeight = tools.getScreenSize().getHeight();

        // Get the screen resolution in DPI (dots per inch)
        double screenDPI = tools.getScreenResolution();

        // Calculate the DPI for both width and height
        dpiX = screenWidth / screenDPI;
        dpiY = screenHeight / screenDPI;
    }

    private void mouseMove(int xPixel, int yPixel){
       //move mouse to that spot
       robot.mouseMove(xPixel, yPixel);
    }

    private void mouseClick(int x){
        //if 1 or 3, left vs right
        if(x == 1)
            robot.mousePress(BUTTON1_DOWN_MASK);
        else if(x == 3)
            robot.mousePress(BUTTON3_DOWN_MASK);
    }

    private void mouseRelease(int x){
        //if 0 or 2, left vs right
        if(x == 0)
            robot.mouseRelease(BUTTON1_DOWN_MASK);
        else if(x == 2)
            robot.mouseRelease(BUTTON3_DOWN_MASK);
    }
}