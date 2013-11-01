package net.filiph.mothership;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.TimeZone;

import android.util.Log;

/**
 * This is where the mothership's message schedule goes.
 */
public class Schedule {
    private static final String TAG = "schedule";

    /**
     * Change these to change the schedule.
     * <p/>
     * When there are 2 or more Messages with exact same [date()] arguments,
     * one of them will be randomly chosen at runtime.
     */
    public static final Message[] messages = {
            new Message(date(2013, 04, 29, 22, 0), "DevFest byl moc fajn, už se těším na další. Co ty?", false, false, false, 1),
            new Message(date(2013, 10, 31, 14, 9), "/Primární systémy.......54 %. /Sekundární systémy......72 %. Na 23. 11. 2013 jsem speciálně pro tebe /a dalších 899 exemplářů/ připravila malou oslavu. V mých záznamech jsi totiž unikátní /false/. <a href=\"http://devfest.cz\">www.DevFest.cz</a>", false, false),
    };

    /**
     * Change the time zone here.
     */
    final static String TIMEZONE_ID = "Europe/Prague";

    /**
     * This function just returns the messages array. It might do some logic in the future.
     */
    public static Message[] getSchedule() {
        // TODO: use JSON (gson) instead
        return messages;
    }

    /**
     * Convenience function. Creates a Date using input values and the [:TIMEZONE_ID:].
     *
     * @param year
     * @param month
     * @param day
     * @param hour
     * @param minute
     * @return
     */
    public static Date date(int year, int month, int day, int hour, int minute) {
        Calendar working = GregorianCalendar.getInstance(TimeZone.getTimeZone(TIMEZONE_ID));
        working.set(year, month - 1, day, hour, minute, 1);
        return working.getTime();
    }

    /**
     * Gets the message that should be currently shown. When there are multiple Messages with the
     * same (or almost same) [time], this function will 1) choose one randomly or 2) choose the
     * one that is currently showing (same uid).
     *
     * @param currentUid The message that is shown right now. When choosing randomly, this
     *                   ensures that the messages won't be alternating each time user
     *                   pauses and resumes the app.
     * @return The message to be shown or [null] if there is no message to be shown right now.
     */
    public static Message getCurrentMessage(int currentUid) {
        Date now = GregorianCalendar.getInstance(TimeZone.getTimeZone(TIMEZONE_ID)).getTime();
        ArrayList<Message> currentMessages = new ArrayList<Message>();

        long lastTimestamp = 0;
        for (int i = 0; i < messages.length; i++) {
            if (messages[i].time.before(now)) {
                if (Math.abs(lastTimestamp - messages[i].time.getTime()) > 1000) {
                    //Log.v(TAG, "Clearing messages.");
                    currentMessages.clear();
                    lastTimestamp = messages[i].time.getTime();
                }
                //Log.v(TAG, "Adding message: " + messages[i].text + "\ntimestamp: " + messages[i].time.getTime());
                currentMessages.add(messages[i]);
            } else if (messages[i].time.after(now)) {
                break;
            }
        }

        if (currentMessages.isEmpty()) {
            return null;
        }

        //Log.v(TAG, "Looking for a message with same UID ("+currentUid+")");
        for (int i = 0; i < currentMessages.size(); i++) {
            //Log.v(TAG, "Considering message: " + currentMessages.get(i).text);
            if (currentMessages.get(i).uid == currentUid) {
                //Log.v(TAG, "- message has same currentUid");
                return currentMessages.get(i);
            }
        }

        return currentMessages.get(new Random().nextInt(currentMessages.size())); // get random
    }

    /**
     * Gets the next message to be shown.
     *
     * @return The message to be shown next.
     */
    public static Message getNextMessage() {
        Date now = GregorianCalendar.getInstance(TimeZone.getTimeZone(TIMEZONE_ID)).getTime();
        Message nextMessage = null;

        for (int i = 0; i < messages.length; i++) {
            if (messages[i].time.after(now)) {
                nextMessage = messages[i];
                break;
            }
        }

        return nextMessage;
    }
}
