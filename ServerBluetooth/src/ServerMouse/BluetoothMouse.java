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
    int screenWidth;
    int screenHeight;
    boolean leftPressed;
    boolean rightPressed;
    
    public BluetoothMouse(){
        tools = Toolkit.getDefaultToolkit();
        setDimensions();
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
        if(!(leftPressed&&rightPressed)) {
            x = (int) (300 * (INCHES_PER_METER * xPos) + x);
            y = y - (int) (300 * (INCHES_PER_METER * yPos));
        }
        // Get the screen resolution in pixels

        x = (x > screenWidth)? screenWidth: x;
        x = (x < 0)? 0: x;
        y = (y > screenHeight)? screenHeight: y;
        y = (y < 0)? 0: y;

            mouseMove(x,y);
    }

    private void setDimensions(){
        screenWidth = (int) tools.getScreenSize().getWidth();
        screenHeight = (int) tools.getScreenSize().getHeight();

    }

    private void mouseMove(int xPixel, int yPixel){
       //move mouse to that spot
       robot.mouseMove(xPixel, yPixel);
    }

    private void mouseClick(int x){
        //if 1 or 3, left vs right
        if(x == 1) {
            robot.mousePress(BUTTON1_DOWN_MASK);
            leftPressed = true;
        }
        else if(x == 3) {
            robot.mousePress(BUTTON3_DOWN_MASK);
            rightPressed = true;

        }
    }

    private void mouseRelease(int x){
        //if 0 or 2, left vs right
        if(x == 0) {
            robot.mouseRelease(BUTTON1_DOWN_MASK);
            leftPressed = false;
        }
        else if(x == 2) {
            robot.mouseRelease(BUTTON3_DOWN_MASK);
            rightPressed = false;
        }
    }
}
