#include <SoftwareSerial.h>   // Serial port Library
SoftwareSerial BT(10,11);    // We define pins 10 and 11 for serial communication with the bluetooth module
String cadena;               // string for recieving data from our app
String cadena2;              // string for sending data to our app
int i = 0;
 
void setup()
{
  BT.begin(9600);       // We initialize our bluetooth serial for 9600 bauds
  Serial.begin(9600);   // We initialize the serial pins to comunicate with our radio in 9600 bauds as well
}
 
void loop()
{
  
  if(BT.available())             //if we recieve data from our app
  {                              
    char dato=BT.read();         //we save each character in a variable
    cadena=cadena+dato;            //we concatenate each character in our string variable                        
    }else{
      Serial.print(cadena);       //with this line we send all recieved data to our radio circuit (commands)
      cadena="";                  //reasign of the value of our string
    }
    if(Serial.available()){       //if the radio module sends data
      char d = Serial.read();     //we read each character in a variable
      cadena2=cadena2+d;          //we concatenate each character in our string variable 
    }else{
      Serial.print(cadena2);      //prtins the data to chexk in the serial monitor what its sending (can be avoided)
      BT.print(cadena2);          //sends the information from the radio circuit to our app
      cadena2="";                 //reasign of the string variable
    }
    delay(5);                     //delay of the cpu so it doesn't get stuck (can be avoided)
}
