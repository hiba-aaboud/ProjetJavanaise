

package irc;

import java.awt.*;
import java.awt.event.*;

import jvn.JvnException;
import jvn.JvnServerImpl;
import jvn.JvnObject;

import java.io.*;


public class Irc {
    public TextArea	text;
    public TextField data;
    Frame frame;
    JvnObject sentence;


    /**
     * main method
     * create a JVN object nammed IRC for representing the Chat application
     **/
    public static void main(String argv[]) {
        try {
            // initialize JVN
            JvnServerImpl js = JvnServerImpl.jvnGetServer();
            // look up the IRC object in the JVN server
            // if not found, create it, and register it in the JVN server
            JvnObject jo = js.jvnLookupObject("IRC");
            if (jo == null) {
                jo = js.jvnCreateObject(new Sentence());
                // after creation, I have a write lock on the object
                jo.jvnUnLock();
                js.jvnRegisterObject("IRC", jo);
            }
            // create the graphical part of the Chat application
            new Irc(jo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * IRC Constructor
     @param jo the JVN object representing the Chat
     **/
    public Irc(JvnObject jo) {
        sentence = jo;
        frame=new Frame();
        frame.setLayout(new GridLayout(1,1));
        text=new TextArea(10,60);
        text.setEditable(false);
        text.setForeground(Color.red);
        frame.add(text);
        data=new TextField(40);
        frame.add(data);
        Button read_button = new Button("read");
        read_button.setForeground(Color.darkGray);
        Button unlock_button = new Button("unlock");
        unlock_button.setForeground(Color.darkGray);
        read_button.addActionListener(new readListener(this));
        unlock_button.addActionListener(new unlockListener(this));
        frame.add(read_button);
        Button write_button = new Button("write");
        write_button.setForeground(Color.darkGray);
        write_button.addActionListener(new writeListener(this));
        frame.add(write_button);
        frame.add(unlock_button);
        frame.setSize(545,201);
        text.setBackground(Color.black);
        frame.setVisible(true);
    }
}


/**
 * Internal class to manage user events (read) on the CHAT application
 **/
class readListener implements ActionListener {
    Irc irc;

    public readListener (Irc i) {
        irc = i;
    }

    /**
     * Management of user events
     **/
    public void actionPerformed (ActionEvent e) {
        try {
            // lock the object in read mode
            //System.out.println("\nBefore lockRead operation :"+ irc.sentence.jvnGetSharedObject().toString());
            irc.sentence.jvnLockRead();
            // invoke the method
            String s = ((Sentence)(irc.sentence.jvnGetSharedObject())).read();

            // unlock the object
            //irc.sentence.jvnUnLock();
            //System.out.println("After lockRead operation  :"+ irc.sentence.jvnGetSharedObject().toString());

            // display the read value
            irc.data.setText(s);
            irc.text.append(s+"\n");
        } catch (JvnException je) {
            System.out.println("IRC problem : " + je.getMessage());
        }
    }
}

/**
 * Internal class to manage user events (write) on the CHAT application
 **/
class writeListener implements ActionListener {
    Irc irc;

    public writeListener (Irc i) {
        irc = i;
    }

    /**
     * Management of user events
     **/
    public void actionPerformed (ActionEvent e) {
        try {
            // get the value to be written from the buffer
            String s = irc.data.getText();
            System.out.println("\nBefore lockWrite operation :"+ irc.sentence.jvnGetSharedObject().toString());

            // lock the object in write mode
            irc.sentence.jvnLockWrite();
            System.out.println("After lockWrite operation  :"+ irc.sentence.jvnGetSharedObject().toString());

            // invoke the method
            ((Sentence)(irc.sentence.jvnGetSharedObject())).write(s);

            // unlock the object
            //irc.sentence.jvnUnLock();
        } catch (JvnException je) {
            System.out.println("IRC problem  : " + je.getMessage());
        }
    }
}


/**
 * Internal class to manage user events (unlock) on the CHAT application
 **/
class unlockListener implements ActionListener {
    Irc irc;

    public unlockListener (Irc i) {
        irc = i;
    }

    /**
     * Management of user events
     **/
    public void actionPerformed (ActionEvent e) {
        try {
            irc.sentence.jvnUnLock();
        } catch (JvnException je) {
            System.out.println("IRC problem  : " + je.getMessage());
        }
    }
}