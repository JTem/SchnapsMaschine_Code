#include <SoftwareSerial.h>
#include <AccelStepper.h>

SoftwareSerial btAdapter (12, 13); //RX TX
AccelStepper stepper(AccelStepper::DRIVER, 9, 8);



String counterString, minuteString;
int minute = 0, counter = 12;
int minuteCounter = 0;
int currentMin = 0;

String HC05_Response = "";

int enableStepper = 10;
int schalter = 2;
int i;
long steps = 5493.4256 * counter;
long millisecs;
boolean _on = false;
boolean _restart = false;
boolean _los = false;
boolean _finish = true;
boolean justOnce = true;
boolean bt_Connected = false;


const long p1 = 5493.4256;
const long p2 = 5493.4256 * 2;
const long p3 =5493.4256 * 3;
const long p4 = 5493.4256 * 4;
const long p5 = 5493.4256 * 5;
const long p6 = 5493.4256 * 6;
const long p7 = 5493.4256 * 7;
const long p8 = 5493.4256* 8;
const long p9 = 5493.4256 * 9;
const long p10 = 5493.4256 * 10;
const long p11 = 5493.4256 * 11;
const long p12 = 5493.4256 * 12;

void setup() {
  pinMode(enableStepper, OUTPUT);
  pinMode(schalter, INPUT_PULLUP);
  btAdapter.begin(9600);



  stepper.setMaxSpeed(1700);
  stepper.setAcceleration(2500);
  stepper.moveTo(-steps);

}

void loop() {
  checkBT();

  if (digitalRead(schalter)) {
    _on = true;

  } else if (atTop(-stepper.currentPosition())) {
    _on = false;
    stepper.setCurrentPosition(0);
    stepper.moveTo(-steps);
    delay(50);
  }

  if (_on == false || stepper.distanceToGo() == 0)digitalWrite(enableStepper, HIGH);
  else digitalWrite(enableStepper, LOW);

  if (!bt_Connected) {

    if (_on == true) {

      stepper.run();
    }


  } else {



    if ( minuteCounter == currentMin + minute && justOnce == false && _finish == true || _los == true) {
      _restart = true;

    }


    if (_restart == true && _on == true ) {
     _finish=false;
      //stepper.moveTo(-steps);
      stepper.run();
      if (stepper.distanceToGo() == 0) {
        _restart = false;
        _los = false;
        _finish = true;
        currentMin = minuteCounter;
        stepper.setCurrentPosition(0);
        delay(50);
      }
    }
  }
}

void checkBT() {
  if (_on == true) {
    millisecs = millis();
    if (millisecs >= 60000 * (minuteCounter + 1)) {
      minuteCounter += 1;
    }
  }

  if (btAdapter.available() > 0) {
    bt_Connected = true;

    char c = btAdapter.read();
    HC05_Response += c;
    if (HC05_Response.length() == 1 && HC05_Response.charAt(0) == 'a') {
      HC05_Response = "";
    }
    if (HC05_Response.length() == 11 && HC05_Response.charAt(0) == 'S'
        && HC05_Response.charAt(10) == 'E') {

      _los = true;

      minuteString = HC05_Response.substring(2, 5);
      counterString = HC05_Response.substring(6, 9);

      minute = minuteString.toInt() - 100;
      counter = counterString.toInt() - 100;
      if (minute == 0)justOnce = true;
      else justOnce = false;
      steps = 5493.4256 * counter;
      if (stepper.distanceToGo() == 0) {
        stepper.setCurrentPosition(0);
        stepper.moveTo(-steps);
      }

      HC05_Response = "";
    }

  }
}

bool atTop(long pos) {

  bool b = false;
  if (pos == 0 || pos == p1 || pos == p2 || pos == p3 || pos == p4 || pos == p5 || pos == p6 || pos == p7
      || pos == p8 || pos == p9 || pos == p10 || pos == p11 || pos == p12)b = true;
  return b;

}

