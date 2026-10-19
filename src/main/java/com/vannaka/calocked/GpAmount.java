package com.vannaka.calocked;
import java.math.BigDecimal;
import java.util.Locale;
final class GpAmount {
 static long parse(String input){
  String value=input.trim().toLowerCase(Locale.ROOT).replace(",","").replace(" ","");
  if(!value.matches("[0-9]+(?:\\.[0-9]+)?[kmb]?"))throw new IllegalArgumentException("Enter a GP amount, such as 500K or 1.5M");
  long multiplier=1;
  char suffix=value.charAt(value.length()-1);
  if(suffix=='k'||suffix=='m'||suffix=='b'){
   multiplier=suffix=='k'?1000:suffix=='m'?1_000_000:1_000_000_000;
   value=value.substring(0,value.length()-1);
  }
  try{return new BigDecimal(value).multiply(BigDecimal.valueOf(multiplier)).longValueExact();}
  catch(ArithmeticException e){throw new IllegalArgumentException("Enter a whole GP amount within the supported range");}
 }
}
