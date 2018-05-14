#include "Arduino.h"
#include "GolfBallStand.h"



GolfBallStand::GolfBallStand() {
  _init();
}

void GolfBallStand::_init() {
  pinMode(PIN_LED_1_UNDER, OUTPUT);
  pinMode(PIN_LED_1_FRONT, OUTPUT);
  pinMode(PIN_LED_2_UNDER, OUTPUT);
  pinMode(PIN_LED_2_FRONT, OUTPUT);
  pinMode(PIN_LED_3_UNDER, OUTPUT);
  pinMode(PIN_LED_3_FRONT, OUTPUT);
  pinMode(PIN_RED, OUTPUT);
  pinMode(PIN_GREEN, OUTPUT);
  pinMode(PIN_BLUE, OUTPUT);
  pinMode(PIN_GOLF_BALL_STAND_SWITCH, INPUT_PULLUP);
  digitalWrite(PIN_LED_1_UNDER, LED_TRANSISTOR_OFF);
  digitalWrite(PIN_LED_1_FRONT, LED_TRANSISTOR_OFF);
  digitalWrite(PIN_LED_2_UNDER, LED_TRANSISTOR_OFF);
  digitalWrite(PIN_LED_2_FRONT, LED_TRANSISTOR_OFF);
  digitalWrite(PIN_LED_3_UNDER, LED_TRANSISTOR_OFF);
  digitalWrite(PIN_LED_3_FRONT, LED_TRANSISTOR_OFF);
  digitalWrite(PIN_RED, COLOR_TRANSISTOR_OFF);
  digitalWrite(PIN_GREEN, COLOR_TRANSISTOR_OFF);
  digitalWrite(PIN_BLUE, COLOR_TRANSISTOR_OFF);
  setLedState(LED_OFF, LOCATION_1 + LOCATION_2 + LOCATION_3, LED_UNDER_AND_FRONT);
}

void GolfBallStand::setLedState(int ledColor, int location, int underOrFront) {
  // Start by clearing off all LEDs and colors.
  digitalWrite(PIN_RED, COLOR_TRANSISTOR_OFF);
  digitalWrite(PIN_GREEN, COLOR_TRANSISTOR_OFF);
  digitalWrite(PIN_BLUE, COLOR_TRANSISTOR_OFF);
  digitalWrite(PIN_LED_1_UNDER, LED_TRANSISTOR_OFF);
  digitalWrite(PIN_LED_2_UNDER, LED_TRANSISTOR_OFF);
  digitalWrite(PIN_LED_3_UNDER, LED_TRANSISTOR_OFF);
  digitalWrite(PIN_LED_1_FRONT, LED_TRANSISTOR_OFF);
  digitalWrite(PIN_LED_2_FRONT, LED_TRANSISTOR_OFF);
  digitalWrite(PIN_LED_3_FRONT, LED_TRANSISTOR_OFF);

  // Decide which of the six LEDs to turn on.
  if ((location & LOCATION_1) && (underOrFront & LED_UNDER)) {
    digitalWrite(PIN_LED_1_UNDER, LED_TRANSISTOR_ON);
  }
  if ((location & LOCATION_1) && (underOrFront & LED_FRONT)) {
    digitalWrite(PIN_LED_1_FRONT, LED_TRANSISTOR_ON);
  }
  if ((location & LOCATION_2) && (underOrFront & LED_UNDER)) {
    digitalWrite(PIN_LED_2_UNDER, LED_TRANSISTOR_ON);
  }
  if ((location & LOCATION_2) && (underOrFront & LED_FRONT)) {
    digitalWrite(PIN_LED_2_FRONT, LED_TRANSISTOR_ON);
  }
  if ((location & LOCATION_3) && (underOrFront & LED_UNDER)) {
    digitalWrite(PIN_LED_3_UNDER, LED_TRANSISTOR_ON);
  }
  if ((location & LOCATION_3) && (underOrFront & LED_FRONT)) {
    digitalWrite(PIN_LED_3_FRONT, LED_TRANSISTOR_ON);
  }

  // Set the color.
  if (ledColor & LED_BLUE) {
    digitalWrite(PIN_BLUE, COLOR_TRANSISTOR_ON);
  }
  if (ledColor & LED_GREEN) {
    digitalWrite(PIN_GREEN, COLOR_TRANSISTOR_ON);
  }
  if (ledColor & LED_RED) {
    digitalWrite(PIN_RED, COLOR_TRANSISTOR_ON);
  }
}

int GolfBallStand::getAnalogReading(int location) {
  int photoReading = -1;
  switch (location) {
    case LOCATION_1:
      photoReading = analogRead(PIN_PHOTO_1);
      break;
    case LOCATION_2:
      photoReading = analogRead(PIN_PHOTO_2);
      break;
    case LOCATION_3:
      photoReading = analogRead(PIN_PHOTO_3);
      break;
    case LOCATION_EXTERNAL:
      photoReading = analogRead(PIN_PHOTO_EXTERNAL);
      break;
  }
  return photoReading;
}

//int GolfBallStand::determineBallColor(int location , LiquidCrystal* lcdpointer, int* hsi) {
//  LiquidCrystal lcd = *lcdpointer;
//  setLedState(LED_OFF, location, LED_UNDER_AND_FRONT);
//  delay(GBS_TIME_DELAY);
//  int offReading = round((1 - getAnalogReading(location) / 1024.0) * 255);
//
//  setLedState(LED_RED, location, LED_UNDER_AND_FRONT);
//  delay(GBS_TIME_DELAY);
//  int redReading = round((1 - getAnalogReading(location) / 1024.0) * 255);
//
//  setLedState(LED_GREEN, location, LED_UNDER_AND_FRONT);
//  delay(GBS_TIME_DELAY);
//  int greenReading = round((1 - getAnalogReading(location) / 1024.0) * 255);
//
//  setLedState(LED_BLUE, location, LED_UNDER_AND_FRONT);
//  delay(GBS_TIME_DELAY);
//  int blueReading = round((1 - getAnalogReading(location) / 1024.0) * 255);
//
//  setLedState(LED_OFF, location, LED_UNDER_AND_FRONT);
//
//  delay(GBS_TIME_DELAY);
//
//
//  int rgb[] = {redReading, greenReading, blueReading};
//
//  if (location == LOCATION_1 && judgeisball(rgb, offReading)) {
//    rgb[1] -= rgb[1] * 0.15;
//
//  }
//
//  if (location == LOCATION_3 && judgeisball(rgb, offReading)) {
//    rgb[1] =  rgb[1] < 30 ? round(rgb[1] * 3) : round(rgb[1] * 2.1);
//    if (rgb[1] < 40) {
//      rgb[0] += round(rgb[0] * 1.2);
//      rgb[1] -= round(rgb[1] * 0.8);
//    }
//    double f = rgb[1] > 100 ? 0.3 : 0.8;
//    rgb[2] -= round(rgb[2] * f);
//
//  }
//  rgbtohsi(rgb, hsi);
//
//  lcd.clear();
//  lcd.print(rgb[0]);
//  lcd.print(" ");
//  lcd.print(rgb[1]);
//  lcd.print(" ");
//  lcd.print(rgb[2]);
//  lcd.print(" ");
//  lcd.print(offReading);
//  lcd.print(" ");
//  lcd.setCursor(0, 1);
//  lcd.print(hsi[0]);
//  lcd.print(" ");
//  lcd.print(hsi[1]);
//  lcd.print(" ");
//  lcd.print(hsi[2]);
//  lcd.print(" ");
//  if (judgeisball(rgb, offReading)) {
//    return judgeBallColor(hsi);
//  } else {
//    return BALL_NONE;
//  }
//}
//
//void GolfBallStand::determineAllBallColor(int* result, LiquidCrystal* lcdpointer) {
//
//  LiquidCrystal lcd = *lcdpointer;
//  getenvrionmentI();
//
//
//  int red_ball[] = { -1, -1, -1};
//  int hsi[3];
//  int red_ball_count = 0;
//
//  int ball = determineBallColor(LOCATION_1 , lcdpointer, hsi);
//  if (ball == BALL_RED) {
//    red_ball_count++;
//    red_ball[0] = hsi[2];
//  }
//  result[0] = ball;
//
//  ball = determineBallColor(LOCATION_2 , lcdpointer, hsi);
//  if (ball == BALL_RED) {
//    red_ball_count++;
//    red_ball[1] = hsi[2];
//  }
//  result[1] = ball;
//
//  ball = determineBallColor(LOCATION_3 , lcdpointer, hsi);
//  if (ball == BALL_RED) {
//    red_ball_count++;
//    red_ball[2] = hsi[2];
//  }
//  result[2] = ball;
//  delay(2000);
//  if (red_ball_count == 1) {
//    for (int i = 0; i < 3; i++) {
//      if (red_ball[i] > 0) {
//        result[i] = red_ball[i] > WHITE_S ? BALL_WHITE : BALL_BLACK;
//      }
//    }
//  }
//  if (red_ball_count > 1) {
//    int lowi = -1;
//    int highi = -1;
//    for (int i = 0; i < 3; i++) {
//      if (red_ball[i] > 0) {
//        if (lowi == -1) {
//          lowi = i;
//        } else {
//          highi = i;
//        }
//      }
//    }
//    if (red_ball[lowi] > red_ball[highi]) {
//      int temp = lowi;
//      lowi = highi;
//      highi = temp;
//    }
//    if (red_ball[highi] > WHITE_S) {
//      result[highi] = BALL_WHITE;
//      result[lowi] = BALL_RED;
//    } else {
//      result[highi] = BALL_RED;
//      result[lowi] = BALL_BLACK;
//    }
//  }
//  lcd.clear();
//  lcd.print(result[0]);
//  lcd.print(" ");
//  lcd.print(result[1]);
//  lcd.print(" ");
//  lcd.print(result[2]);
//  lcd.print(" ");
//
//}
//
//void GolfBallStand::rgbtohsi(int* rgb, int* hsi) {
//  int r = rgb[0];
//  int g = rgb[1];
//  int b = rgb[2];
//  int num = (r - g) + (r - b);
//  int den = 2 * sqrt((r - g) * (r - g) + (r - b) * (g - b));
//  double theta = acos(num / (double) den);
//  if (g >= b) {
//    theta = (theta);
//  } else {
//    theta = (2 * 3.1415 - theta);
//  }
//  hsi[0] = round(theta * 180 / 3.1415926);
//  hsi[1] = 1 - 3 * min(min(r, g), b) / (r + g + b);
//  hsi[2] = (r + g + b) / 3;
//  return hsi;
//}
//
//void GolfBallStand::getRefelectionReading(int color, int* output) {
//  setLedState(color, LOCATION_1 + LOCATION_2 + LOCATION_3, LED_UNDER_AND_FRONT);
//  delay(GBS_TIME_DELAY);
//  output[0] = round((1 - getAnalogReading(LOCATION_1) / 1024.0) * 255);
//  output[1] = round((1 - getAnalogReading(LOCATION_2) / 1024.0) * 255);
//  output[2] = round((1 - getAnalogReading(LOCATION_3) / 1024.0) * 255);
//}
//
//
//int GolfBallStand::judgeBallColor(int* hsi) {
//  int h = hsi[0];
//  int i = hsi[2];
//  if (abs(h - YELLOW_S) < TOLERANCE) {
//    return BALL_YELLOW;
//  } else if (abs(h - GREEN_S) < (TOLERANCE * 2)) {
//    return BALL_GREEN;
//  } else if (abs(h - BLUE_S) < (TOLERANCE * 3)) {
//    return BALL_BLUE;
//  } else {
//    return BALL_RED;
//  }
//
//
//}
void GolfBallStand::getLocationRefelectionReading(int location, int* reading) {

  setLedState(LED_OFF, location, LED_UNDER_AND_FRONT);
  delay(GBS_TIME_DELAY);
  reading[4] = getAnalogReading(location);

  setLedState(LED_RED, location, LED_UNDER_AND_FRONT);
  delay(GBS_TIME_DELAY);
  reading[0] = getAnalogReading(location);

  setLedState(LED_GREEN, location, LED_UNDER_AND_FRONT);
  delay(GBS_TIME_DELAY);
  reading[1] = getAnalogReading(location);

  setLedState(LED_BLUE, location, LED_UNDER_AND_FRONT);
  delay(GBS_TIME_DELAY);
  reading[2] = getAnalogReading(location);

  setLedState(LED_WHITE, location, LED_UNDER_AND_FRONT);
  delay(GBS_TIME_DELAY);
  reading[3] = getAnalogReading(location);

  setLedState(LED_OFF, location, LED_UNDER_AND_FRONT);

}
bool GolfBallStand::judgeisball(int* reading) {
  int count = 0;
  int diff = 0;
  for (int i = 0; i < 4; i++) {
    for (int j = i + 1; j < 5; j++) {
      count++;
      diff += abs(reading[i] - reading[j]);
    }
  }
  double aver = diff / (double) count;
  return aver > 5;


//
//  int r = rgb[0];
//  int g = rgb[1];
//  int b = rgb[2];
//  int t = abs(r - g);
//  t += abs(r - b);
//  t += abs(r - off);
//  t += abs(g - b);
//  t += abs(g - off);
//  t += abs(b - off);
//  int a = t / 6;
//  return a > 5;
}

int GolfBallStand::getenvrionmentI() {
  int reading = getAnalogReading(PIN_PHOTO_EXTERNAL);
  environment = round(reading / 1024.0 * 255);
}

