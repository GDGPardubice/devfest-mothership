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
            new Message(date(2013, 04, 15, 0, 0), "Ti troubové zase pořádají <a href='http://pardubice.devfest.cz'>DevFest</a>, naštěstí jsem tu já...", false, false),
            new Message(date(2013, 04, 26, 13, 30), "Už jen pár hodin do <a href=\"http://pardubice.devfest.cz/\">DevFestu</a>! Pro jistotu jsem zamkla orgy na místě, aby nepřišli pozdě.", false, false),
            new Message(date(2013, 04, 27, 8, 15), "Pojď se rychle checknout, mám vše připravené.", true, true),
            new Message(date(2013, 04, 27, 8, 45), "Cítím se nějak <a href=\"http://cs.wikipedia.org/wiki/Schizofrenie\">schizofreně</a>. :-/", false, false),
            new Message(date(2013, 04, 27, 10, 40), "Pavel Lahoda se chystá na přednášku o vývoji pro Android.", false, false, false, 1),
            new Message(date(2013, 04, 27, 13, 0), "Začíná přednaška o bezpečnosti od Michala Špačka a paralelně hardcore přednáška od Danuta Enachioiu.", true, true, false, 1),
            new Message(date(2013, 04, 27, 14, 10), "Znáš reaktivní programování? Aleš už se chystá ti to vysvělit a Milan Matys s přednáškou o situačním geoprostorovém systému se připravuje v chillout stage.", true, true, false, 1),
            new Message(date(2013, 04, 27, 15, 25), "Chceš vědět jak v Google probíhá Code Review? HardDev Stage těď!", true, true, false, 1),
            new Message(date(2013, 04, 27, 16, 40), "Google Closure versus ostatní knihovny a frameworky, o tom ti poví Daniel Steigerwald.", true, true, false, 1),
            new Message(date(2013, 04, 27, 17, 55), "Už se těšíš na Afterparty? Ještě neutíkej, bude vyhlášení soutěže.", true, true, false, 1),
            new Message(date(2013, 04, 28, 10, 0), "Jak se ti líbilo v Pardubicich <a href=\"http://gdgpardubice.cz/go/devfest-feedback\">", true, false, false, 1),
            new Message(date(2013, 04, 29, 22, 0), "DevFest byl moc fajn, už se těším na další. Co ty?", false, false, false, 1),
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
