package test;

import org.apache.xmlbeans.XmlException;

import net.opengis.wns.x00.NotificationChannelType;
import net.opengis.wns.x00.NotificationTargetDocument;
import net.opengis.wns.x00.NotificationChannelType.WNS;
import net.opengis.wns.x00.NotificationFormatDocument.NotificationFormat;
import net.opengis.wns.x00.NotificationTargetDocument.NotificationTarget;


public class NotificationTargetTest {

   /**
    * @param args
    */
   public static void main(String[] args) {

      NotificationTargetDocument ntd = NotificationTargetDocument.Factory.newInstance();
      
      NotificationTarget nt = ntd.addNewNotificationTarget();
      
      nt.setNotificationFormat(NotificationFormat.BASIC);
      
      NotificationChannelType nct = nt.addNewNotificationChannel();

      nct.addEmail("foo@bar.de");
      nct.addXMPP("xmpp@jabber.org");
      WNS wns = nct.addNewWNS();
      wns.setWNSID("345");
      wns.setWNSURL("http://wns.52north.org/WNS/wns");
      
      System.out.println(ntd);
      
      String target = 
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
      "<NotificationTarget xmlns=\"http://www.opengis.net/wns\">"+
  "<NotificationChannel>"+
    "<XMPP>xmpp@jabber.org</XMPP>"+
    "<Email>foo@bar.de</Email>"+
    "<WNS>"+
      "<WNSID>345</WNSID>"+
      "<WNSURL>http://wns.52north.org/WNS/wns</WNSURL>"+
    "</WNS>"+
  "</NotificationChannel>"+
  "<NotificationFormat>basic</NotificationFormat>"+
"</NotificationTarget>";
      
      try {
         
         NotificationTargetDocument ntd2 = NotificationTargetDocument.Factory.parse(target);
         
         NotificationChannelType nct2 = ntd2.getNotificationTarget().getNotificationChannel();
         
         System.out.println(nct2.getEmailArray()[0]);
         System.out.println(nct2);
         
      } catch (XmlException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      
      
   }

}
