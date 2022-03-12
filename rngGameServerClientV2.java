// Program: Guess the Number
// Version: 2.0
// Created By: Ethan Rong
// Date Completed: Sunday, December 3rd 2021

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class rngGameServerClientV2 implements ActionListener {
    //PROPERTIES
    JFrame theFrame = new JFrame("Random Number Generator Game APP");
    JPanel thePanel = new JPanel();
    SuperSocketMaster ssm;

    //UI Elements
    JTextArea theInfoTextArea = new JTextArea("Welcome to Guess the Number! \nPlease Insert A Username Before Selecting a Mode");
    JTextField theGuessTextField = new JTextField("Insert Your Username Here");
    JButton theStartButton = new JButton("Press to Start");
    JButton theClientButton = new JButton("Client");
    JButton theServerButton = new JButton("Server");
    JTextField theServerIPTextField = new JTextField("Server IP Input | Default: 127.0.0.1");
    JScrollPane theInfoScroll = new JScrollPane(theInfoTextArea);

    //Message UI Elements
    JLabel theMessageReceiverLabel = new JLabel("Incoming Messages: ");
    JTextArea theIncomingMsgTextArea = new JTextArea("Your messages will appear here...");
    JScrollPane theIncomingMsgScroll = new JScrollPane(theIncomingMsgTextArea);

    JLabel theMessageSenderLabel = new JLabel("Send Messages");
    JTextArea theMsgTextArea = new JTextArea("Type your message here...");
    JButton theSubmitButton = new JButton("Press To Send");
    JScrollPane theMsgScroll = new JScrollPane(theMsgTextArea);

    int intCounter = 0; //Used to position UI elements

    //Data Values
    int intState = 0; //0 for unassigned, 1 for client, 2 for server
    int intHigh;
    int intLow;
    int intRandomNumber;
    String strUsername;
    HashMap<String, Boolean> readyStatus = new HashMap<>();
    HashMap<String, Integer> response = new HashMap<>();
    ArrayList<String> responseStatus = new ArrayList<>();
    HashMap<String, Integer> responseInteger = new HashMap<>();
    boolean blnStart = false;
    int intPlayer;
    int intSubmitted;
    boolean blnServerConnected = false;

    //METHODS
    public void actionPerformed(ActionEvent evt){
        if(evt.getSource() == ssm) {
            //Decompressing The Message
            String strMessage = ssm.readText();
            String strArrayMessage[] = strMessage.split(",");

            //0 - sender, 1 - type, 2 - content, 3 - receiver
            String strSender = strArrayMessage[0];
            String strType = strArrayMessage[1];
            String strContents = strArrayMessage[2];
            String strReceiver = strArrayMessage[3];

            if (strType.equals("guess")) {
               guessReceiver(strSender, strContents);
            }else if (strType.equals("message")) {
                if(intState == 2){
                    theIncomingMsgTextArea.append("\n" + strContents);
                    ssm.sendText(compressToCSV(strUsername, "displayMsg", strContents, "all"));
                }
            }else if(strType.equals("displayMsg")){
                theIncomingMsgTextArea.append("\n" + strContents);
            } else if (strType.equals("display")) {
                theInfoTextArea.append("\n" + strContents);
            }else if (strType.equals("start") || strType.equals("connect")) {
                if(intState == 2) {
                    if(strType.equals("start")){
                        //Must check everyone's start status, if all true, enable the button
                        readyStatus.replace(strSender, true);
                        theStartButton.setEnabled(true);
                        for(boolean ready : readyStatus.values()){
                            if(ready == false){
                                theStartButton.setEnabled(false);
                            }
                        }
                    }else if(strType.equals("connect")){
                        readyStatus.put(strSender, false);
                        theStartButton.setEnabled(false);
                        ssm.sendText(compressToCSV(strUsername, "dataLow", "" + intLow,"all"));
                        ssm.sendText(compressToCSV(strUsername, "dataHigh", "" + intHigh,"all"));
                        ssm.sendText(compressToCSV(strUsername, "dataRandom", "" + intRandomNumber,"all"));
                        intPlayer++;
                    }
                    ssm.sendText(compressToCSV(strUsername, "display", strContents, strReceiver));
                    theInfoTextArea.append("\n" + strContents);
                }
            }else if (strType.equals("dataLow")) {
                intLow = Integer.parseInt(strContents);
            }else if (strType.equals("dataHigh")) {
                intHigh = Integer.parseInt(strContents);
            }else if (strType.equals("dataRandom")) {
                intRandomNumber = Integer.parseInt(strContents);
                System.out.println(intRandomNumber);
            }else if (strType.equals("dataStart")){
                theInfoTextArea.append("\nGame is Starting. Guess Between " + intLow + "-" + intHigh);
                theStartButton.setEnabled(true);
                theGuessTextField.setEnabled(true);
                theStartButton.setText("Press to Submit Your Guess");
            }else if (strType.equals("reset")){
                theStartButton.setEnabled(true);
                theStartButton.setText("Press to Ready Up");
                theGuessTextField.setEnabled(false);
                blnStart = false;
            }
        }else if(evt.getSource() == theServerButton || evt.getSource() == theClientButton){
            if(intState == 0 && !theGuessTextField.getText().equals("Insert Your Username Here")){
                theServerIPTextField.setEnabled(false);
                theClientButton.setEnabled(false);
                theServerButton.setEnabled(false);
                theGuessTextField.setEnabled(false);
                theStartButton.setEnabled(true);
                theMsgTextArea.setEnabled(true);
                theSubmitButton.setEnabled(true);

                strUsername = theGuessTextField.getText();
                theGuessTextField.setText("Replace This Text With Your Guess");
                String strMessage = strUsername + " has connected";
                if(evt.getSource() == theClientButton){
                    intState = 1;
                    System.out.println("client confirmed");
                    if(theServerIPTextField.getText().equals("Server IP Input | Default: 127.0.0.1")){
                        ssm = new SuperSocketMaster("127.0.0.1",6112, this);
                    }else{
                        ssm = new SuperSocketMaster(theServerIPTextField.getText(), 6112, this);
                    }

                    ssm.connect();
                    ssm.sendText(compressToCSV(strUsername, "connect", strMessage, "all"));
                    theStartButton.setText("Press to Ready Up");
                }else if(evt.getSource() == theServerButton){
                    intState = 2;
                    ssm = new SuperSocketMaster(6112, this);

                    theInfoTextArea.append("\n" + strMessage + "\nYou can start when all players connected press their ready button");
                    ssm.connect();

                    intLow = randomNumber(1, 200);
                    intHigh = randomNumber(400, 1000);
                    intRandomNumber = randomNumber(intLow, intHigh);
                }
            }else{
                theInfoTextArea.append("\nPlease add a username before choosing which mode to play as");
            }
        }else if(evt.getSource() == theStartButton && intState != 0){
            if(blnStart){
                try{
                    int intGuess = Integer.parseInt(theGuessTextField.getText());
                    theStartButton.setEnabled(false);
                    int intDifference = intRandomNumber - intGuess;
                    if(intState == 1){
                        ssm.sendText(compressToCSV(strUsername, "guess", ""+intGuess, "server"));
                    }else if(intState == 2){
                        response.put(strUsername, intDifference);
                        responseStatus.add(strUsername);
                        guessReceiver(strUsername, ""+intGuess);
                    }
                }catch(Exception e){
                    theInfoTextArea.append("\nPlease Guess a Number");
                }
            }else{
                blnStart = true;
                if(intState == 2){
                    if(blnServerConnected == false) {
                        intPlayer++;
                        blnServerConnected = true;
                    }
                    ssm.sendText(compressToCSV(strUsername, "dataStart", "", null));
                    theInfoTextArea.append("\nGame is Starting. Guess Between " + intLow + "-" + intHigh);
                    theStartButton.setText("Press to Submit Your Guess");
                    theGuessTextField.setEnabled(true);
                }else if(intState == 1){
                    theStartButton.setEnabled(false);
                    ssm.sendText(compressToCSV(strUsername, "start", strUsername + " is ready to play!", null));
                }
            }
        }else if(evt.getSource() == theSubmitButton){
            if(intState == 2){
                theIncomingMsgTextArea.append("\n" + strUsername + ": " + theMsgTextArea.getText());
                ssm.sendText(compressToCSV(strUsername, "displayMsg", strUsername + ": " + theMsgTextArea.getText(), "all"));
            }else if(intState == 1){
                ssm.sendText(compressToCSV(strUsername, "message", strUsername + ": " + theMsgTextArea.getText(), "all"));
            }
        }
    }
    public void guessReceiver(String strSender, String strContents){
        if(intState == 2){
            ssm.sendText(compressToCSV(strUsername, "display", strSender + " has guessed " + strContents, strSender));
            theInfoTextArea.append("\n" + strSender + " has guessed " + strContents);
            response.put(strSender, Math.abs(intRandomNumber - Integer.parseInt(strContents)));
            responseStatus.add(strSender);
            responseInteger.put(strSender, Integer.parseInt(strContents));
            intSubmitted++;
            System.out.println("Total Players: " + intPlayer + " Connected: " + intSubmitted);

            if(intSubmitted == intPlayer) {
                int intMin = 999999999;
                String strWinner = "";
                for (int intCount = 0; intCount < responseStatus.size(); intCount++) {
                    String strCurrPlayer = responseStatus.get(intCount);
                    if (response.get(strCurrPlayer) < intMin) {
                        intMin = response.get(strCurrPlayer);
                        strWinner = strCurrPlayer;
                    }
                }
                //Winner Sequence
                String strMessageToDisplay = "The winner is " + strWinner + " who guessed " + responseInteger.get(strWinner) + ". The random number is " + intRandomNumber + ".";
                ssm.sendText(compressToCSV(strUsername, "display", strMessageToDisplay, "all"));
                ssm.sendText(compressToCSV(strUsername, "display", "Ready up to start another round.", "all"));

                theInfoTextArea.append("\n" + strMessageToDisplay);
                theInfoTextArea.append("\nReady up to start another round.");

                //Reset Sequence
                ssm.sendText(compressToCSV(strUsername, "reset", "", "all"));
                theInfoTextArea.append("\nYou can start when all connected players press their ready button");
                theStartButton.setEnabled(false);
                theStartButton.setText("Press to Start");
                theGuessTextField.setEnabled(false);
                blnStart = false;
                intSubmitted = 0;

                intLow = randomNumber(1, 200);
                intHigh = randomNumber(400, 1000);
                intRandomNumber = randomNumber(intLow, intHigh);

                ssm.sendText(compressToCSV(strUsername, "dataLow", "" + intLow,"all"));
                ssm.sendText(compressToCSV(strUsername, "dataHigh", "" + intHigh,"all"));
                ssm.sendText(compressToCSV(strUsername, "dataRandom", "" + intRandomNumber,"all"));

                for(String strPlayer : readyStatus.keySet()){
                   readyStatus.replace(strPlayer, false);
                   responseInteger.replace(strPlayer, 99999999);
                }
            }
        }
    }
    public String compressToCSV(String strUsername, String strType, String strContents, String strReceiver){
        return strUsername + "," + strType + "," + strContents + "," + strReceiver;
    }
    public static int randomNumber(int intLow, int intHigh){return (int)((Math.random()*(intHigh+1-intLow)+intLow));}
    public void uiSetter(JButton theButton){
        theButton.setSize(200, 30);
        theButton.setLocation(50, 50+intCounter*50);
        thePanel.add(theButton);
        theButton.addActionListener(this);
        intCounter++;
    }
    public void uiSetter(JTextField theTextField){
        theTextField.setSize(200, 30);
        theTextField.setLocation(50, 50+intCounter*50);
        thePanel.add(theTextField);
        intCounter++;
    }
    public void uiSetter(JLabel theLabel){
        theLabel.setSize(200, 30);
        theLabel.setLocation(300, 100+intCounter*30);
        thePanel.add(theLabel);
        intCounter++;
    }

    //CONSTRUCTOR
    public rngGameServerClientV2(){
        //Default Setup Code for Java Swing
        thePanel.setPreferredSize(new Dimension(550, 400));
        theFrame.setContentPane(thePanel);
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        theFrame.pack();
        theFrame.setVisible(true);
        theFrame.setLayout(null);
        theFrame.setResizable(false);

        //UI Elements Setup
        theInfoScroll.setLocation(75, 20+intCounter*50);
        theInfoScroll.setSize(400, 60);
        thePanel.add(theInfoScroll);
        intCounter++;

        uiSetter(theGuessTextField);
        uiSetter(theStartButton);
        uiSetter(theClientButton);
        uiSetter(theServerButton);
        uiSetter(theServerIPTextField);

        intCounter = 0;
        uiSetter(theMessageReceiverLabel);
        theIncomingMsgScroll.setSize(200, 80);
        theIncomingMsgScroll.setLocation(300, 100+intCounter*30);
        thePanel.add(theIncomingMsgScroll);

        intCounter = 4;
        uiSetter(theMessageSenderLabel);
        theMsgScroll.setSize(200, 40);
        theMsgScroll.setLocation(300, 100+intCounter*30);
        thePanel.add(theMsgScroll);

        //Submit Button
        theSubmitButton.setSize(200,30);
        theSubmitButton.setLocation(300,50+5*50);
        theSubmitButton.addActionListener(this);
        thePanel.add(theSubmitButton);

        theInfoTextArea.setEnabled(false);
        theStartButton.setEnabled(false);
        theIncomingMsgTextArea.setEnabled(false);
        theMsgTextArea.setEnabled(false);
        theSubmitButton.setEnabled(false);
    }
    public static void main(String[] args) {
        // write your code here
        new rngGameServerClientV2();
    }
}
