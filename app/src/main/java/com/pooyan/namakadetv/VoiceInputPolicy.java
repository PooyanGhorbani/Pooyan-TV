package com.pooyan.namakadetv;
import java.util.List;
public final class VoiceInputPolicy {
 public static String result(List<String> values){if(values==null)return "";for(String s:values)if(s!=null&&!s.trim().isEmpty())return s.trim().substring(0,Math.min(100,s.trim().length()));return "";}
 public static String error(boolean available,boolean denied){return !available?"سرویس تشخیص گفتار نصب نیست؛ از جست‌وجوی متنی استفاده کنید.":denied?"مجوز میکروفون توسط سرویس پذیرفته نشد؛ جست‌وجوی متنی در دسترس است.":"گفتار دریافت نشد؛ اتصال، میکروفون یا زبان سرویس را بررسی کنید. جست‌وجوی متنی در دسترس است.";}
}
