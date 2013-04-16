package net.filiph.mothership;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple class holding the information about each message.
 */
public class Message {
    public final Date time;
    public String text;
    public boolean notify = true;
    public boolean vibrate = false;
    public boolean append = false;
    public int ai_id = 0;
    public boolean forceShowActivity = false;
    public final int uid;

    /**
     * Tag used on log messages.
     */
    static final String TAG = "Message";

    /**
     * Constructs a new message that should fire in time [:t:] with message [:s:].
     *
     * @param t The time when this message should appear.
     * @param s The actual text of the message. This can contain HTML tags.
     */
    public Message(Date t, String s) {
        time = t;
        parseMessageString(s);
        uid = getHashFromString(this.text + getTimeString());
    }

    /**
     * Constructs a new message that should fire in time [:t:] with message [:s:].
     *
     * @param t        The time when this message should appear.
     * @param s        The actual text of the message. This can contain HTML tags.
     * @param _notify  Whether to make a notification when the message's time comes.
     * @param _vibrate Whether to vibrate when the message's time comes.
     * @param _append  Whether to append message to dialog.
     * @param _ai_id   Id of AI, which says the message.
     */
    public Message(Date t, String s, boolean _notify, boolean _vibrate, boolean _append, int _ai_id) {
        time = t;
        text = s;
        notify = _notify;
        vibrate = _vibrate;
        append = _append;
        ai_id = _ai_id;
        uid = getHashFromString(s + getTimeString());
    }

    public Message(Date t, String s, boolean _notify, boolean _vibrate) {
        this(t, s, _notify, _vibrate, false, 0);
    }

    /**
     * Compute very simple hash from a given string.
     *
     * @param s String to be hashed.
     * @return An (hopefully) unique integer.
     */
    public static int getHashFromString(String s) {
        int hash = 7;
        for (int i = 0; i < s.length(); i++) {
            hash = hash * 31 + s.charAt(i);
        }
        return hash;
    }

    /**
     * Constructs a string describing the time
     *
     * @return A string in "yyyy/MM/dd//HH:mm" format.
     */
    public String getTimeString() {
        return (String) new SimpleDateFormat("yyyy/MM/dd//HH:mm").format(time);
    }

    private void parseMessageString(String messageString) {
        int startBracketIndex = messageString.indexOf("[");
        int endBracketIndex = messageString.indexOf("]");
        if (startBracketIndex == 0 && endBracketIndex > 0) {
            for (int i = 1; i < endBracketIndex; i++) {
                char ch = messageString.charAt(i);
                switch (ch) {
                    case 'v':
                        vibrate = true;
                        break;
                    case 'n':
                        notify = true;
                        break;
                    case 'a':
                        append = true;
                        break;
                    case '1':
                        ai_id = 1;
                        break;
                }
            }
        } else {
            Log.v("TEST:" + TAG, "Received message without '[]' options in the front.");
        }
        if (endBracketIndex < messageString.length() - 1) {
            text = messageString.substring(endBracketIndex + 1);
        }
    }
}



