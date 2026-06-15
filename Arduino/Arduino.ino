#include<Servo.h>

Servo steeringServo;
int servoPin = 9;

const int in1 = 3;  // Motor A Forward
const int in2 = 5;  // Motor A Reverse
const int in3 = 6;  // Motor B Forward
const int in4 = 11; // Motor B Reverse

void setup() {
  Serial.begin(9600);
  steeringServo.attach(servoPin);
  steeringServo.write(90);

  pinMode(in1, OUTPUT);
  pinMode(in2, OUTPUT);
  pinMode(in3, OUTPUT);
  pinMode(in4, OUTPUT);
  
  // Ensure the car is totally stopped when it turns on
  stopMotors();
}

void loop() {
  if(Serial.available() > 0){
    char commandMarker = Serial.read();
    
    if(commandMarker == 'A'){
      int targetAngle = Serial.parseInt();
      targetAngle = constrain(targetAngle, 0, 180);
      steeringServo.write(targetAngle);
    }

    if(commandMarker == 'S'){
      int targetSpeed = Serial.parseInt();
      
      if(targetSpeed > 0) {
        // DRIVE FORWARD
        // Safety constraint to ensure we don't exceed max PWM limit
        targetSpeed = constrain(targetSpeed, 0, 255); 
        
        analogWrite(in1, targetSpeed); // Spin Motor A forward
        analogWrite(in2, 0);
        analogWrite(in3, targetSpeed); // Spin Motor B forward
        analogWrite(in4, 0);
      } 
      else if(targetSpeed < 0) {
        // DRIVE REVERSE
        int reverseSpeed = abs(targetSpeed);
        reverseSpeed = constrain(reverseSpeed, 0, 255);
        
        analogWrite(in1, 0);
        analogWrite(in2, reverseSpeed); // Spin Motor A backward
        analogWrite(in3, 0);
        analogWrite(in4, reverseSpeed); // Spin Motor B backward
      } 
      else {
        // STOP (targetSpeed == 0)
        stopMotors();
      }
    }
  }
}

void stopMotors() {
  analogWrite(in1, 0);
  analogWrite(in2, 0);
  analogWrite(in3, 0);
  analogWrite(in4, 0);
}
