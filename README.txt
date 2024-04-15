This is a phone -> mouse project for my school's senior year engineering class
the project involves attempting to use code on a cellphone that would read in
accelerometer data, connect to the user's computer, and act as a mouse for
that computer. Right now, this is just a proof of concept to have something
for our presentation in May, but if it works well enough we may continue to
refine it.

The code for this project is in Java only, since that's the main language I am
comfortable in.

There are four folders, MouseBluetooth, MouseNonBluetooth, ExternalLibraries,
and ServerBluetooth

MouseBluetooth - Code in Android Studio to connect the phone and the laptop
using bluetooth, and send accelerometer data. Currently, this is the phone
code on the android end that both creates the app, and sends the phone's
position data to the computer in the form of x's and y's. It also sends
the mouse presses when needed. In order to calculate the change in position of
the phone, the code does a double integral of the accelerometer data over a
set of time between sensor changes.

ServerBluetooth - Code in IntelliJ in java that connects the phone client to
the server code on the computer. The server reads in the changes in
position,converts them to pixels and moves the mouse on the screen. It also
reads in and performs mouse presses, left click and right click.

ExternalLibraries - jar files that the server needs for bluetooth events

MouseNonBluetooth - Code in Android Studio to connect the phone and the laptop
using a usb connection and send accelerometer data. Was a bakup to try when
bluetooth seemed too difficult.
