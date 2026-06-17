package eu.izadpanah.mcheck;

public class ITabHandler implements Runnable {
    //TODO :: Start to listen to the incoming Messages from iTabSoketServer
    //TODO :: Check if the a Handling is needed in Order to check the Authorization
        //TODO :: Is there a Handling during the  Process needed or after that in Payment
        //TODO :: The Response/Handling should be sent to iTab back
        //TODO :: The Response from iTab ????

    protected static String startItab="<ITAB_SCO>";
    protected static String endItab="</ITAB_SCO>";
    protected static String message="<message>";
    protected static String messageEnd="</message>";
    protected static String parameters="<parameters>";
    protected static String parametersEnd="</parameters>";
    protected static String operationId="<operator_id/>";
    protected static String messageType="<type>";
    protected static String messageTypeEnd="</type>";
    protected static String verificationType="<verification_type>";
    protected static String verificationTypeEnd="</verification_type>";
    protected static String ageLevel="<age_level>";
    protected static String agelevelEnd="</age_level>";
    protected static String interfaceVersionall="<interface_version>3.5</interface_version>";
    protected static String response="<response>";
    protected static String responseEnd="</response>";
    protected static String messageId="<id>";
    protected static String messageIdEnd="</id>";
    protected static String interfaceVersion="<interface_version>";
    protected static String interfaceVersionEnd="</interface_version>";
    protected static String xmlVersion="<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    protected static String acceptAgeRemote="";

    public ITabHandler() {
    }

    @Override
    public void run() {

    }

    protected void remoteAgeAccept(){
        acceptAgeRemote=xmlVersion+startItab+message+parameters+operationId+ageLevel+"20"+agelevelEnd+verificationType+"OTHER"+
                verificationTypeEnd+parametersEnd+messageId+Main.mID+messageIdEnd+messageType+"Remote age accepted"+
                messageTypeEnd+messageEnd+interfaceVersionall+endItab;
        //Main.itabsocketserver.activeClients.forEach(k,ip -> System.out.println(ip));
        //System.out.println("remote Age Message: "+acceptAgeRemote);
        if (!Main.sentRmoteAgeAccept.get())
            Main.itabsocketserver.sendMessageToClient("127.0.0.1",acceptAgeRemote);
    }
}
